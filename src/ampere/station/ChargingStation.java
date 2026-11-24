package ampere.station;

import ampere.billing.BillingService;
import ampere.charger.ACCharger;
import ampere.charger.Charger;
import ampere.charger.DCFastCharger;
import ampere.request.ChargingRequest;
import ampere.session.ChargingSession;
import ampere.session.Receipt;
import ampere.thread.ChargingTask;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ChargingStation {
    private static final int TICK_MILLIS = 350;

    public record Submission(boolean started, long sessionId, String bayId) {}

    private record Handoff(ChargingSession next, Throwable failure) {}

    private final ChargingBay[][] grid;
    private final Clock clock;
    private final ChargingTask.TickDelay delay;
    private final BillingService billing = new BillingService();

    // This station's intrinsic lock protects occupancy, the queue, and all live sessions.
    private final LinkedHashMap<String, ChargingSession> active = new LinkedHashMap<>();
    private final WaitingQueue waiting = new WaitingQueue();
    private final HashSet<String> openRegistrations = new HashSet<>();
    private ArrayList<Receipt> history = new ArrayList<>();
    private long nextId = 1;
    private boolean accepting = true;

    public ChargingStation() {
        this(new ChargingBay[][] {
            {new ChargingBay("A1", new ACCharger()), new ChargingBay("A2", new ACCharger()),
             new ChargingBay("A3", new DCFastCharger())},
            {new ChargingBay("B1", new ACCharger()), new ChargingBay("B2", new ACCharger()),
             new ChargingBay("B3", new DCFastCharger())}
        }, Clock.systemDefaultZone(), registration -> Thread.sleep(TICK_MILLIS));
    }

    public ChargingStation(ChargingBay[][] layout, Clock clock, ChargingTask.TickDelay delay) {
        this.grid = copyGrid(layout);
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        this.delay = Objects.requireNonNull(delay, "Tick delay is required.");
    }

    public Submission submit(ChargingRequest request) {
        Objects.requireNonNull(request, "Charging request is required.");
        ChargingSession session;
        synchronized (this) {
            if (!accepting) {
                throw new IllegalStateException("The station is no longer accepting requests.");
            }
            String registration = request.getVehicle().getRegistration();
            if (openRegistrations.contains(registration)) {
                throw new IllegalArgumentException("Vehicle already waiting or charging: " + registration);
            }
            ChargingBay bay = findFreeBay(request.getType());

            // Every queued request needs a future ID, and nextId must remain representable.
            int queuedCount = openRegistrations.size() - active.size();
            if (Long.MAX_VALUE - nextId <= queuedCount) {
                throw new IllegalStateException("No more session IDs are available.");
            }

            if (bay == null) {
                waiting.add(request);
                openRegistrations.add(registration);
                return new Submission(false, 0, "");
            }
            session = reserve(request, bay);
            openRegistrations.add(registration);
        }
        startWorker(session, null);
        return new Submission(true, session.getId(), session.getBay().getId());
    }

    public boolean cancel(long sessionId) {
        Thread worker = null;
        synchronized (this) {
            for (ChargingSession session : active.values()) {
                if (session.getId() == sessionId && !session.isCancellationRequested()) {
                    session.requestCancellation();
                    worker = session.getWorker();
                    break;
                }
            }
        }
        if (worker == null) {
            return false;
        }
        worker.interrupt();
        return true;
    }

    public synchronized boolean canCharge(ChargingSession session) {
        return ownsBay(session) && !session.isCancellationRequested();
    }

    public synchronized boolean tick(ChargingSession session) {
        if (!canCharge(session)) {
            return false;
        }
        ChargingRequest request = session.getRequest();
        int remaining = request.getRequestedKwh() - session.getDeliveredKwh();
        if (remaining > 0) {
            int step = request.getType() == Charger.Type.DC ? 3 : 1;
            session.addEnergy(Math.min(step, remaining));
        }
        return session.getDeliveredKwh() == request.getRequestedKwh();
    }

    public void finish(ChargingSession session, boolean deliveredAll) {
        Handoff handoff = settleAndHandOff(session, deliveredAll);
        startWorker(handoff.next(), handoff.failure());
    }

    public synchronized List<Receipt> getHistory() {
        return List.copyOf(history);
    }

    public synchronized String describeState() {
        StringBuilder text = new StringBuilder("Bays:\n");
        for (ChargingBay[] row : grid) {
            for (ChargingBay bay : row) {
                text.append("  ").append(bay.getId()).append(' ')
                    .append(bay.getCharger().getType()).append(' ')
                    .append(active.containsKey(bay.getId()) ? "occupied" : "free");
            }
            text.append('\n');
        }

        text.append("Active sessions:\n");
        if (active.isEmpty()) {
            text.append("  None\n");
        }
        for (ChargingSession session : active.values()) {
            ChargingRequest request = session.getRequest();
            int delivered = session.getDeliveredKwh();
            long battery = (100L * (request.getStartingKwh() + (long) delivered))
                / request.getVehicle().getCapacityKwh();
            text.append("  #").append(session.getId()).append(" | ").append(session.getBay().getId())
                .append(" | ").append(request.getVehicle().getRegistration())
                .append(" | ").append(request.getType())
                .append(" | ").append(delivered).append('/').append(request.getRequestedKwh())
                .append(" kWh | Battery ").append(battery).append('%');
            if (session.isCancellationRequested()) {
                text.append(" | Cancelling");
            }
            text.append('\n');
        }

        text.append("Waiting requests:\n");
        if (waiting.isEmpty()) {
            text.append("  None\n");
        }
        for (ChargingRequest request : waiting.snapshot()) {
            text.append("  ").append(request.getVehicle().getRegistration())
                .append(" | ").append(request.getType())
                .append(" | ").append(request.getRequestedKwh()).append(" kWh\n");
        }
        return text.toString();
    }

    public synchronized void restoreHistory(List<Receipt> receipts) {
        if (!isIdle()) {
            throw new IllegalStateException("Wait for the station to be idle before restoring history.");
        }
        List<Receipt> incoming = List.copyOf(receipts);
        LinkedHashMap<Long, Receipt> merged = new LinkedHashMap<>();
        for (Receipt receipt : history) {
            merged.put(receipt.getSessionId(), receipt);
        }

        long firstUnused = nextId;
        for (Receipt receipt : incoming) {
            long id = receipt.getSessionId();
            if (id <= 0 || id == Long.MAX_VALUE) {
                throw new IllegalArgumentException("Cannot restore session ID " + id + ".");
            }
            Receipt previous = merged.putIfAbsent(id, receipt);
            if (previous != null && !previous.equals(receipt)) {
                throw new IllegalArgumentException("Conflicting receipts for session " + id + ".");
            }
            firstUnused = Math.max(firstUnused, id + 1);
        }
        history = new ArrayList<>(merged.values());
        nextId = firstUnused;
    }

    public synchronized void stopAccepting() {
        accepting = false;
    }

    public synchronized void awaitIdle() throws InterruptedException {
        while (!isIdle()) {
            wait();
        }
    }

    public synchronized boolean awaitIdle(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "Timeout is required.");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout cannot be negative.");
        }
        long timeoutNanos;
        try {
            timeoutNanos = timeout.toNanos();
        } catch (ArithmeticException tooLarge) {
            timeoutNanos = Long.MAX_VALUE;
        }
        long started = System.nanoTime();
        while (!isIdle()) {
            long remaining = timeoutNanos - (System.nanoTime() - started);
            if (remaining <= 0) {
                return false;
            }
            TimeUnit.NANOSECONDS.timedWait(this, remaining);
        }
        return true;
    }

    private boolean isIdle() {
        return active.isEmpty() && waiting.isEmpty();
    }

    private boolean ownsBay(ChargingSession session) {
        return session != null && active.get(session.getBay().getId()) == session;
    }

    private ChargingBay findFreeBay(Charger.Type type) {
        boolean supported = false;
        for (ChargingBay[] row : grid) {
            for (ChargingBay bay : row) {
                if (bay.getCharger().getType() == type) {
                    supported = true;
                    if (!active.containsKey(bay.getId())) {
                        return bay;
                    }
                }
            }
        }
        if (!supported) {
            throw new IllegalArgumentException("The station has no " + type + " bay.");
        }
        return null;
    }

    private ChargingSession reserve(ChargingRequest request, ChargingBay bay) {
        LocalDateTime startedAt = LocalDateTime.now(clock);
        int rate = billing.rateAt(bay.getCharger(), startedAt);
        ChargingSession session = new ChargingSession(nextId, request, bay, startedAt, rate);
        Thread worker = new Thread(new ChargingTask(this, session, delay), "ampere-charge-" + nextId);
        worker.setDaemon(false);
        session.attachWorker(worker);
        active.put(bay.getId(), session);
        nextId++;
        return session;
    }

    private synchronized Handoff settleAndHandOff(ChargingSession session, boolean deliveredAll) {
        if (!ownsBay(session)) {
            return new Handoff(null, null);
        }
        ChargingBay bay = session.getBay();
        ChargingRequest request = session.getRequest();
        Throwable failure = null;
        try {
            if (deliveredAll && !session.isCancellationRequested()
                    && session.getDeliveredKwh() == request.getRequestedKwh()) {
                session.finish(ChargingSession.Status.COMPLETED);
                history.add(billing.complete(session, LocalDateTime.now(clock)));
            } else {
                session.finish(ChargingSession.Status.CANCELLED);
            }
        } catch (RuntimeException | Error completionFailure) {
            failure = completionFailure;
        }

        active.remove(bay.getId());
        openRegistrations.remove(request.getVehicle().getRegistration());

        // Reserve the same bay before another submitter can acquire the station lock.
        ChargingSession next = null;
        ChargingRequest queued;
        while ((queued = waiting.removeFirstCompatible(bay.getCharger().getType())) != null) {
            try {
                next = reserve(queued, bay);
                break;
            } catch (RuntimeException | Error reservationFailure) {
                openRegistrations.remove(queued.getVehicle().getRegistration());
                if (failure == null) {
                    failure = reservationFailure;
                }
            }
        }
        notifyAll();
        return new Handoff(next, failure);
    }

    private void startWorker(ChargingSession session, Throwable failure) {
        while (session != null) {
            Thread worker;
            synchronized (this) {
                worker = session.getWorker();
            }
            try {
                worker.start();
                break;
            } catch (RuntimeException | Error startFailure) {
                if (failure == null) {
                    failure = startFailure;
                }
                session = settleAndHandOff(session, false).next();
            }
        }
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
    }

    private static ChargingBay[][] copyGrid(ChargingBay[][] layout) {
        Objects.requireNonNull(layout, "Bay layout is required.");
        if (layout.length == 0) {
            throw new IllegalArgumentException("The layout must have at least one bay.");
        }
        ChargingBay[][] copy = new ChargingBay[layout.length][];
        HashSet<String> ids = new HashSet<>();
        for (int row = 0; row < layout.length; row++) {
            ChargingBay[] sourceRow = layout[row];
            if (sourceRow == null || sourceRow.length == 0) {
                throw new IllegalArgumentException("Each layout row must contain bays.");
            }
            copy[row] = sourceRow.clone();
            for (ChargingBay bay : copy[row]) {
                if (bay == null || !ids.add(bay.getId())) {
                    throw new IllegalArgumentException("The layout requires non-null bays with unique IDs.");
                }
            }
        }
        return copy;
    }
}

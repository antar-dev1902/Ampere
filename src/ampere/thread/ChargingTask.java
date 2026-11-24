package ampere.thread;

import ampere.session.ChargingSession;
import ampere.station.ChargingStation;
import java.util.Objects;

public final class ChargingTask implements Runnable {
    @FunctionalInterface
    public interface TickDelay {
        void awaitTick(String registration) throws InterruptedException;
    }

    private final ChargingStation station;
    private final ChargingSession session;
    private final TickDelay delay;

    public ChargingTask(ChargingStation station, ChargingSession session, TickDelay delay) {
        this.station = Objects.requireNonNull(station, "Charging station is required.");
        this.session = Objects.requireNonNull(session, "Charging session is required.");
        this.delay = Objects.requireNonNull(delay, "Tick delay is required.");
    }

    @Override
    public void run() {
        boolean deliveredAll = false;
        try {
            String registration = session.getRequest().getVehicle().getRegistration();
            while (!deliveredAll && station.canCharge(session)) {
                delay.awaitTick(registration);
                deliveredAll = station.tick(session);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            station.finish(session, deliveredAll);
        }
    }
}

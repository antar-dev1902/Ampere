package ampere.session;

import ampere.request.ChargingRequest;
import ampere.station.ChargingBay;
import java.time.LocalDateTime;

/**
 * One charging session on one bay.
 * Only ChargingStation changes it, and always inside its synchronized lock.
 */
public class ChargingSession {

    public enum Status {
        CHARGING, COMPLETED, CANCELLED
    }

    private final long id;
    private final ChargingRequest request;
    private final ChargingBay bay;
    private final LocalDateTime startedAt;
    private final int ratePerKwh;

    private int deliveredKwh;
    private Status status;
    private boolean cancellationRequested;
    private Thread worker;

    public ChargingSession(long id, ChargingRequest request, ChargingBay bay,
                           LocalDateTime startedAt, int ratePerKwh) {
        if (id <= 0) {
            throw new IllegalArgumentException("Session ID must be positive.");
        }
        if (request == null || bay == null || startedAt == null) {
            throw new IllegalArgumentException("Request, bay and start time are required.");
        }
        if (ratePerKwh <= 0) {
            throw new IllegalArgumentException("Rate per kWh must be positive.");
        }
        this.id = id;
        this.request = request;
        this.bay = bay;
        this.startedAt = startedAt;
        this.ratePerKwh = ratePerKwh;
        this.deliveredKwh = 0;
        this.status = Status.CHARGING;
        this.cancellationRequested = false;
        this.worker = null;
    }

    public long getId() {
        return id;
    }

    public ChargingRequest getRequest() {
        return request;
    }

    public ChargingBay getBay() {
        return bay;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public int getRatePerKwh() {
        return ratePerKwh;
    }

    public int getDeliveredKwh() {
        return deliveredKwh;
    }

    public Status getStatus() {
        return status;
    }

    public Thread getWorker() {
        return worker;
    }

    public boolean isCancellationRequested() {
        return cancellationRequested;
    }

    public void attachWorker(Thread worker) {
        if (worker == null) {
            throw new IllegalArgumentException("Worker thread must not be null.");
        }
        if (this.worker != null) {
            throw new IllegalStateException("Worker is already attached.");
        }
        this.worker = worker;
    }

    public void addEnergy(int amount) {
        if (status != Status.CHARGING) {
            throw new IllegalStateException("Session has already finished.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Energy must be positive.");
        }
        // Subtraction avoids int overflow.
        if (amount > request.getRequestedKwh() - deliveredKwh) {
            throw new IllegalArgumentException("Delivered energy cannot exceed requested kWh.");
        }
        deliveredKwh = deliveredKwh + amount;
    }

    public void requestCancellation() {
        if (status == Status.CHARGING) {
            cancellationRequested = true;
        }
    }

    public void finish(Status terminalStatus) {
        if (terminalStatus != Status.COMPLETED && terminalStatus != Status.CANCELLED) {
            throw new IllegalArgumentException("Status must be COMPLETED or CANCELLED.");
        }
        if (status != Status.CHARGING) {
            throw new IllegalStateException("Session has already finished.");
        }
        status = terminalStatus;
    }
}

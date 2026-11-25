package ampere.session;

import ampere.charger.Charger;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An immutable record of a completed charging session.
 * Created once by {@code BillingService.complete} when the station
 * accepts a successful completion. Cancelled sessions produce no receipt.
 *
 * <p>Implements {@link Serializable} for binary backup and
 * {@link Comparable} with natural ordering by ascending session ID.
 */
public final class Receipt implements Serializable, Comparable<Receipt> {

    private static final long serialVersionUID = 1L;

    private final long sessionId;
    private final String registration;
    private final String driverName;
    private final String bayId;
    private final Charger.Type type;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final int deliveredKwh;
    private final int ratePerKwh;
    private final long totalRupees;

    public Receipt(long sessionId, String registration, String driverName,
                   String bayId, Charger.Type type, LocalDateTime startedAt,
                   LocalDateTime finishedAt, int deliveredKwh, int ratePerKwh,
                   long totalRupees) {

        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive.");
        }
        if (registration == null || registration.isBlank()) {
            throw new IllegalArgumentException("Registration must not be blank.");
        }
        if (driverName == null || driverName.isBlank()) {
            throw new IllegalArgumentException("Driver name must not be blank.");
        }
        if (bayId == null || bayId.isBlank()) {
            throw new IllegalArgumentException("Bay ID must not be blank.");
        }
        Objects.requireNonNull(type, "Charger type is required.");
        Objects.requireNonNull(startedAt, "Start time is required.");
        Objects.requireNonNull(finishedAt, "Finish time is required.");
        if (deliveredKwh <= 0) {
            throw new IllegalArgumentException("Delivered energy must be positive.");
        }
        if (ratePerKwh <= 0) {
            throw new IllegalArgumentException("Rate per kWh must be positive.");
        }
        long expectedTotal = (long) deliveredKwh * ratePerKwh;
        if (totalRupees != expectedTotal) {
            throw new IllegalArgumentException(
                    "Total must equal deliveredKwh * ratePerKwh ("
                    + expectedTotal + "), got " + totalRupees + ".");
        }

        this.sessionId = sessionId;
        this.registration = registration;
        this.driverName = driverName;
        this.bayId = bayId;
        this.type = type;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.deliveredKwh = deliveredKwh;
        this.ratePerKwh = ratePerKwh;
        this.totalRupees = totalRupees;
    }

    // ---- Getters ----

    public long getSessionId()          { return sessionId; }
    public String getRegistration()     { return registration; }
    public String getDriverName()       { return driverName; }
    public String getBayId()            { return bayId; }
    public Charger.Type getType()       { return type; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt(){ return finishedAt; }
    public int getDeliveredKwh()        { return deliveredKwh; }
    public int getRatePerKwh()          { return ratePerKwh; }
    public long getTotalRupees()        { return totalRupees; }

    // ---- Display ----

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Receipt #").append(sessionId).append('\n');
        sb.append("  Driver      : ").append(driverName).append('\n');
        sb.append("  Registration: ").append(registration).append('\n');
        sb.append("  Bay         : ").append(bayId).append('\n');
        sb.append("  Type        : ").append(type).append('\n');
        sb.append("  Started     : ").append(startedAt).append('\n');
        sb.append("  Finished    : ").append(finishedAt).append('\n');
        sb.append("  Delivered   : ").append(deliveredKwh).append(" kWh\n");
        sb.append("  Rate        : ").append(ratePerKwh).append(" Rs/kWh\n");
        sb.append("  Total       : ").append(totalRupees).append(" Rs");
        return sb.toString();
    }

    // ---- Natural ordering (ascending session ID) ----

    @Override
    public int compareTo(Receipt other) {
        return Long.compare(this.sessionId, other.sessionId);
    }

    // ---- Value-based equals/hashCode using all persisted fields ----

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Receipt other)) return false;
        return sessionId == other.sessionId
                && deliveredKwh == other.deliveredKwh
                && ratePerKwh == other.ratePerKwh
                && totalRupees == other.totalRupees
                && registration.equals(other.registration)
                && driverName.equals(other.driverName)
                && bayId.equals(other.bayId)
                && type == other.type
                && startedAt.equals(other.startedAt)
                && finishedAt.equals(other.finishedAt);
    }

    @Override
    public int hashCode() {
        int h = Long.hashCode(sessionId);
        h = 31 * h + registration.hashCode();
        h = 31 * h + driverName.hashCode();
        h = 31 * h + bayId.hashCode();
        h = 31 * h + type.hashCode();
        h = 31 * h + startedAt.hashCode();
        h = 31 * h + finishedAt.hashCode();
        h = 31 * h + Integer.hashCode(deliveredKwh);
        h = 31 * h + Integer.hashCode(ratePerKwh);
        h = 31 * h + Long.hashCode(totalRupees);
        return h;
    }

    @Override
    public String toString() {
        return summary();
    }
}

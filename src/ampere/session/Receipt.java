package ampere.session;

import ampere.charger.Charger;
import java.time.LocalDateTime;

/**
 * Immutable bill for one completed session (all fields final, no setters).
 * Natural ordering is ascending session ID.
 */
public final class Receipt implements Comparable<Receipt> {

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

    public Receipt(long sessionId, String registration, String driverName, String bayId,
                   Charger.Type type, LocalDateTime startedAt, LocalDateTime finishedAt,
                   int deliveredKwh, int ratePerKwh, long totalRupees) {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive.");
        }
        if (isBlank(registration) || isBlank(driverName) || isBlank(bayId)) {
            throw new IllegalArgumentException("Registration, driver name and bay ID are required.");
        }
        if (type == null || startedAt == null || finishedAt == null) {
            throw new IllegalArgumentException("Type, start time and finish time are required.");
        }
        if (deliveredKwh <= 0 || ratePerKwh <= 0) {
            throw new IllegalArgumentException("Energy and rate must be positive.");
        }
        if (totalRupees != (long) deliveredKwh * ratePerKwh) {
            throw new IllegalArgumentException("Total must equal deliveredKwh * ratePerKwh.");
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

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public long getSessionId() {
        return sessionId;
    }

    public String getRegistration() {
        return registration;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getBayId() {
        return bayId;
    }

    public Charger.Type getType() {
        return type;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public int getDeliveredKwh() {
        return deliveredKwh;
    }

    public int getRatePerKwh() {
        return ratePerKwh;
    }

    public long getTotalRupees() {
        return totalRupees;
    }

    public String summary() {
        StringBuilder text = new StringBuilder();
        text.append("Receipt #").append(sessionId).append("\n");
        text.append("  Driver      : ").append(driverName).append("\n");
        text.append("  Registration: ").append(registration).append("\n");
        text.append("  Bay         : ").append(bayId).append(" (").append(type).append(")\n");
        text.append("  Started     : ").append(startedAt).append("\n");
        text.append("  Finished    : ").append(finishedAt).append("\n");
        text.append("  Delivered   : ").append(deliveredKwh).append(" kWh\n");
        text.append("  Rate        : ").append(ratePerKwh).append(" Rs/kWh\n");
        text.append("  Total       : ").append(totalRupees).append(" Rs");
        return text.toString();
    }

    @Override
    public int compareTo(Receipt other) {
        return Long.compare(sessionId, other.sessionId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Receipt other = (Receipt) obj;
        return sessionId == other.sessionId
                && registration.equals(other.registration)
                && driverName.equals(other.driverName)
                && bayId.equals(other.bayId)
                && type == other.type
                && startedAt.equals(other.startedAt)
                && finishedAt.equals(other.finishedAt)
                && deliveredKwh == other.deliveredKwh
                && ratePerKwh == other.ratePerKwh
                && totalRupees == other.totalRupees;
    }

    @Override
    public int hashCode() {
        // Equal receipts have equal session IDs, so this is consistent with equals.
        return Long.hashCode(sessionId);
    }

    @Override
    public String toString() {
        return summary();
    }
}

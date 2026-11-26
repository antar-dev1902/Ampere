package ampere.billing;

import ampere.charger.Charger;
import ampere.session.ChargingSession;
import ampere.session.Receipt;
import java.time.LocalDateTime;

/**
 * Finds the rate for a session and builds its receipt.
 * First evaluation: rate is the charger's base rate (AC 12, DC 18).
 */
public class BillingService {

    public BillingService() {
    }

    public int rateAt(Charger charger, LocalDateTime startedAt) {
        if (charger == null || startedAt == null) {
            throw new IllegalArgumentException("Charger and start time are required.");
        }
        return charger.getRatePerKwh();
    }

    public Receipt complete(ChargingSession session, LocalDateTime completedAt) {
        if (session == null || completedAt == null) {
            throw new IllegalArgumentException("Session and completion time are required.");
        }
        int delivered = session.getDeliveredKwh();
        int rate = session.getRatePerKwh();
        long total = (long) delivered * rate;

        return new Receipt(
                session.getId(),
                session.getRequest().getVehicle().getRegistration(),
                session.getRequest().getVehicle().getDriverName(),
                session.getBay().getId(),
                session.getBay().getCharger().getType(),
                session.getStartedAt(),
                completedAt,
                delivered,
                rate,
                total);
    }
}

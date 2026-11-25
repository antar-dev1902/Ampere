package ampere.billing;

import ampere.charger.Charger;
import ampere.session.ChargingSession;
import ampere.session.Receipt;
import java.time.LocalDateTime;

/**
 * Provides tariff lookup and receipt generation for completed charging sessions.
 *
 * <p><b>E1 behaviour:</b> {@code rateAt} returns the charger's base rate
 * (AC&nbsp;12&nbsp;Rs/kWh, DC&nbsp;18&nbsp;Rs/kWh). The FINAL milestone adds a
 * 3&nbsp;Rs/kWh peak surcharge for sessions starting 18:00–21:59.
 *
 * <p>{@code complete} reads the session's <em>preserved</em> rate (locked in at
 * bay-assignment time) and delivered energy to build an immutable {@link Receipt}.
 * The station decides whether a receipt should be created; cancelled sessions
 * produce none.
 */
public class BillingService {

    public BillingService() {
        // No external dependencies for E1.
    }

    /**
     * Returns the applicable rate in rupees per kWh for the given charger
     * at the given session-start time.
     *
     * <p>E1: returns the charger's base rate. FINAL: adds 3 Rs/kWh when
     * {@code startedAt} is at 18:00 (inclusive) through 22:00 (exclusive).
     *
     * @param charger   the charger assigned to the session
     * @param startedAt the time charging started (used for peak detection)
     * @return the rate in Rs/kWh
     */
    public int rateAt(Charger charger, LocalDateTime startedAt) {
        if (charger == null) {
            throw new IllegalArgumentException("Charger must not be null.");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("Start time must not be null.");
        }
        // E1: base rate only. FINAL will add peak-window logic here.
        return charger.getRatePerKwh();
    }

    /**
     * Builds an immutable {@link Receipt} from a completed session.
     *
     * <p>Uses the session's preserved rate (set at bay assignment) and
     * delivered energy. The total is {@code (long) deliveredKwh * ratePerKwh}.
     *
     * @param session     the completed session (status must be CHARGING or
     *                    about to transition to COMPLETED by the station)
     * @param completedAt the time the last tick finished
     * @return a new immutable receipt
     */
    public Receipt complete(ChargingSession session, LocalDateTime completedAt) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null.");
        }
        if (completedAt == null) {
            throw new IllegalArgumentException("Completion time must not be null.");
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
                total
        );
    }
}

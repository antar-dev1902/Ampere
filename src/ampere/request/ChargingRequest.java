package ampere.request;

import ampere.charger.Charger;
import ampere.vehicle.Vehicle;
import java.time.LocalDateTime;

/**
 * Immutable charging request for a registered vehicle.
 */
public final class ChargingRequest {

    private final Vehicle vehicle;
    private final Charger.Type type;
    private final int startingKwh;
    private final int requestedKwh;
    private final LocalDateTime arrivedAt;

    public ChargingRequest(Vehicle vehicle,
                           Charger.Type type,
                           int startingKwh,
                           int requestedKwh,
                           LocalDateTime arrivedAt) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle is required.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Charger type is required.");
        }
        if (arrivedAt == null) {
            throw new IllegalArgumentException("Arrival time is required.");
        }
        if (!vehicle.supports(type)) {
            throw new IllegalArgumentException("Vehicle does not support " + type + " charging.");
        }
        if (startingKwh < 0) {
            throw new IllegalArgumentException("Starting energy cannot be negative.");
        }
        if (startingKwh > vehicle.getCapacityKwh()) {
            throw new IllegalArgumentException("Starting energy cannot exceed battery capacity.");
        }
        if (requestedKwh <= 0) {
            throw new IllegalArgumentException("Requested energy must be positive.");
        }
        // Use subtraction so integer addition cannot overflow.
        if (requestedKwh > vehicle.getCapacityKwh() - startingKwh) {
            throw new IllegalArgumentException("Requested energy exceeds remaining battery capacity.");
        }

        this.vehicle = vehicle;
        this.type = type;
        this.startingKwh = startingKwh;
        this.requestedKwh = requestedKwh;
        this.arrivedAt = arrivedAt;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Charger.Type getType() {
        return type;
    }

    public int getStartingKwh() {
        return startingKwh;
    }

    public int getRequestedKwh() {
        return requestedKwh;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }

    @Override
    public String toString() {
        return vehicle.getRegistration() + " [" + type + ", +" + requestedKwh + " kWh from " + startingKwh + " kWh]";
    }
}

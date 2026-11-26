package ampere.vehicle;

import ampere.charger.Charger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable model for a registered electric vehicle.
 */
public final class Vehicle {

    private final String driverName;
    private final String registration;
    private final Set<Charger.Type> supportedTypes;
    private final int capacityKwh;

    public Vehicle(String driverName,
                   String registration,
                   Set<Charger.Type> supportedTypes,
                   int capacityKwh) {
        if (driverName == null || driverName.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver name must not be blank.");
        }
        if (registration == null || registration.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration must not be blank.");
        }
        if (supportedTypes == null || supportedTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one supported charger type is required.");
        }
        if (capacityKwh <= 0) {
            throw new IllegalArgumentException("Battery capacity must be positive.");
        }

        Set<Charger.Type> copy = new HashSet<>();
        for (Charger.Type type : supportedTypes) {
            if (type == null) {
                throw new IllegalArgumentException("Supported charger type must not be null.");
            }
            copy.add(type);
        }

        this.driverName = driverName.trim();
        this.registration = registration.trim();
        this.supportedTypes = Collections.unmodifiableSet(copy);
        this.capacityKwh = capacityKwh;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getRegistration() {
        return registration;
    }

    public Set<Charger.Type> getSupportedTypes() {
        return supportedTypes;
    }

    public int getCapacityKwh() {
        return capacityKwh;
    }

    public boolean supports(Charger.Type type) {
        return type != null && supportedTypes.contains(type);
    }

    @Override
    public String toString() {
        return registration + " (" + driverName + ", " + supportedTypes + ", " + capacityKwh + " kWh)";
    }
}

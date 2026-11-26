package ampere.vehicle;

import ampere.charger.Charger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Stores registered vehicles in insertion order using LinkedHashMap.
 */
public class VehicleRegistry {

    private final Map<String, Vehicle> vehicles = new LinkedHashMap<>();

    public VehicleRegistry() {
    }

    public Vehicle register(String driverName,
                            String registration,
                            Set<Charger.Type> supportedTypes,
                            int capacityKwh) {
        String validDriver = validateDriverName(driverName);
        String normalized = normalizeAndValidateRegistration(registration);

        if (vehicles.containsKey(normalized)) {
            throw new IllegalArgumentException("Vehicle already registered: " + normalized);
        }

        Vehicle vehicle = new Vehicle(validDriver, normalized, supportedTypes, capacityKwh);
        vehicles.put(normalized, vehicle);
        return vehicle;
    }

    public Vehicle find(String registration) {
        String normalized = normalizeAndValidateRegistration(registration);
        return vehicles.get(normalized);
    }

    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(new ArrayList<>(vehicles.values()));
    }

    private static String validateDriverName(String driverName) {
        if (driverName == null || driverName.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver name must not be blank.");
        }
        String trimmed = driverName.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isISOControl(trimmed.charAt(i))) {
                throw new IllegalArgumentException("Driver name must not contain control characters.");
            }
        }
        return trimmed;
    }

    private static String normalizeAndValidateRegistration(String registration) {
        if (registration == null) {
            throw new IllegalArgumentException("Invalid registration. Use 4-12 letters/digits (e.g. PB01AB1234).");
        }

        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < registration.length(); i++) {
            char ch = registration.charAt(i);
            if (!Character.isWhitespace(ch) && ch != '-') {
                cleaned.append(ch);
            }
        }

        String normalized = cleaned.toString().toUpperCase(Locale.ROOT);
        int length = normalized.length();
        if (length < 4 || length > 12) {
            throw new IllegalArgumentException("Invalid registration. Use 4-12 letters/digits (e.g. PB01AB1234).");
        }

        for (int i = 0; i < length; i++) {
            char ch = normalized.charAt(i);
            boolean isDigit = ch >= '0' && ch <= '9';
            boolean isLetter = ch >= 'A' && ch <= 'Z';
            if (!isDigit && !isLetter) {
                throw new IllegalArgumentException("Invalid registration. Use 4-12 letters/digits (e.g. PB01AB1234).");
            }
        }
        return normalized;
    }
}

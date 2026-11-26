package ampere.app;

import ampere.charger.Charger;
import ampere.request.ChargingRequest;
import ampere.session.Receipt;
import ampere.station.ChargingStation;
import ampere.vehicle.Vehicle;
import ampere.vehicle.VehicleRegistry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

/**
 * Simple console menu for registering vehicles, submitting charging requests,
 * viewing station state, and printing completed receipts.
 */
public class ConsoleMenu {

    private final Scanner scanner;
    private final VehicleRegistry registry;
    private final ChargingStation station;

    public ConsoleMenu(Scanner scanner, VehicleRegistry registry, ChargingStation station) {
        if (scanner == null || registry == null || station == null) {
            throw new IllegalArgumentException("Scanner, registry, and station are required.");
        }
        this.scanner = scanner;
        this.registry = registry;
        this.station = station;
    }

    public void run() throws InterruptedException {
        while (true) {
            printMenu();
            String choice = readLine();
            if (choice == null) {
                System.out.println("End of input. Draining accepted work...");
                return;
            }

            String trimmed = choice.trim();
            if ("1".equals(trimmed)) {
                handleRegister();
            } else if ("2".equals(trimmed)) {
                System.out.println(station.describeState());
            } else if ("3".equals(trimmed)) {
                handleRequest();
            } else if ("4".equals(trimmed)) {
                System.out.println(station.describeState());
            } else if ("5".equals(trimmed)) {
                handleHistory();
            } else if ("6".equals(trimmed)) {
                station.awaitIdle();
                System.out.println("Station is idle.");
            } else if ("0".equals(trimmed)) {
                System.out.println("Exiting. Draining accepted work...");
                return;
            } else {
                System.out.println("Invalid choice. Enter 0 to 6.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("--- Ampere Menu ---");
        System.out.println("1 Register");
        System.out.println("2 Bays");
        System.out.println("3 Request");
        System.out.println("4 Active and waiting");
        System.out.println("5 History");
        System.out.println("6 Wait until idle");
        System.out.println("0 Exit");
        System.out.print("Choice: ");
    }

    private void handleRegister() {
        String driverName = promptDriverName();
        if (driverName == null) {
            return;
        }

        String registration = promptValidRegistration();
        if (registration == null) {
            return;
        }

        Set<Charger.Type> supportedTypes = promptSupportedTypes();
        if (supportedTypes == null) {
            return;
        }

        Integer capacity = promptPositiveInt("Battery capacity (kWh): ");
        if (capacity == null) {
            return;
        }

        try {
            Vehicle vehicle = registry.register(driverName, registration, supportedTypes, capacity);
            System.out.println("Registered " + vehicle.getRegistration());
        } catch (IllegalArgumentException exception) {
            System.out.println("Registration rejected: " + exception.getMessage());
        }
    }

    private void handleRequest() {
        String rawRegistration = promptNonBlank("Registration: ");
        if (rawRegistration == null) {
            return;
        }

        Vehicle vehicle;
        try {
            vehicle = registry.find(rawRegistration);
        } catch (IllegalArgumentException exception) {
            System.out.println("Invalid registration: " + exception.getMessage());
            return;
        }

        if (vehicle == null) {
            System.out.println("Vehicle not found. Please register it first.");
            return;
        }

        Charger.Type type = promptChargerType();
        if (type == null) {
            return;
        }

        Integer startingKwh = promptInt("Starting energy (kWh): ");
        if (startingKwh == null) {
            return;
        }

        Integer requestedKwh = promptInt("Additional energy (kWh): ");
        if (requestedKwh == null) {
            return;
        }

        try {
            ChargingRequest request = new ChargingRequest(
                    vehicle, type, startingKwh, requestedKwh, LocalDateTime.now());
            ChargingStation.Submission submission = station.submit(request);
            if (submission.started()) {
                System.out.println("Started session #" + submission.sessionId()
                        + " on bay " + submission.bayId()
                        + " for " + vehicle.getRegistration());
            } else {
                System.out.println("Queued request for " + vehicle.getRegistration());
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            System.out.println("Request rejected: " + exception.getMessage());
        }
    }

    private void handleHistory() {
        List<Receipt> history = new ArrayList<>(station.getHistory());
        if (history.isEmpty()) {
            System.out.println("No completed sessions yet.");
            return;
        }
        Collections.sort(history);
        Iterator<Receipt> iterator = history.iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next().summary());
            System.out.println();
        }
    }

    private String promptDriverName() {
        while (true) {
            System.out.print("Driver name: ");
            String line = readLine();
            if (line == null) {
                return null;
            }
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !hasControlChars(trimmed)) {
                return trimmed;
            }
            System.out.println("Driver name must not be blank or contain control characters.");
        }
    }

    private String promptValidRegistration() {
        while (true) {
            System.out.print("Registration (e.g. PB01AB1234): ");
            String line = readLine();
            if (line == null) {
                return null;
            }
            try {
                registry.find(line);
                return line;
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            }
        }
    }

    private Set<Charger.Type> promptSupportedTypes() {
        while (true) {
            System.out.print("Supported charger type (AC, DC, or BOTH): ");
            String line = readLine();
            if (line == null) {
                return null;
            }
            String normalized = line.trim().toUpperCase(Locale.ROOT);
            Set<Charger.Type> types = new HashSet<>();
            if ("AC".equals(normalized)) {
                types.add(Charger.Type.AC);
                return types;
            } else if ("DC".equals(normalized)) {
                types.add(Charger.Type.DC);
                return types;
            } else if ("BOTH".equals(normalized)) {
                types.add(Charger.Type.AC);
                types.add(Charger.Type.DC);
                return types;
            }
            System.out.println("Invalid charger option. Enter AC, DC, or BOTH.");
        }
    }

    private Charger.Type promptChargerType() {
        while (true) {
            System.out.print("Charger type (AC or DC): ");
            String line = readLine();
            if (line == null) {
                return null;
            }
            String normalized = line.trim().toUpperCase(Locale.ROOT);
            if ("AC".equals(normalized)) {
                return Charger.Type.AC;
            } else if ("DC".equals(normalized)) {
                return Charger.Type.DC;
            }
            System.out.println("Invalid charger type. Enter AC or DC.");
        }
    }

    private String promptNonBlank(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = readLine();
            if (line == null) {
                return null;
            }
            if (!line.trim().isEmpty()) {
                return line.trim();
            }
            System.out.println("Input must not be blank.");
        }
    }

    private Integer promptPositiveInt(String prompt) {
        while (true) {
            Integer value = promptInt(prompt);
            if (value == null) {
                return null;
            }
            if (value > 0) {
                return value;
            }
            System.out.println("Value must be a positive whole number.");
        }
    }

    private Integer promptInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = readLine();
            if (line == null) {
                return null;
            }
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException exception) {
                System.out.println("Invalid number. Enter a whole integer.");
            }
        }
    }

    private String readLine() {
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine();
    }

    private static boolean hasControlChars(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isISOControl(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}

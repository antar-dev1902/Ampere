package ampere.app;

import ampere.station.ChargingStation;
import ampere.vehicle.VehicleRegistry;
import java.util.Scanner;

/**
 * Entry point for the Ampere EV Charging Station Management and Billing System (E1).
 */
public final class AmpereApp {

    private AmpereApp() {
    }

    public static void main(String[] args) {
        VehicleRegistry registry = new VehicleRegistry();
        ChargingStation station = new ChargingStation();

        System.out.println("Ampere EV Charging Station Management System (E1)");
        System.out.println("Note: Vehicle registrations are in memory for this run; re-register after restart.");

        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleMenu menu = new ConsoleMenu(scanner, registry, station);
            menu.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.out.println("Session interrupted.");
        } finally {
            station.stopAccepting();
            try {
                station.awaitIdle();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while draining active charging work.");
            }
        }
    }
}

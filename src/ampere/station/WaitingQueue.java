package ampere.station;

import ampere.charger.Charger;
import ampere.request.ChargingRequest;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/** Holds pending requests. ChargingStation calls these methods under its lock. */
public final class WaitingQueue {
    private final LinkedList<ChargingRequest> requests;

    public WaitingQueue() {
        requests = new LinkedList<>();
    }

    public void add(ChargingRequest request) {
        requests.addLast(Objects.requireNonNull(request, "Charging request is required."));
    }

    public ChargingRequest removeFirstCompatible(Charger.Type type) {
        Objects.requireNonNull(type, "Charger type is required.");
        Iterator<ChargingRequest> iterator = requests.iterator();
        while (iterator.hasNext()) {
            ChargingRequest request = iterator.next();
            if (request.getType() == type) {
                iterator.remove();
                return request;
            }
        }
        return null;
    }

    public List<ChargingRequest> snapshot() {
        return List.copyOf(requests);
    }

    public boolean isEmpty() {
        return requests.isEmpty();
    }
}

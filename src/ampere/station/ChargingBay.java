package ampere.station;

import ampere.charger.Charger;
import java.util.Objects;

public final class ChargingBay {
    private final String id;
    private final Charger charger;

    public ChargingBay(String id, Charger charger) {
        this.id = Objects.requireNonNull(id, "Bay ID is required.");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Bay ID cannot be blank.");
        }
        this.charger = Objects.requireNonNull(charger, "Bay charger is required.");
    }

    public String getId() {
        return id;
    }

    public Charger getCharger() {
        return charger;
    }
}

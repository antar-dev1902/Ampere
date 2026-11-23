package ampere.charger;

import java.util.Objects;

public abstract class Charger {
    public enum Type {
        AC,
        DC
    }

    private final Type type;

    protected Charger(Type type) {
        this.type = Objects.requireNonNull(type, "Charger type is required.");
    }

    public Type getType() {
        return type;
    }

    public abstract int getRatePerKwh();
}

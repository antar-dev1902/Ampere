package ampere.charger;

public class DCFastCharger extends Charger {
    public DCFastCharger() {
        super(Type.DC);
    }

    @Override
    public int getRatePerKwh() {
        return 18;
    }
}

package ampere.charger;

public class ACCharger extends Charger {
    public ACCharger() {
        super(Type.AC);
    }

    @Override
    public int getRatePerKwh() {
        return 12;
    }
}

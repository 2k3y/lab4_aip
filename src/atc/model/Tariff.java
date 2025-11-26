package atc.model;
import java.util.Locale;
import java.util.Objects;


public class Tariff {
    private String city;
    private TariffType type;
    private double pricePerMinute;
    private double discountPercent;

    public Tariff(String city, TariffType type, double pricePerMinute, double discountPercent) {
        this.city = city;
        this.type = type;
        this.pricePerMinute = pricePerMinute;
        this.discountPercent = discountPercent;
    }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public TariffType getType() { return type; }
    public void setType(TariffType type) { this.type = type; }

    public double getPricePerMinute() { return pricePerMinute; }
    public void setPricePerMinute(double pricePerMinute) { this.pricePerMinute = pricePerMinute; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    public double finalPrice() {
        if (type == TariffType.PRIVILEGED) {
            double k = 1.0 - (discountPercent / 100.0);
            if (k < 0) k = 0;
            return pricePerMinute * k;
        }
        return pricePerMinute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tariff t)) return false;
        return Double.compare(pricePerMinute, t.pricePerMinute) == 0
                && Double.compare(discountPercent, t.discountPercent) == 0
                && city.trim().equalsIgnoreCase(t.city.trim())
                && type == t.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(city.trim().toLowerCase(Locale.ROOT), type,
                Double.valueOf(pricePerMinute), Double.valueOf(discountPercent));
    }
}

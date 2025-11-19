package atc.model;

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
}

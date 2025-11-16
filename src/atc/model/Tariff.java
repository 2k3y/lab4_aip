package atc.model;

public class Tariff {
    private String city;
    private TariffType type;
    private double pricePerMinute;   // базовая цена, руб/мин
    private double discountPercent;  // 0..100, используется только для льготного

    public Tariff(String city, TariffType type, double pricePerMinute, double discountPercent) {
        this.city = city;
        this.type = type;
        this.pricePerMinute = pricePerMinute;
        this.discountPercent = discountPercent;
    }

    // --- Геттеры/сеттеры
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public TariffType getType() { return type; }
    public void setType(TariffType type) { this.type = type; }

    public double getPricePerMinute() { return pricePerMinute; }
    public void setPricePerMinute(double pricePerMinute) { this.pricePerMinute = pricePerMinute; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    // Итоговая цена с учётом стратегии
    public double finalPrice() {
        if (type == TariffType.PRIVILEGED) {
            double k = 1.0 - (discountPercent / 100.0);
            if (k < 0) k = 0;
            return pricePerMinute * k;
        }
        return pricePerMinute;
    }
}

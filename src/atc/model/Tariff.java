package atc.model;

public class Tariff {
    // Для БД/репозитория: может быть null для новых записей
    private Long id;

    private String city;
    private TariffType type;
    private double pricePerMinute;
    private double discountPercent;

    // Конструктор для «новых» (id ещё нет)
    public Tariff(String city, TariffType type, double price, double discount) {
        this(null, city, type, price, discount);
    }

    // Конструктор для «существующих» (id уже есть)
    public Tariff(Long id, String city, TariffType type, double price, double discount) {
        this.id = id;
        this.city = city;
        this.type = type;
        this.pricePerMinute = price;
        this.discountPercent = discount;
    }

    // --- геттеры/сеттеры ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
            return round2(pricePerMinute * (1.0 - discountPercent / 100.0));
        }
        return round2(pricePerMinute);
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}

package atc.service;

import atc.model.Tariff;
import atc.model.TariffType;

import java.util.ArrayList;
import java.util.List;

public class TariffManager {
    private final List<Tariff> tariffs = new ArrayList<>();

    public List<Tariff> getTariffs() { return tariffs; }

    public void add(Tariff t) { tariffs.add(t); }

    public void update(int index, Tariff t) {
        if (index < 0 || index >= tariffs.size()) throw new TariffException("Неверный индекс");
        tariffs.set(index, t);
    }

    public void remove(int index) {
        if (index < 0 || index >= tariffs.size()) throw new TariffException("Неверный индекс");
        tariffs.remove(index);
    }

    public void clear() { tariffs.clear(); }

    public double averageFinalPrice() {
        if (tariffs.isEmpty()) throw new TariffException("Нет тарифов");
        double sum = 0;
        for (Tariff t : tariffs) sum += t.finalPrice();
        return sum / tariffs.size();
    }

    public double totalFinalPrice() {
        if (tariffs.isEmpty()) throw new TariffException("Нет тарифов");
        double sum = 0;
        for (Tariff t : tariffs) sum += t.finalPrice();
        return sum;
    }

    public void increaseAllPrices(double percent) {
        double factor = 1.0 + percent / 100.0;
        if (factor <= 0.0) {
            throw new TariffException("Процент слишком мал: цена станет ≤ 0");
        }
        if (percent > 100) {
            throw new TariffException("Процент не должен превышать 100");
        }

        for (Tariff t : tariffs) {
            double newPrice = round2(t.getPricePerMinute() * factor);
            validatePrice(newPrice);
            t.setPricePerMinute(newPrice);
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public void addRegular(String city, double price) {
        validateCity(city);
        validatePrice(price);
        add(new Tariff(city.trim(), TariffType.REGULAR, price, 0.0));
    }

    public void addPrivileged(String city, double price, double discountPercent) {
        validateCity(city);
        validatePrice(price);
        validateDiscount(discountPercent);
        add(new Tariff(city.trim(), TariffType.PRIVILEGED, price, discountPercent));
    }

    public static void validateCity(String city) {
        if (city == null) throw new TariffException("Пустое название города");
        city = city.trim();
        if (city.isEmpty()) throw new TariffException("Пустое название города");
        if (city.length() < 2 || city.length() > 30)
            throw new TariffException("Название города: 2–30 символов");
        if (!city.matches("[\\p{L} .\\-]+"))
            throw new TariffException("Только буквы, пробел, точка, дефис");
    }

    public static void validatePrice(double price) {
        if (price <= 0 || price > 1_000) throw new TariffException("Цена должна быть > 0 и разумной");
    }

    public static void validateDiscount(double d) {
        if (d < 0 || d > 100) throw new TariffException("Скидка должна быть 0..100%");
    }
}

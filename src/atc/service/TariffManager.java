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
        double k = 1.0 + percent / 100.0;
        if (k < 0) throw new TariffException("Итоговый коэффициент цены < 0");
        for (Tariff t : tariffs) {
            t.setPricePerMinute(t.getPricePerMinute() * k);
        }
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
        if (city == null || city.trim().isEmpty()) throw new TariffException("Пустое название города");
    }

    public static void validatePrice(double price) {
        if (price <= 0 || price > 1_000_000) throw new TariffException("Цена должна быть > 0 и разумной");
    }

    public static void validateDiscount(double d) {
        if (d < 0 || d > 100) throw new TariffException("Скидка должна быть 0..100%");
    }
}

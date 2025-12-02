package atc.service;

import atc.data.InMemoryTariffRepository;
import atc.data.TariffRepository;
import atc.model.Tariff;
import atc.model.TariffType;

import java.util.ArrayList;
import java.util.List;

public class TariffManager {
    private final TariffRepository repo;

    private final List<Tariff> cache = new ArrayList<>();

    public TariffManager() {
        this(new InMemoryTariffRepository());
    }

    /** Для подмены на БД-репозиторий. */
    public TariffManager(TariffRepository repo) {
        this.repo = repo;
        refresh();
    }

    private void refresh() {
        cache.clear();
        cache.addAll(repo.findAll());
    }

    public List<Tariff> getTariffs() { return cache; }

    // ---------- ВАЛИДАЦИЯ ----------
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
        if (price < 0.01 || price > 1_000.00)
            throw new TariffException("Цена должна быть 0.01..1 000.00");
    }

    public static void validateDiscount(double d) {
        if (d < 0 || d > 100)
            throw new TariffException("Скидка должна быть в диапазоне 0..100%");
    }

    private static String normCity(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
    private static double r2(double v) { return Math.round(v * 100.0) / 100.0; }

    /** «Точный дубль» по смыслу домена (id игнорируем). */
    private static boolean sameTariff(Tariff a, Tariff b) {
        return a.getType() == b.getType()
                && normCity(a.getCity()).equalsIgnoreCase(normCity(b.getCity()))
                && Double.compare(r2(a.getPricePerMinute()), r2(b.getPricePerMinute())) == 0
                && Double.compare(r2(a.getDiscountPercent()), b.getType()==TariffType.PRIVILEGED ? r2(b.getDiscountPercent()) : 0.0) == 0;
    }

    // ---------- CRUD ----------
    public void add(Tariff t) {
        if (t == null) throw new TariffException("Тариф не задан");
        validateCity(t.getCity());
        validatePrice(t.getPricePerMinute());
        if (t.getType() == TariffType.PRIVILEGED) validateDiscount(t.getDiscountPercent());
        else t.setDiscountPercent(0.0);

        // защита от дублей (по текущему состоянию репозитория)
        for (Tariff x : repo.findAll()) {
            if (sameTariff(x, t)) {
                throw new TariffException("Такой тариф уже существует");
            }
        }

        repo.add(t);
        refresh();
    }

    /** Обновление по индексу строки в текущей таблице (UI-совместимость). */
    public void update(int index, Tariff t) {
        if (index < 0 || index >= cache.size()) throw new TariffException("Неверный индекс");
        Long id = cache.get(index).getId();

        validateCity(t.getCity());
        validatePrice(t.getPricePerMinute());
        if (t.getType() == TariffType.PRIVILEGED) validateDiscount(t.getDiscountPercent());
        else t.setDiscountPercent(0.0);

        // запретить дубль (кроме самой записи)
        for (Tariff x : repo.findAll()) {
            if (!x.getId().equals(id) && sameTariff(x, t)) {
                throw new TariffException("Такой тариф уже существует");
            }
        }

        Tariff toSave = new Tariff(id, t.getCity(), t.getType(), t.getPricePerMinute(), t.getDiscountPercent());
        repo.update(toSave);
        refresh();
    }

    public void remove(int index) {
        if (index < 0 || index >= cache.size()) throw new TariffException("Неверный индекс");
        Long id = cache.get(index).getId();
        repo.delete(id);
        refresh();
    }

    public void clear() {
        repo.deleteAll();
        refresh();
    }

    // ---------- агрегации/массовые операции ----------
    public double averageFinalPrice() {
        if (cache.isEmpty()) throw new TariffException("Нет тарифов");
        double sum = 0;
        for (Tariff t : cache) sum += t.finalPrice();
        return sum / cache.size();
    }

    public double totalFinalPrice() {
        if (cache.isEmpty()) throw new TariffException("Нет тарифов");
        double sum = 0;
        for (Tariff t : cache) sum += t.finalPrice();
        return sum;
    }

    public void increaseAllPrices(double percent) {
        double factor = 1.0 + percent / 100.0;
        if (factor <= 0.0) throw new TariffException("Процент слишком мал: цена станет ≤ 0");
        if (percent > 100) throw new TariffException("Процент не должен превышать 100");

        for (Tariff x : repo.findAll()) {
            double newPrice = r2(x.getPricePerMinute() * factor);
            validatePrice(newPrice);
            Tariff upd = new Tariff(x.getId(), x.getCity(), x.getType(), newPrice, x.getDiscountPercent());
            repo.update(upd);
        }
        refresh();
    }

    // удобные хелперы как раньше
    public void addRegular(String city, double price) {
        add(new Tariff(city.trim(), TariffType.REGULAR, price, 0.0));
    }
    public void addPrivileged(String city, double price, double discountPercent) {
        add(new Tariff(city.trim(), TariffType.PRIVILEGED, price, discountPercent));
    }
}

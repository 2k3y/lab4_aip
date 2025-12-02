package atc.data;

import atc.model.Tariff;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTariffRepository implements TariffRepository {
    private final List<Tariff> store = new ArrayList<>();
    private final AtomicLong seq = new AtomicLong(1);

    @Override
    public synchronized List<Tariff> findAll() {
        // возвращаем копию, чтобы снаружи не ломали внутренний список
        return new ArrayList<>(store);
    }

    @Override
    public synchronized Tariff add(Tariff t) {
        long id = seq.getAndIncrement();
        Tariff withId = new Tariff(id, t.getCity(), t.getType(), t.getPricePerMinute(), t.getDiscountPercent());
        store.add(withId);
        return withId;
    }

    @Override
    public synchronized void update(Tariff t) {
        if (t.getId() == null) throw new IllegalArgumentException("id is null");
        for (int i = 0; i < store.size(); i++) {
            if (Objects.equals(store.get(i).getId(), t.getId())) {
                store.set(i, t);
                return;
            }
        }
        throw new NoSuchElementException("not found id=" + t.getId());
    }

    @Override
    public synchronized void delete(long id) {
        store.removeIf(x -> Objects.equals(x.getId(), id));
    }

    @Override
    public synchronized void deleteAll() {
        store.clear();
    }

    @Override
    public synchronized Optional<Tariff> findById(long id) {
        return store.stream().filter(x -> Objects.equals(x.getId(), id)).findFirst();
    }
}

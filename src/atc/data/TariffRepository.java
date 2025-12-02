package atc.data;

import atc.model.Tariff;
import java.util.List;
import java.util.Optional;

public interface TariffRepository {
    /** Полный список тарифов в «естественном» порядке (для UI). */
    List<Tariff> findAll();

    /** Добавляет и возвращает тариф уже с присвоенным id. */
    Tariff add(Tariff t);

    /** Обновляет по id; id в t обязан быть не null. */
    void update(Tariff t);

    /** Удаляет по id. */
    void delete(long id);

    /** Полная очистка хранилища. */
    void deleteAll();

    Optional<Tariff> findById(long id);
}

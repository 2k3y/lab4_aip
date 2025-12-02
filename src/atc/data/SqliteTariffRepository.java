package atc.data;

import atc.model.Tariff;
import atc.model.TariffType;
import atc.service.TariffException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Репозиторий тарифов на SQLite.
 *
 * Таблица tariffs:
 *  id       INTEGER PRIMARY KEY AUTOINCREMENT
 *  city     TEXT NOT NULL
 *  type     TEXT NOT NULL ('REGULAR' / 'PRIVILEGED')
 *  price    REAL NOT NULL (0.01..1000.00)
 *  discount REAL NOT NULL (0..100), для REGULAR всегда 0
 *
 * UNIQUE по "сырым" полям (город+тип+цена+скидка).
 * Дополнительная логика проверки дублей остаётся в TariffManager.
 */
public class SqliteTariffRepository implements TariffRepository {

    private final String url;

    static {
        // Для совместимости со старыми JDK / драйверами
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
        }
    }

    public SqliteTariffRepository(String url) {
        this.url = url;
        initSchema();
    }

    private Connection conn() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    /** Создаём таблицу, если её ещё нет. БЕЗ выражений в UNIQUE. */
    private void initSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS tariffs(
                  id       INTEGER PRIMARY KEY AUTOINCREMENT,
                  city     TEXT NOT NULL,
                  type     TEXT NOT NULL CHECK (type IN ('REGULAR','PRIVILEGED')),
                  price    REAL NOT NULL CHECK (price BETWEEN 0.01 AND 1000.00),
                  discount REAL NOT NULL DEFAULT 0 CHECK (discount BETWEEN 0 AND 100),
                  UNIQUE(city, type, price, discount)
                );
                """;
        try (Connection c = conn();
             Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new TariffException("Ошибка инициализации БД: " + e.getMessage());
        }
    }

    // ================== Реализация TariffRepository ==================

    @Override
    public List<Tariff> findAll() {
        String sql = "SELECT id, city, type, price, discount FROM tariffs " +
                "ORDER BY city, type, price, id";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Tariff> res = new ArrayList<>();
            while (rs.next()) {
                long id = rs.getLong("id");
                String city = rs.getString("city");
                TariffType type = TariffType.valueOf(rs.getString("type"));
                double price = rs.getDouble("price");
                double disc = rs.getDouble("discount");
                res.add(new Tariff(id, city, type, price, disc));
            }
            return res;
        } catch (SQLException | IllegalArgumentException e) {
            throw new TariffException("Ошибка чтения из БД: " + e.getMessage());
        }
    }

    @Override
    public Tariff add(Tariff t) {
        String sql = "INSERT INTO tariffs(city, type, price, discount) VALUES (?,?,?,?)";
        double discount = (t.getType() == TariffType.PRIVILEGED) ? t.getDiscountPercent() : 0.0;

        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, normCity(t.getCity()));
            ps.setString(2, t.getType().name());
            ps.setDouble(3, t.getPricePerMinute());
            ps.setDouble(4, discount);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                Long id = null;
                if (keys.next()) {
                    id = keys.getLong(1);
                }
                return new Tariff(id, t.getCity(), t.getType(), t.getPricePerMinute(), discount);
            }
        } catch (SQLException e) {
            throw mapSqlException(e);
        }
    }

    @Override
    public void update(Tariff t) {
        if (t.getId() == null) {
            throw new IllegalArgumentException("id тарифа не задан");
        }
        String sql = "UPDATE tariffs SET city=?, type=?, price=?, discount=? WHERE id=?";
        double discount = (t.getType() == TariffType.PRIVILEGED) ? t.getDiscountPercent() : 0.0;

        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, normCity(t.getCity()));
            ps.setString(2, t.getType().name());
            ps.setDouble(3, t.getPricePerMinute());
            ps.setDouble(4, discount);
            ps.setLong(5, t.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new TariffException("Тариф с id=" + t.getId() + " не найден");
            }
        } catch (SQLException e) {
            throw mapSqlException(e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM tariffs WHERE id=?";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TariffException("Ошибка удаления: " + e.getMessage());
        }
    }

    @Override
    public void deleteAll() {
        try (Connection c = conn();
             Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM tariffs");
        } catch (SQLException e) {
            throw new TariffException("Ошибка очистки БД: " + e.getMessage());
        }
    }

    @Override
    public Optional<Tariff> findById(long id) {
        String sql = "SELECT id, city, type, price, discount FROM tariffs WHERE id=?";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                long tid = rs.getLong("id");
                String city = rs.getString("city");
                TariffType type = TariffType.valueOf(rs.getString("type"));
                double price = rs.getDouble("price");
                double disc = rs.getDouble("discount");
                return Optional.of(new Tariff(tid, city, type, price, disc));
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new TariffException("Ошибка поиска тарифа: " + e.getMessage());
        }
    }

    // ================== helpers ==================

    private static String normCity(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ");
    }

    /** Переводим SQLException в понятное пользователю сообщение. */
    private TariffException mapSqlException(SQLException e) {
        String msg = e.getMessage();
        String lower = msg == null ? "" : msg.toLowerCase(Locale.ROOT);

        // SQLite: код 19 = SQLITE_CONSTRAINT, в том числе UNIQUE
        if (e.getErrorCode() == 19 && lower.contains("unique")) {
            return new TariffException("Такой тариф уже существует (нарушено ограничение уникальности).");
        }
        return new TariffException("Ошибка работы с БД: " + e.getMessage());
    }
}

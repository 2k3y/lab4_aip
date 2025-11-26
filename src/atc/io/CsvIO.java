// CsvIO.java
package atc.io;

import atc.model.Tariff;
import atc.model.TariffType;
import atc.service.TariffException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class CsvIO {

    /** Сохранение в CSV (UTF-8 с BOM), разделитель ';', десятичная точка. */
    public static void save(File file, List<Tariff> items) {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                throw new IOException("Не удалось создать папку: " + dir);
            }

            File tmp = File.createTempFile("atc_", ".csv", dir);
            try (PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(tmp, false), StandardCharsets.UTF_8))) {

                // BOM для Excel
                out.write('\uFEFF');
                out.println("city;type;price;discount");

                for (Tariff t : items) {
                    out.printf("%s;%s;%.4f;%.2f%n",
                            escape(t.getCity()),
                            t.getType().name(),
                            t.getPricePerMinute(),
                            t.getDiscountPercent());
                }
            }

            if (!tmp.renameTo(file)) {
                try (InputStream in = new FileInputStream(tmp);
                     OutputStream out = new FileOutputStream(file, false)) {
                    in.transferTo(out);
                } finally {
                    //noinspection ResultOfMethodCallIgnored
                    tmp.delete();
                }
            }
        } catch (IOException e) {
            throw new TariffException("Не удалось сохранить: " + e.getMessage());
        }
    }

    /**
     * Импорт «добавлением»: список НЕ очищаем; добавляем записи.
     * Обязательна корректная шапка: city;type;price;discount (без учёта регистра и лишних пробелов).
     * Точные дубликаты пропускаются. Запятая в числах допускается (конвертируется в точку).
     */
    public static ImportResult loadAdd(File file, List<Tariff> target) {
        int added = 0, skipped = 0, total = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;

            // 1) Находим первую непустую строку и проверяем, что это шапка
            String headerLine = null;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = stripBom(line);
                if (!line.trim().isEmpty()) { headerLine = line; break; }
            }
            if (headerLine == null) {
                // пустой файл — просто нечего импортировать
                return new ImportResult(0, 0, 0);
            }
            if (!isValidHeader(headerLine)) {
                throw new TariffException(
                        "Некорректная шапка CSV в строке " + lineNo +
                                ". Ожидалось: city;type;price;discount");
            }

            // 2) Читаем данные
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(";", -1);
                if (parts.length < 4) {
                    throw new TariffException("Строка " + lineNo + ": ожидалось 4 поля (city;type;price;discount)");
                }

                try {
                    String city = unescape(parts[0]).trim();
                    TariffType type = TariffType.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
                    double price = parseDouble(parts[2]); // допускаем запятую
                    double disc  = parseDouble(parts[3]);

                    // строгая валидация
                    atc.service.TariffManager.validateCity(city);
                    atc.service.TariffManager.validatePrice(price);
                    if (type == TariffType.PRIVILEGED) {
                        atc.service.TariffManager.validateDiscount(disc);
                    } else if (disc != 0.0) {
                        throw new TariffException("для обычного тарифа скидка должна быть 0");
                    }

                    Tariff t = new Tariff(city, type, price, disc);
                    total++;

                    if (exists(target, t)) {
                        skipped++;
                    } else {
                        target.add(t);
                        added++;
                    }
                } catch (TariffException | NumberFormatException e) {
                    throw new TariffException("Строка " + lineNo + ": " + e.getMessage());
                } catch (IllegalArgumentException e) { // неверный enum Type
                    throw new TariffException("Строка " + lineNo + ": неизвестный тип тарифа: " + parts[1]);
                }
            }
        } catch (IOException e) {
            throw new TariffException("Ошибка чтения: " + e.getMessage());
        }

        return new ImportResult(added, skipped, total);
    }

    // ---------- helpers ----------

    private static boolean isValidHeader(String line) {
        if (line == null) return false;
        String s = stripBom(line).trim();
        String[] p = s.split(";", -1);
        if (p.length < 4) return false;
        return p[0].trim().equalsIgnoreCase("city")
                && p[1].trim().equalsIgnoreCase("type")
                && p[2].trim().equalsIgnoreCase("price")
                && p[3].trim().equalsIgnoreCase("discount");
    }

    private static String stripBom(String s) {
        return (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') ? s.substring(1) : s;
    }

    /** Допускаем запятую (заменяем на точку), удаляем пробелы-разделители. */
    private static double parseDouble(String s) {
        String t = s == null ? "" : s.trim();
        t = t.replace("\u00A0", "").replace(" ", "");
        if (t.indexOf(',') >= 0) t = t.replace(',', '.');
        return Double.parseDouble(t);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace(";", ",");
    }

    private static String unescape(String s) { return s; }

    /** Проверка точного дубликата (город+тип+цена+скидка) с округлением. */
    private static boolean exists(List<Tariff> list, Tariff t) {
        for (Tariff x : list) {
            if (eqCity(x.getCity(), t.getCity())
                    && x.getType() == t.getType()
                    && Double.compare(round4(x.getPricePerMinute()), round4(t.getPricePerMinute())) == 0
                    && Double.compare(round2(x.getDiscountPercent()), round2(t.getDiscountPercent())) == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean eqCity(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }

    /** Результат импорта. */
    public static class ImportResult {
        private final int added, skipped, total;
        public ImportResult(int added, int skipped, int total) {
            this.added = added; this.skipped = skipped; this.total = total;
        }
        public int getAdded()   { return added; }
        public int getSkipped() { return skipped; }
        public int getTotal()   { return total; }
    }
}

package atc.io;

import atc.model.Tariff;
import atc.model.TariffType;
import atc.service.TariffException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvIO {
    public static void save(File file, List<Tariff> items) {
        try (PrintWriter out = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            out.write('\uFEFF');
            out.println("city;type;price;discount");
            for (Tariff t : items) {
                out.printf("%s;%s;%.4f;%.2f%n",
                        escape(t.getCity()),
                        t.getType().name(),
                        t.getPricePerMinute(),
                        t.getDiscountPercent());
            }
        } catch (IOException e) {
            throw new TariffException("Не удалось сохранить: " + e.getMessage());
        }
    }

    public static void load(File file, List<Tariff> target) {
        target.clear();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(";", -1);
                if (parts.length < 4) continue;
                String city = unescape(parts[0]);
                TariffType type = TariffType.valueOf(parts[1]);
                double price = parseDouble(parts[2]);
                double disc  = parseDouble(parts[3]);
                target.add(new Tariff(city, type, price, disc));
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new TariffException("Не удалось загрузить: " + e.getMessage());
        }
    }

    private static double parseDouble(String s) {
        s = s.trim().replace(',', '.');
        return Double.parseDouble(s);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace(";", ",");
    }
    private static String unescape(String s) { return s; }
}

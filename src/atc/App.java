package atc;

import atc.data.SqliteTariffRepository;
import atc.service.TariffException;
import atc.service.TariffManager;
import atc.ui.MainFrame;

import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            try {
                // ВАЖНО: один-единственный файл БД — atc.db в корне проекта
                SqliteTariffRepository repo =
                        new SqliteTariffRepository("jdbc:sqlite:atc.db");
                TariffManager manager = new TariffManager(repo);

                MainFrame f = new MainFrame(manager);
                f.setMinimumSize(new Dimension(900, 520));
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            } catch (TariffException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        ex.getMessage(),
                        "Ошибка работы с БД",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}

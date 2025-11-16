package atc;

import atc.ui.MainFrame;

import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            MainFrame f = new MainFrame();
            f.setMinimumSize(new Dimension(900, 520));
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}

package atc.ui;

import atc.io.CsvIO;
import atc.model.Tariff;
import atc.service.TariffException;
import atc.service.TariffManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;

public class MainFrame extends JFrame {
    private final TariffManager manager = new TariffManager();
    private final TariffTableModel model = new TariffTableModel(manager);
    private JTable table;

    public MainFrame() {
        super("АТС — тарифы (Swing)");
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void buildUI() {
        table = new JTable(model);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(new TableRowSorter<>(model));

        JScrollPane scroll = new JScrollPane(table);

        JButton addBtn = new JButton("Добавить");
        JButton editBtn = new JButton("Изменить");
        JButton delBtn = new JButton("Удалить");

        addBtn.addActionListener(e -> onAdd());
        editBtn.addActionListener(e -> onEdit());
        delBtn.addActionListener(e -> onDelete());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(delBtn);

        setJMenuBar(buildMenu());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buttons, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
    }

    private JMenuBar buildMenu() {
        JMenuBar mb = new JMenuBar();

        JMenu file = new JMenu("Файл");
        JMenuItem open = new JMenuItem("Открыть…");
        JMenuItem save = new JMenuItem("Сохранить как…");
        JMenuItem exit = new JMenuItem("Выход");

        open.addActionListener(e -> onOpen());
        save.addActionListener(e -> onSave());
        exit.addActionListener(e -> dispose());

        file.add(open); file.add(save); file.addSeparator(); file.add(exit);

        JMenu act = new JMenu("Действия");
        JMenuItem avg  = new JMenuItem("Средняя цена");
        JMenuItem sum  = new JMenuItem("Общая сумма цен");
        JMenuItem inc  = new JMenuItem("Изменить все цены на %");

        avg.addActionListener(e -> onAverage());
        sum.addActionListener(e -> onTotal());
        inc.addActionListener(e -> onIncreaseAll());

        act.add(avg); act.add(sum); act.add(inc);

        mb.add(file);
        mb.add(act);
        return mb;
    }

    private void onAdd() {
        TariffFormDialog dlg = new TariffFormDialog(this, manager);
        dlg.setVisible(true);
        model.fireAll();
    }

    private void onEdit() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        Tariff t = model.getAt(modelRow);

        TariffFormDialog dlg = new TariffFormDialog(this, manager, t, modelRow);
        dlg.setVisible(true);
        model.fireAll();
    }

    private void onDelete() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = table.convertRowIndexToModel(viewRow);

        int res = JOptionPane.showConfirmDialog(this, "Удалить выбранный тариф?", "Подтверждение",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res == JOptionPane.YES_OPTION) {
            manager.remove(modelRow);
            model.fireAll();
        }
    }

    private void onAverage() {
        try {
            double v = manager.averageFinalPrice();
            JOptionPane.showMessageDialog(this, String.format("Средняя цена: %.2f руб/мин", v));
        } catch (TariffException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onTotal() {
        try {
            double v = manager.totalFinalPrice();
            JOptionPane.showMessageDialog(this, String.format("Общая сумма: %.2f руб/мин", v));
        } catch (TariffException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onIncreaseAll() {
        String s = JOptionPane.showInputDialog(this, "На сколько процентов изменить цены? (например, 10 или -5)");
        if (s == null) return;
        try {
            double p = Double.parseDouble(s.trim().replace(',', '.'));
            manager.increaseAllPrices(p);
            model.fireAll();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Введите число (например 10)", "Ошибка", JOptionPane.ERROR_MESSAGE);
        } catch (TariffException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onOpen() {
        JFileChooser fc = chooser("Загрузить CSV");
        int r = fc.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                CsvIO.load(file, manager.getTariffs());
                model.fireAll();
            } catch (TariffException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onSave() {
        JFileChooser fc = chooser("Сохранить CSV");
        int r = fc.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = appendCsvIfMissing(fc.getSelectedFile());
            try {
                CsvIO.save(file, manager.getTariffs());
                JOptionPane.showMessageDialog(this, "Сохранено: " + file.getAbsolutePath());
            } catch (TariffException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static JFileChooser chooser(String title) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setFileFilter(new FileNameExtensionFilter("CSV файлы", "csv"));
        return fc;
    }

    private static File appendCsvIfMissing(File f) {
        String name = f.getName().toLowerCase();
        if (!name.endsWith(".csv")) {
            return new File(f.getParentFile(), f.getName() + ".csv");
        }
        return f;
    }
}

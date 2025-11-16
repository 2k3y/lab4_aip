package atc.ui;

import atc.model.Tariff;
import atc.model.TariffType;
import atc.service.TariffException;
import atc.service.TariffManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class TariffFormDialog extends JDialog {
    private final TariffManager manager;
    private final boolean editMode;
    private final Integer editIndex;

    private JTextField cityField;
    private JComboBox<TariffType> typeBox;
    private JSpinner priceSpin;
    private JSpinner discSpin;
    private JButton okBtn, cancelBtn;

    // Конструктор «добавить»
    public TariffFormDialog(Frame owner, TariffManager manager) {
        this(owner, manager, null, null);
    }

    // Конструктор «изменить» (перегрузка) — изменяем визуальный вид и данные
    public TariffFormDialog(Frame owner, TariffManager manager, Tariff toEdit, Integer editIndex) {
        super(owner, true);
        this.manager = manager;
        this.editMode = (toEdit != null);
        this.editIndex = editIndex;

        buildUI();
        setTitle(editMode ? "Изменить тариф" : "Добавить тариф");

        if (editMode && toEdit != null) {
            cityField.setText(toEdit.getCity());
            typeBox.setSelectedItem(toEdit.getType());
            priceSpin.setValue(toEdit.getPricePerMinute());
            discSpin.setValue(toEdit.getDiscountPercent());
            updateDiscountEnabled();
        }

        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        cityField = new JTextField(20);
        typeBox = new JComboBox<>(TariffType.values());
        priceSpin = new JSpinner(new SpinnerNumberModel(1.00, 0.01, 1_000_000.0, 0.10));
        discSpin  = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 1.0));

        typeBox.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) updateDiscountEnabled(); });

        okBtn = new JButton(editMode ? "Сохранить" : "Добавить");
        cancelBtn = new JButton("Отмена");

        okBtn.addActionListener(e -> onOk());
        cancelBtn.addActionListener(e -> dispose());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Город:"), c);
        c.gridx = 1; form.add(cityField, c);

        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Тип тарифа:"), c);
        c.gridx = 1; form.add(typeBox, c);

        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Цена, руб/мин:"), c);
        c.gridx = 1; form.add(priceSpin, c);

        c.gridx = 0; c.gridy = 3; form.add(new JLabel("Скидка, %:"), c);
        c.gridx = 1; form.add(discSpin, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(okBtn);
        buttons.add(cancelBtn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        updateDiscountEnabled();
    }

    private void updateDiscountEnabled() {
        boolean isPriv = typeBox.getSelectedItem() == TariffType.PRIVILEGED;
        discSpin.setEnabled(isPriv);
    }

    private double dbl(Object v) { return ((Number) v).doubleValue(); }

    private void onOk() {
        try {
            String city = cityField.getText();
            TariffManager.validateCity(city);

            TariffType type = (TariffType) typeBox.getSelectedItem();
            double price = dbl(priceSpin.getValue());
            TariffManager.validatePrice(price);

            double disc = dbl(discSpin.getValue());
            if (type == TariffType.PRIVILEGED) {
                TariffManager.validateDiscount(disc);
            } else {
                disc = 0.0;
            }

            Tariff t = new Tariff(city.trim(), type, price, disc);
            if (editMode && editIndex != null) {
                manager.update(editIndex, t);
            } else {
                manager.add(t);
            }
            dispose();
        } catch (TariffException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}

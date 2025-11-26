package atc.ui;

import atc.model.Tariff;
import atc.model.TariffType;
import atc.service.TariffException;
import atc.service.TariffManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TariffFormDialog extends JDialog {
    private final TariffManager manager;
    private final boolean editMode;
    private final Integer editIndex;

    private JTextField cityField;
    private JComboBox<TariffType> typeBox;
    private JSpinner priceSpin;
    private JSpinner discSpin;
    private JButton okBtn, cancelBtn;

    /** Флаг: когда true — верификаторы пропускают фокус (закрытие/Отмена). */
    private volatile boolean skipVerify = false;

    // «Добавить»
    public TariffFormDialog(Frame owner, TariffManager manager) {
        this(owner, manager, null, null);
    }

    // «Изменить» (перегрузка)
    public TariffFormDialog(Frame owner, TariffManager manager, Tariff toEdit, Integer editIndex) {
        super(owner, true);
        this.manager = manager;
        this.editMode = (toEdit != null);
        this.editIndex = editIndex;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Крестик — выходим без проверок
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { skipVerify = true; }
        });
        // Esc — выходим без проверок
        getRootPane().registerKeyboardAction(
                e -> { skipVerify = true; dispose(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

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
        typeBox   = new JComboBox<>(TariffType.values());
        priceSpin = new JSpinner(new SpinnerNumberModel(1.00, 0.01, 1_000.00, 0.10));
        discSpin  = new JSpinner(new SpinnerNumberModel(0.0,  0.0,  100.0,   1.0));
        typeBox.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) updateDiscountEnabled(); });

        okBtn = new JButton(editMode ? "Сохранить" : "Добавить");
        cancelBtn = new JButton("Выход");

        okBtn.addActionListener(e -> onOk());
        // Не запускаем валидацию при переходе фокуса на «Отмена»
        cancelBtn.setVerifyInputWhenFocusTarget(false);
        cancelBtn.addActionListener(e -> { skipVerify = true; dispose(); });

        // Enter = OK
        getRootPane().setDefaultButton(okBtn);

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
        addVerifiers();
    }

    // ------- Верификаторы ввода (строгий формат чисел) -------

    /** true, если текст НЕ является числом в формате: цифры или цифры,зз (запятая, не точка). */
    private static boolean invalidNumericText(String txt, int maxFracDigits, boolean forbidDot) {
        if (txt == null) return true;
        String t = txt.replace("\u00A0", "").replace(" ", "").trim(); // убираем пробелы/НБП
        if (t.isEmpty()) return true;
        if (forbidDot && t.indexOf('.') >= 0) return true;            // точка запрещена
        String frac = maxFracDigits <= 0 ? "" : "(,\\d{1," + maxFracDigits + "})?";
        return !t.matches("\\d+" + frac);
    }

    /** Парсит проверенный текст: запятая -> точка. */
    private static double parseStrict(String txt) {
        String t = txt.replace("\u00A0", "").replace(" ", "").trim().replace(',', '.');
        return Double.parseDouble(t);
    }

    private void addVerifiers() {
        // PRICE
        JSpinner.NumberEditor pe = (JSpinner.NumberEditor) priceSpin.getEditor();
        pe.getTextField().setInputVerifier(new InputVerifier() {
            @Override public boolean verify(JComponent c) {
                if (skipVerify) return true;
                String raw = ((JSpinner.NumberEditor) priceSpin.getEditor()).getTextField().getText();
                if (invalidNumericText(raw, 2, true)) {
                    JOptionPane.showMessageDialog(TariffFormDialog.this,
                            "Цена: только цифры и запятая (до 2 знаков). Точка запрещена.",
                            "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                try {
                    double v = parseStrict(raw);
                    TariffManager.validatePrice(v); // 0.01..1000.00
                    return true;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TariffFormDialog.this,
                            "Цена должна быть 0.01..1 000.00",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        });

        // DISCOUNT
        JSpinner.NumberEditor de = (JSpinner.NumberEditor) discSpin.getEditor();
        de.getTextField().setInputVerifier(new InputVerifier() {
            @Override public boolean verify(JComponent c) {
                if (skipVerify) return true;
                // если не льготный — поле может быть любым (обычно отключено)
                if (typeBox.getSelectedItem() != TariffType.PRIVILEGED) return true;

                String raw = ((JSpinner.NumberEditor) discSpin.getEditor()).getTextField().getText();
                if (invalidNumericText(raw, 2, true)) {
                    JOptionPane.showMessageDialog(TariffFormDialog.this,
                            "Скидка: только цифры и запятая (до 2 знаков). Точка запрещена.",
                            "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                try {
                    double v = parseStrict(raw);
                    TariffManager.validateDiscount(v); // 0..100
                    return true;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TariffFormDialog.this,
                            "Скидка должна быть в диапазоне 0..100%",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        });
    }

    // ------- Логика формы -------

    private void updateDiscountEnabled() {
        boolean isPriv = typeBox.getSelectedItem() == TariffType.PRIVILEGED;
        discSpin.setEnabled(isPriv);
    }

    private void onOk() {
        try {
            // Принудительно дергаем верификаторы (даже если фокус еще в поле)
            JFormattedTextField pTF = ((JSpinner.NumberEditor) priceSpin.getEditor()).getTextField();
            if (!pTF.getInputVerifier().verify(pTF)) return;

            JFormattedTextField dTF = ((JSpinner.NumberEditor) discSpin.getEditor()).getTextField();
            if (!dTF.getInputVerifier().verify(dTF)) return;

            String city = cityField.getText();
            TariffManager.validateCity(city);

            TariffType type = (TariffType) typeBox.getSelectedItem();

            double price = parseStrict(pTF.getText());
            TariffManager.validatePrice(price);

            double disc = 0.0;
            if (type == TariffType.PRIVILEGED) {
                disc = parseStrict(dTF.getText());
                TariffManager.validateDiscount(disc);
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

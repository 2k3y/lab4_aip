package atc.ui;

import atc.model.Tariff;
import atc.model.TariffType;
import atc.service.TariffManager;

import javax.swing.table.AbstractTableModel;

public class TariffTableModel extends AbstractTableModel {
    private final TariffManager manager;

    private final String[] cols = {
            "Город",
            "Тип",
            "Цена, руб/мин",
            "Скидка, %",
            "Итоговая цена, руб/мин"
    };

    public TariffTableModel(TariffManager manager) {
        this.manager = manager;
    }

    @Override public int getRowCount() { return manager.getTariffs().size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int col) { return cols[col]; }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0, 1 -> String.class;
            default -> Double.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Tariff t = manager.getTariffs().get(rowIndex);
        return switch (columnIndex) {
            case 0 -> t.getCity();
            case 1 -> t.getType().toString();
            case 2 -> t.getPricePerMinute();
            case 3 -> t.getType() == TariffType.PRIVILEGED ? t.getDiscountPercent() : 0.0;
            case 4 -> t.finalPrice();
            default -> null;
        };
    }

    public Tariff getAt(int modelRow) {
        return manager.getTariffs().get(modelRow);
    }

    public void fireAll() { fireTableDataChanged(); }
}

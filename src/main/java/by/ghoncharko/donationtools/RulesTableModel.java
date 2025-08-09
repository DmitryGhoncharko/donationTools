// by/ghoncharko/donationtools/RulesTableModel.java
package by.ghoncharko.donationtools;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RulesTableModel extends AbstractTableModel {
    private final String[] cols = {"Min", "Max", "Actions (comma-separated)"};
    private final List<DonationActionsProperties.Rule> data;

    public RulesTableModel(List<DonationActionsProperties.Rule> rules) {
        this.data = new ArrayList<>(rules != null ? rules : List.of());
    }

    public List<DonationActionsProperties.Rule> getRules() {
        return data;
    }

    public void addEmpty() {
        DonationActionsProperties.Rule r = new DonationActionsProperties.Rule();
        r.setMin(null);
        r.setMax(null);
        r.setActions(new ArrayList<>());
        data.add(r);
        fireTableRowsInserted(data.size()-1, data.size()-1);
    }

    public void removeAt(int idx) {
        if (idx >= 0 && idx < data.size()) {
            data.remove(idx);
            fireTableRowsDeleted(idx, idx);
        }
    }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int column) { return cols[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var r = data.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> r.getMin();
            case 1 -> r.getMax();
            case 2 -> (r.getActions() == null || r.getActions().isEmpty())
                    ? ""
                    : String.join(",", r.getActions().stream().map(Enum::name).toList());
            default -> "";
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) { return true; }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        var r = data.get(row);
        switch (col) {
            case 0 -> r.setMin(parseDecimal(aValue));
            case 1 -> r.setMax(parseDecimal(aValue));
            case 2 -> r.setActions(parseActions(String.valueOf(aValue)));
        }
        fireTableRowsUpdated(row, row);
    }

    private BigDecimal parseDecimal(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        return new BigDecimal(s);
    }

    private List<ActionType> parseActions(String s) {
        if (s == null || s.isBlank()) return new ArrayList<>();
        return Stream.of(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .map(String::toUpperCase)
                .map(ActionType::valueOf)
                .toList();
    }
}

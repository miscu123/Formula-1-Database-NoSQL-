import javax.swing.table.AbstractTableModel;
import java.sql.*;

public class ResultSetTableModel extends AbstractTableModel {
    private ResultSet rs;
    private ResultSetMetaData metaData;
    private int rowCount;

    public ResultSetTableModel(ResultSet rs) throws SQLException {
        this.rs = rs;
        this.metaData = rs.getMetaData();

        rs.last();
        rowCount = rs.getRow();
        rs.beforeFirst();
    }

    @Override
    public int getRowCount() { return rowCount; }

    @Override
    public int getColumnCount() {
        try { return metaData.getColumnCount(); }
        catch (SQLException e) { return 0; }
    }

    @Override
    public String getColumnName(int column) {
        try { return metaData.getColumnName(column + 1); }
        catch (SQLException e) { return "?"; }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            rs.absolute(rowIndex + 1);
            return rs.getObject(columnIndex + 1);
        } catch (SQLException e) {
            return null;
        }
    }
}

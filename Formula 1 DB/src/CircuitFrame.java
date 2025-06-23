import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

public class CircuitFrame extends JFrame {
    private JTable table;
    private JButton btnAdd, btnEdit, btnDelete;
    private Formula1DAO db;

    public CircuitFrame(Formula1DAO db) {
        super("Circuite");
        this.db = db;

        table = new JTable();
        JScrollPane scroll = new JScrollPane(table);

        btnAdd = new JButton("Adaugă");
        btnEdit = new JButton("Editează");
        btnDelete = new JButton("Șterge");

        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);

        this.setLayout(new BorderLayout());
        this.add(scroll, BorderLayout.CENTER);
        this.add(btnPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            CircuitDialog dialog = new CircuitDialog(this, db, null);
            dialog.setVisible(true);
            if (dialog.isSaved()) refresh();
        });

        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                Document doc = (Document) table.getModel().getValueAt(modelRow, 0); // ia din model, coloana ascunsă
                CircuitDialog dialog = new CircuitDialog(this, db, doc);
                dialog.setVisible(true);
                if (dialog.isSaved()) refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Selectează un circuit pentru editare.");
            }
        });

        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                Document doc = (Document) table.getModel().getValueAt(modelRow, 0); // ia din model, coloana ascunsă
                try {
                    MongoCollection<Document> coll = db.getCircuitCollection();
                    coll.deleteOne(Filters.eq("_id", doc.getObjectId("_id")));
                    refresh();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Eroare la ștergere: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selectează un circuit pentru ștergere.");
            }
        });

        this.setSize(600, 400);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        refresh();
    }

    private void refresh() {
        MongoCollection<Document> coll = db.getCircuitCollection();
        FindIterable<Document> docs = coll.find();

        Vector<String> columnNames = new Vector<>();
        columnNames.add("Document");  // coloana ascunsă cu Document
        columnNames.add("Nume");
        columnNames.add("Tara");
        columnNames.add("Oras");
        columnNames.add("LungimeKM");

        Vector<Vector<Object>> data = new Vector<>();
        for (Document doc : docs) {
            Vector<Object> row = new Vector<>();
            row.add(doc);  // punem întreg Documentul în coloana 0
            row.add(doc.getString("nume"));
            row.add(doc.getString("tara"));
            row.add(doc.getString("oras"));
            row.add(doc.getDouble("lungime_km"));
            data.add(row);
        }

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // nu permite editarea directă în tabel
            }
        };
        table.setModel(model);

        // Ascundem coloana Document (index 0)
        table.removeColumn(table.getColumnModel().getColumn(0));
    }
}

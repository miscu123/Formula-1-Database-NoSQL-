import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;

public class ResultFrame extends JFrame {
    private JTable table;
    private JButton btnAdd, btnEdit, btnDelete;
    private Formula1DAO db;

    public ResultFrame(Formula1DAO db) {
        super("Rezultate");
        this.db = db;
        initializeUI();
        setupEventHandlers();
        setSize(800, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        refresh();
    }

    private void initializeUI() {
        table = new JTable();
        JScrollPane scroll = new JScrollPane(table);

        btnAdd = new JButton("Adaugă");
        btnEdit = new JButton("Editează");
        btnDelete = new JButton("Șterge");

        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        btnAdd.addActionListener(e -> {
            ResultDialog dialog = new ResultDialog(this, db, null);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refresh();
            }
        });

        btnEdit.addActionListener(e -> {
            ObjectId id = getSelectedResultId();
            if (id != null) {
                ResultDialog dialog = new ResultDialog(this, db, id);
                dialog.setVisible(true);
                if (dialog.isSaved()) {
                    refresh();
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Selectați un rezultat valid pentru editare",
                        "Eroare",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        btnDelete.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                try {
                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    Object idObj = model.getValueAt(selectedRow, 0);

                    if (idObj instanceof ObjectId) {
                        int confirm = JOptionPane.showConfirmDialog(this,
                                "Sigur doriți să ștergeți acest rezultat?",
                                "Confirmare ștergere",
                                JOptionPane.YES_NO_OPTION);

                        if (confirm == JOptionPane.YES_OPTION) {
                            db.getResultsCollection().deleteOne(new Document("_id", idObj));
                            refresh();
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Eroare la ștergere: " + ex.getMessage(),
                            "Eroare",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Selectați un rezultat pentru ștergere",
                        "Atenție",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void refresh() {
        try {
            Vector<String> columns = new Vector<>();
            columns.add("_id");
            columns.add("Pilot");
            columns.add("Cursa");
            columns.add("Poziție");
            columns.add("Timp");
            columns.add("Puncte");

            Vector<Vector<Object>> data = new Vector<>();
            FindIterable<Document> results = db.getResultsCollection().find();

            for (Document doc : results) {
                try {
                    Vector<Object> row = new Vector<>();

                    // Handle _id
                    ObjectId id = doc.getObjectId("_id");
                    row.add(id);

                    // Handle pilot_id (as ObjectId or String)
                    Object pilotIdObj = doc.get("pilot_id");
                    ObjectId pilotId = (pilotIdObj instanceof String) ?
                            new ObjectId((String) pilotIdObj) : (ObjectId) pilotIdObj;
                    row.add(getPilotName(pilotId));

                    // Handle cursa_id (as ObjectId or String)
                    Object raceIdObj = doc.get("cursa_id");
                    ObjectId raceId = (raceIdObj instanceof String) ?
                            new ObjectId((String) raceIdObj) : (ObjectId) raceIdObj;
                    row.add(getRaceName(raceId));

                    // Add other fields
                    row.add(doc.getInteger("pozitie_finala", 0));
                    row.add(doc.getString("timp_final"));
                    row.add(doc.getInteger("puncte", 0));

                    data.add(row);
                } catch (Exception e) {
                    System.err.println("Error processing document: " + doc.toJson());
                    e.printStackTrace();
                }
            }

            DefaultTableModel model = new DefaultTableModel(data, columns) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return columnIndex == 0 ? ObjectId.class : Object.class;
                }
            };

            table.setModel(model);

            // Hide _id column
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setWidth(0);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Eroare la încărcarea datelor: " + e.getMessage(),
                    "Eroare",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private String getPilotName(ObjectId pilotId) {
        if (pilotId == null) return "";
        Document pilot = db.getPilotiCollection().find(new Document("_id", pilotId)).first();
        return pilot != null ? pilot.getString("nume") : "";
    }

    private String getRaceName(ObjectId raceId) {
        if (raceId == null) return "";
        Document race = db.getRacesCollection().find(new Document("_id", raceId)).first();
        return race != null ? race.getString("nume") : "";
    }

    private ObjectId getSelectedResultId() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            Object idObj = table.getModel().getValueAt(selectedRow, 0);

            if (idObj instanceof ObjectId) {
                return (ObjectId) idObj;
            } else if (idObj instanceof String) {
                try {
                    return new ObjectId((String) idObj);
                } catch (IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(this,
                            "ID-ul nu este valid: " + idObj,
                            "Eroare ID",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return null;
    }
}
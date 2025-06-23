import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

public class TeamFrame extends JFrame {
    private JTable table;
    private JButton btnAdd, btnEdit, btnDelete, btnTotalPuncte, btnNumarCurse;
    private Formula1DAO db;

    public TeamFrame(Formula1DAO db) {
        super("Echipe");
        this.db = db;
        initializeUI();
        setupEventHandlers();
        setSize(850, 480);
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
        btnTotalPuncte = new JButton("Vezi total puncte");
        btnNumarCurse = new JButton("Număr curse");

        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        btnPanel.add(btnTotalPuncte);
        btnPanel.add(btnNumarCurse);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        btnAdd.addActionListener(e -> {
            TeamDialog dialog = new TeamDialog(this, db, null);
            dialog.setVisible(true);
            if (dialog.isSaved()) refresh();
        });

        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                ObjectId id = (ObjectId) table.getModel().getValueAt(row, 0);
                TeamDialog dialog = new TeamDialog(this, db, id);
                dialog.setVisible(true);
                if (dialog.isSaved()) refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Selectați o echipă pentru editare.");
            }
        });

        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                ObjectId id = (ObjectId) table.getModel().getValueAt(row, 0);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Sigur doriți să ștergeți această echipă?",
                        "Confirmare",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    db.getDatabase().getCollection("echipe").deleteOne(new Document("_id", id));
                    refresh();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selectați o echipă pentru ștergere.");
            }
        });

        btnTotalPuncte.addActionListener(e -> showTotalPuncte());

        btnNumarCurse.addActionListener(e -> showNumarCurseEchipa());
    }

    private void refresh() {
        MongoCollection<Document> teams = db.getDatabase().getCollection("echipe");
        FindIterable<Document> docs = teams.find();

        Vector<String> columnNames = new Vector<>();
        columnNames.add("_id");
        columnNames.add("Nume");
        columnNames.add("Țara");

        Vector<Vector<Object>> data = new Vector<>();
        for (Document doc : docs) {
            Vector<Object> row = new Vector<>();
            row.add(doc.getObjectId("_id"));
            row.add(doc.getString("nume"));
            row.add(doc.getString("tara"));
            data.add(row);
        }

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(model);

        // Ascunde coloana _id
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);
    }

    private void showTotalPuncte() {
        try {
            List<Document> pipeline = Arrays.asList(
                    new Document("$addFields",
                            new Document("convertedPilotId",
                                    new Document("$toObjectId", "$pilot_id"))),
                    new Document("$lookup",
                            new Document("from", "piloti")
                                    .append("localField", "convertedPilotId")
                                    .append("foreignField", "_id")
                                    .append("as", "pilot_info")),
                    new Document("$unwind", "$pilot_info"),
                    new Document("$addFields",
                            new Document("convertedEchipaId",
                                    new Document("$toObjectId", "$pilot_info.echipa_id"))),
                    new Document("$lookup",
                            new Document("from", "echipe")
                                    .append("localField", "convertedEchipaId")
                                    .append("foreignField", "_id")
                                    .append("as", "echipa_info")),
                    new Document("$unwind", "$echipa_info"),
                    new Document("$group",
                            new Document("_id", "$echipa_info._id")
                                    .append("numeEchipa", new Document("$first", "$echipa_info.nume"))
                                    .append("totalPuncte", new Document("$sum", "$puncte"))),
                    new Document("$sort", new Document("totalPuncte", -1)),
                    new Document("$project",
                            new Document("Echipă", "$numeEchipa")
                                    .append("Puncte", "$totalPuncte")
                                    .append("_id", 0))
            );

            AggregateIterable<Document> result = db.getDatabase()
                    .getCollection("rezultate")
                    .aggregate(pipeline);

            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Echipă", "Puncte"}, 0);

            boolean hasData = false;
            for (Document doc : result) {
                model.addRow(new Object[]{
                        doc.getString("Echipă"),
                        doc.getInteger("Puncte")
                });
                hasData = true;
            }

            if (!hasData) {
                model.addRow(new Object[]{"Nu există date de punctaj", "0"});
            }

            JTable resultTable = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(resultTable);

            JOptionPane.showMessageDialog(
                    this,
                    scrollPane,
                    "Total Puncte pe Echipă",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Eroare la calcularea punctelor: " + e.getMessage(),
                    "Eroare",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void showNumarCurseEchipa() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            ObjectId echipaId = (ObjectId) table.getModel().getValueAt(row, 0);
            String echipaNume = (String) table.getModel().getValueAt(row, 1);

            int numarCurse = db.getNumarCurseEchipa(echipaId);

            JOptionPane.showMessageDialog(
                    this,
                    "Echipa \"" + echipaNume + "\" a participat la " + numarCurse + " curse.",
                    "Număr Curse",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(this, "Selectați o echipă pentru a vedea numărul de curse.");
        }
    }
}

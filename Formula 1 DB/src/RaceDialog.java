import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

public class RaceDialog extends JDialog {
    private JTextField txtName;
    private JTextField txtDate;  // Format yyyy-MM-dd
    private JComboBox<String> comboCircuit;
    private JButton btnSave, btnCancel;
    private boolean saved = false;
    private Formula1DAO db;
    private String raceId;  // MongoDB _id as String
    private List<Document> circuits; // lista circuitelor pentru combo

    public RaceDialog(Frame parent, Formula1DAO db, String raceId) {
        super(parent, true);
        this.db = db;
        this.raceId = raceId;
        setTitle(raceId == null ? "Adaugă Cursă" : "Editează Cursă");

        txtName = new JTextField(20);
        txtDate = new JTextField(10);
        comboCircuit = new JComboBox<>();

        btnSave = new JButton("Salvează");
        btnCancel = new JButton("Renunță");

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Nume cursă:"));
        panel.add(txtName);
        panel.add(new JLabel("Data (YYYY-MM-DD):"));
        panel.add(txtDate);
        panel.add(new JLabel("Circuit:"));
        panel.add(comboCircuit);
        panel.add(btnSave);
        panel.add(btnCancel);

        add(panel);

        btnSave.addActionListener(e -> doSave());
        btnCancel.addActionListener(e -> dispose());

        loadCircuits();

        if (raceId != null) loadRace();

        pack();
        setLocationRelativeTo(parent);
    }

    private void loadCircuits() {
        circuits = db.getCircuitCollection().find().into(new java.util.ArrayList<>());
        comboCircuit.removeAllItems();
        for (Document circuit : circuits) {
            comboCircuit.addItem(circuit.getString("nume"));
        }
    }

    private void loadRace() {
        Document filter = new Document("_id", db.toObjectId(raceId));
        Document race = db.getRacesCollection().find(filter).first();
        if (race != null) {
            txtName.setText(race.getString("nume"));
            Object dateObj = race.get("data");
            if (dateObj != null) txtDate.setText(dateObj.toString());

            ObjectId circuitId = race.getObjectId("circuit_id");
            if (circuitId != null) {
                // Caut indexul circuitului în lista pentru combo
                for (int i = 0; i < circuits.size(); i++) {
                    Document c = circuits.get(i);
                    if (c.getObjectId("_id").equals(circuitId)) {
                        comboCircuit.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    private void doSave() {
        String name = txtName.getText().trim();
        String dateStr = txtDate.getText().trim();

        if (name.isEmpty() || dateStr.isEmpty() || comboCircuit.getSelectedIndex() == -1) {
            JOptionPane.showMessageDialog(this, "Completați toate câmpurile!");
            return;
        }

        try {
            LocalDate.parse(dateStr);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Data este invalidă! Folosiți formatul YYYY-MM-DD.");
            return;
        }

        // Obținem _id-ul circuitului selectat
        Document selectedCircuit = circuits.get(comboCircuit.getSelectedIndex());
        ObjectId circuitId = selectedCircuit.getObjectId("_id");

        Document doc = new Document("nume", name)
                .append("data", dateStr)
                .append("circuit_id", circuitId);

        if (raceId == null) {
            db.getRacesCollection().insertOne(doc);
        } else {
            // Pentru update păstrăm _id în document
            doc.append("_id", db.toObjectId(raceId));
            Document filter = new Document("_id", db.toObjectId(raceId));
            db.getRacesCollection().replaceOne(filter, doc);
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }
}

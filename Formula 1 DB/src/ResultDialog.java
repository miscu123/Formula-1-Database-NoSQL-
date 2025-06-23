import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;

public class ResultDialog extends JDialog {
    private JComboBox<String> cbPilot, cbRace;
    private JTextField txtPosition, txtPoints, txtTime;
    private JButton btnSave, btnCancel;
    private boolean saved = false;
    private Formula1DAO db;
    private ObjectId resultId;

    private Map<String, ObjectId> pilotsMap = new HashMap<>();
    private Map<String, ObjectId> racesMap = new HashMap<>();

    public ResultDialog(Frame parent, Formula1DAO db, ObjectId resultId) {
        super(parent, true);
        this.db = db;
        this.resultId = resultId;
        setTitle(resultId == null ? "Adaugă Rezultat" : "Editează Rezultat");
        initializeUI();
        setupEventHandlers();
        loadData();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        cbPilot = new JComboBox<>();
        cbRace = new JComboBox<>();
        txtPosition = new JTextField(5);
        txtPoints = new JTextField(5);
        txtTime = new JTextField(10);

        btnSave = new JButton("Salvează");
        btnCancel = new JButton("Renunță");

        JPanel panel = new JPanel(new GridLayout(6, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(new JLabel("Pilot:"));
        panel.add(cbPilot);
        panel.add(new JLabel("Cursa:"));
        panel.add(cbRace);
        panel.add(new JLabel("Poziție finală:"));
        panel.add(txtPosition);
        panel.add(new JLabel("Timp final (ex: 1:23.456):"));
        panel.add(txtTime);
        panel.add(new JLabel("Puncte:"));
        panel.add(txtPoints);
        panel.add(btnSave);
        panel.add(btnCancel);

        add(panel);
    }

    private void setupEventHandlers() {
        btnSave.addActionListener(e -> saveResult());
        btnCancel.addActionListener(e -> dispose());
    }

    private void loadData() {
        loadPilots();
        loadRaces();
        if (resultId != null) {
            loadExistingResult();
        }
    }

    private void loadPilots() {
        try {
            MongoCollection<Document> pilots = db.getPilotiCollection();
            FindIterable<Document> docs = pilots.find().sort(new Document("nume", 1));

            for (Document doc : docs) {
                ObjectId id = doc.getObjectId("_id");
                String nume = doc.getString("nume");
                if (nume != null) {
                    pilotsMap.put(nume, id);
                    cbPilot.addItem(nume);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Eroare la încărcarea piloților: " + e.getMessage());
        }
    }

    private void loadRaces() {
        try {
            MongoCollection<Document> races = db.getRacesCollection();
            FindIterable<Document> docs = races.find().sort(new Document("data", -1));

            for (Document doc : docs) {
                ObjectId id = doc.getObjectId("_id");
                String nume = doc.getString("nume");
                if (nume != null) {
                    racesMap.put(nume, id);
                    cbRace.addItem(nume);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Eroare la încărcarea curselor: " + e.getMessage());
        }
    }

    private void loadExistingResult() {
        try {
            Document doc = db.getResultsCollection()
                    .find(new Document("_id", resultId))
                    .first();

            if (doc != null) {
                // Obține pilot_id (suportă atât String cât și ObjectId)
                Object pilotIdObj = doc.get("pilot_id");
                ObjectId pilotId = (pilotIdObj instanceof String)
                        ? new ObjectId((String) pilotIdObj)
                        : (ObjectId) pilotIdObj;

                // Obține cursa_id (suportă atât String cât și ObjectId)
                Object raceIdObj = doc.get("cursa_id");
                ObjectId raceId = (raceIdObj instanceof String)
                        ? new ObjectId((String) raceIdObj)
                        : (ObjectId) raceIdObj;

                // Completează câmpurile
                txtPosition.setText(String.valueOf(doc.getInteger("pozitie_finala", 0)));
                txtPoints.setText(String.valueOf(doc.getInteger("puncte", 0)));
                txtTime.setText(doc.getString("timp_final"));

                // Selectează elementele în combobox-uri
                selectComboBoxItem(cbPilot, pilotsMap, pilotId);
                selectComboBoxItem(cbRace, racesMap, raceId);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Eroare la încărcarea rezultatului: " + e.getMessage(),
                    "Eroare",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void selectComboBoxItem(JComboBox<String> comboBox, Map<String, ObjectId> map, ObjectId id) {
        for (Map.Entry<String, ObjectId> entry : map.entrySet()) {
            if (entry.getValue().equals(id)) {
                comboBox.setSelectedItem(entry.getKey());
                break;
            }
        }
    }

    private void saveResult() {
        try {
            if (!validateInput()) {
                return;
            }

            // Get selected IDs - handle conversion if needed
            Object pilotIdObj = pilotsMap.get(cbPilot.getSelectedItem());
            ObjectId pilotId = (pilotIdObj instanceof String) ?
                    new ObjectId((String) pilotIdObj) :
                    (ObjectId) pilotIdObj;

            Object raceIdObj = racesMap.get(cbRace.getSelectedItem());
            ObjectId raceId = (raceIdObj instanceof String) ?
                    new ObjectId((String) raceIdObj) :
                    (ObjectId) raceIdObj;

            Document doc = new Document()
                    .append("pilot_id", pilotId)
                    .append("cursa_id", raceId)
                    .append("pozitie_finala", Integer.parseInt(txtPosition.getText()))
                    .append("timp_final", txtTime.getText())
                    .append("puncte", Integer.parseInt(txtPoints.getText()));

            MongoCollection<Document> results = db.getResultsCollection();

            if (resultId == null) {
                results.insertOne(doc);
            } else {
                results.replaceOne(new Document("_id", resultId), doc);
            }

            saved = true;
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Eroare la salvarea rezultatului: " + e.getMessage(),
                    "Eroare",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateInput() {
        if (cbPilot.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Selectați un pilot!");
            return false;
        }

        if (cbRace.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Selectați o cursă!");
            return false;
        }

        try {
            Integer.parseInt(txtPosition.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Poziția trebuie să fie un număr întreg!");
            return false;
        }

        try {
            Integer.parseInt(txtPoints.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Punctele trebuie să fie un număr întreg!");
            return false;
        }

        if (txtTime.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Introduceți timpul final!");
            return false;
        }

        return true;
    }

    public boolean isSaved() {
        return saved;
    }
}
import javax.swing.*;
import java.awt.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;

public class TeamDialog extends JDialog {
    private JTextField txtNume, txtTara;
    private JButton btnSave;
    private boolean saved = false;
    private Formula1DAO db;
    private ObjectId teamId;

    public TeamDialog(JFrame parent, Formula1DAO db, ObjectId teamId) {
        super(parent, "Echipă", true);
        this.db = db;
        this.teamId = teamId;

        txtNume = new JTextField(20);
        txtTara = new JTextField(20);

        btnSave = new JButton("Salvează");

        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));
        form.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        form.add(new JLabel("Nume Echipa:"));
        form.add(txtNume);
        form.add(new JLabel("Țara:"));
        form.add(txtTara);
        form.add(new JLabel());
        form.add(btnSave);

        this.add(form);
        this.pack();
        this.setLocationRelativeTo(parent);

        btnSave.addActionListener(e -> save());

        if (teamId != null) loadTeam();
    }

    private void loadTeam() {
        MongoCollection<Document> teams = db.getEchipeCollection();
        Document doc = teams.find(new Document("_id", teamId)).first();
        if (doc != null) {
            txtNume.setText(doc.getString("nume"));
            txtTara.setText(doc.getString("tara"));
        }
    }

    private void save() {
        String nume = txtNume.getText().trim();
        String tara = txtTara.getText().trim();

        if (nume.isEmpty() || tara.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completați toate câmpurile!");
            return;
        }

        MongoCollection<Document> teams = db.getEchipeCollection();
        Document doc = new Document()
                .append("nume", nume)
                .append("tara", tara);

        if (teamId == null) {
            teams.insertOne(doc);
        } else {
            teams.replaceOne(new Document("_id", teamId), doc);
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }
}

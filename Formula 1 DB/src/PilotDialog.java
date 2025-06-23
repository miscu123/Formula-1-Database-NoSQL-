import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

public class PilotDialog extends JDialog {
    private JTextField txtNume, txtNationalitate, txtDataN, txtNumarMasina;
    private JComboBox<EchipaItem> cmbEchipa;
    private JButton btnSave, btnExistaPilot, btnTransferaPilot;
    private boolean saved = false;
    private Formula1DAO db;
    private String pilotId;  // ID-ul este acum String
    private JLabel lblTotalPuncte;

    public PilotDialog(JFrame parent, Formula1DAO db, String pilotIdStr) {
        super(parent, "Pilot", true);
        this.db = db;
        this.pilotId = pilotIdStr;  // folosim String direct

        txtNume = new JTextField(15);
        txtNationalitate = new JTextField(15);
        txtDataN = new JTextField(10);
        txtNumarMasina = new JTextField(5);
        cmbEchipa = new JComboBox<>();
        btnSave = new JButton("Salvează");
        btnExistaPilot = new JButton("Verifică Existență");
        btnTransferaPilot = new JButton("Transferă Pilot");
        lblTotalPuncte = new JLabel("0");

        loadEchipe();

        JPanel panel = new JPanel(new GridLayout(10, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Nume:"));
        panel.add(txtNume);
        panel.add(new JLabel("Naționalitate:"));
        panel.add(txtNationalitate);
        panel.add(new JLabel("Data nașterii (yyyy-MM-dd):"));
        panel.add(txtDataN);
        panel.add(new JLabel("Număr Mașină:"));
        panel.add(txtNumarMasina);
        panel.add(new JLabel("Echipa:"));
        panel.add(cmbEchipa);
        panel.add(new JLabel("Puncte Totale:"));
        panel.add(lblTotalPuncte);
        panel.add(new JLabel(""));
        panel.add(btnSave);
        panel.add(new JLabel(""));
        panel.add(btnExistaPilot);
        panel.add(new JLabel(""));
        panel.add(btnTransferaPilot);

        this.add(panel);

        if (pilotId != null) {
            loadPilot();
            refreshTotalPuncte();
        }

        btnSave.addActionListener(e -> doSave());
        btnExistaPilot.addActionListener(e -> verificaExistentaPilot());
        btnTransferaPilot.addActionListener(e -> transferaPilot());

        this.pack();
        this.setLocationRelativeTo(parent);
    }

    private void loadEchipe() {
        MongoCollection<Document> echipe = db.getEchipeCollection();
        cmbEchipa.removeAllItems();
        for (Document doc : echipe.find()) {
            ObjectId id = doc.getObjectId("_id");
            String nume = doc.getString("nume");
            cmbEchipa.addItem(new EchipaItem(id, nume));
        }
    }

    private void loadPilot() {
        MongoCollection<Document> col = db.getPilotiCollection();
        Document pilot = col.find(Filters.eq("_id", new ObjectId(pilotId))).first();
        if (pilot != null) {
            txtNume.setText(pilot.getString("nume"));
            txtNationalitate.setText(pilot.getString("nationalitate"));

            Date data = pilot.getDate("data_nasterii");
            if (data != null) {
                txtDataN.setText(new SimpleDateFormat("yyyy-MM-dd").format(data));
            }

            Object numarObj = pilot.get("numar_masina");
            if (numarObj != null) {
                txtNumarMasina.setText(numarObj.toString());
            }

            ObjectId echipaId = pilot.getObjectId("echipa_id");
            if (echipaId != null) {
                for (int i = 0; i < cmbEchipa.getItemCount(); i++) {
                    EchipaItem item = cmbEchipa.getItemAt(i);
                    if (item.id.equals(echipaId)) {
                        cmbEchipa.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    private void refreshTotalPuncte() {
        try {
            if (pilotId != null) {
                int totalPuncte = db.getTotalPunctePilot(pilotId);
                lblTotalPuncte.setText(String.valueOf(totalPuncte));
            } else {
                lblTotalPuncte.setText("0");
            }
        } catch (Exception ex) {
            System.err.println("Eroare la calcularea punctelor totale: " + ex.getMessage());
            lblTotalPuncte.setText("Error");
        }
    }

    private void verificaExistentaPilot() {
        if (pilotId == null) {
            JOptionPane.showMessageDialog(this, "Salvați mai întâi pilotul pentru a putea verifica existența.");
            return;
        }

        try {
            boolean exists = db.existaPilot(new ObjectId(pilotId));
            String message = exists ?
                    "Pilotul există în baza de date." :
                    "Pilotul NU există în baza de date.";
            JOptionPane.showMessageDialog(this, message, "Verificare Existență",
                    exists ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la verificarea existenței pilotului: " + ex.getMessage(),
                    "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void transferaPilot() {
        if (pilotId == null) {
            JOptionPane.showMessageDialog(this, "Salvați mai întâi pilotul pentru a putea face transferul.");
            return;
        }

        EchipaItem novaEchipa = (EchipaItem) cmbEchipa.getSelectedItem();
        if (novaEchipa == null) {
            JOptionPane.showMessageDialog(this, "Selectați echipa de destinație pentru transfer.");
            return;
        }

        try {
            MongoCollection<Document> colPiloti = db.getPilotiCollection();
            Document pilot = colPiloti.find(Filters.eq("_id", new ObjectId(pilotId))).first();
            ObjectId echipaVeche = pilot != null ? pilot.getObjectId("echipa_id") : null;

            String echipaVecheNume = "Necunoscut";
            if (echipaVeche != null) {
                MongoCollection<Document> colEchipe = db.getEchipeCollection();
                Document echipaDoc = colEchipe.find(Filters.eq("_id", echipaVeche)).first();
                if (echipaDoc != null) {
                    echipaVecheNume = echipaDoc.getString("nume");
                }
            }

            if (echipaVeche != null && echipaVeche.equals(novaEchipa.id)) {
                JOptionPane.showMessageDialog(this, "Pilotul este deja în această echipă.",
                        "Transfer", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Sigur doriți să transferați pilotul de la '" + echipaVecheNume +
                            "' la '" + novaEchipa.nume + "'?",
                    "Confirmare Transfer", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = db.transferaPilot(new ObjectId(pilotId), novaEchipa.id);

                if (success) {
                    JOptionPane.showMessageDialog(this, "Transferul a fost realizat cu succes!",
                            "Transfer Reușit", JOptionPane.INFORMATION_MESSAGE);
                    loadPilot();
                    refreshTotalPuncte();
                } else {
                    JOptionPane.showMessageDialog(this, "Transferul nu a putut fi realizat.",
                            "Transfer Eșuat", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la efectuarea transferului: " + ex.getMessage(),
                    "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doSave() {
        try {
            String nume = txtNume.getText().trim();
            String nationalitate = txtNationalitate.getText().trim();
            String dataStr = txtDataN.getText().trim();
            String numarMasinaStr = txtNumarMasina.getText().trim();
            EchipaItem echipa = (EchipaItem) cmbEchipa.getSelectedItem();

            if (nume.isEmpty() || nationalitate.isEmpty() || echipa == null || dataStr.isEmpty() || numarMasinaStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Completează toate câmpurile.");
                return;
            }

            MongoCollection<Document> col = db.getPilotiCollection();

            // Verificare duplicat după nume (indiferent de echipă)
            Document existing = col.find(Filters.eq("nume", nume)).first();

            if (pilotId == null && existing != null) {
                JOptionPane.showMessageDialog(this,
                        "Un pilot cu acest nume există deja!\nNu poți adăuga duplicate.",
                        "Eroare Validare", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (pilotId != null && existing != null && !existing.getObjectId("_id").toHexString().equals(pilotId)) {
                JOptionPane.showMessageDialog(this,
                        "Acest nume este deja folosit de un alt pilot!\nNu poți folosi același nume.",
                        "Eroare Validare", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Date dataNasterii = new SimpleDateFormat("yyyy-MM-dd").parse(dataStr);

            int anulNasterii = Integer.parseInt(new SimpleDateFormat("yyyy").format(dataNasterii));
            if (anulNasterii > 2010) {
                JOptionPane.showMessageDialog(this,
                        "Pilotul trebuie să fie născut cel târziu în 2010 (minim 15 ani).",
                        "Eroare Validare", JOptionPane.ERROR_MESSAGE);
                return;
            }
            else if (anulNasterii < 1980) {
                JOptionPane.showMessageDialog(this,
                        "Pilotul trebuie să fie născut cel mai devreme în 1980 (maxim 45 de ani).",
                        "Eroare Validare", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int numarMasina;
            try {
                numarMasina = Integer.parseInt(numarMasinaStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Numărul mașinii trebuie să fie un număr valid.");
                return;
            }

            Document doc = new Document("nume", nume)
                    .append("nationalitate", nationalitate)
                    .append("data_nasterii", dataNasterii)
                    .append("numar_masina", numarMasina)
                    .append("echipa_id", echipa.id);

            if (pilotId == null) {
                col.insertOne(doc);
                pilotId = doc.getObjectId("_id").toHexString();
            } else {
                col.updateOne(Filters.eq("_id", new ObjectId(pilotId)), new Document("$set", doc));
            }

            refreshTotalPuncte();
            saved = true;
            this.dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la salvare. Asigură-te că data este în format yyyy-MM-dd.");
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private static class EchipaItem {
        ObjectId id;
        String nume;

        EchipaItem(ObjectId id, String nume) {
            this.id = id;
            this.nume = nume;
        }

        public String toString() {
            return nume;
        }
    }
}

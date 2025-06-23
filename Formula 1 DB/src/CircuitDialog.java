import javax.swing.*;
import java.awt.*;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

public class CircuitDialog extends JDialog {
    private JTextField txtNume;
    private JTextField txtTara;
    private JTextField txtOras;
    private JTextField txtLungimeKM;
    private JButton btnSave, btnCancel;

    private Formula1DAO db;
    private Document circuitDoc;  // documentul existent sau null pentru creare nouă
    private boolean saved = false;

    public CircuitDialog(JFrame parent, Formula1DAO db, Document circuitDoc) {
        super(parent, circuitDoc == null ? "Adaugă Circuit" : "Editează Circuit", true);
        this.db = db;
        this.circuitDoc = circuitDoc;

        txtNume = new JTextField(20);
        txtTara = new JTextField(20);
        txtOras = new JTextField(20);
        txtLungimeKM = new JTextField(10);

        if (circuitDoc != null) {
            txtNume.setText(circuitDoc.getString("nume"));
            txtTara.setText(circuitDoc.getString("tara"));
            txtOras.setText(circuitDoc.getString("oras"));
            Double lungime = circuitDoc.getDouble("lungime_km");
            txtLungimeKM.setText(lungime != null ? lungime.toString() : "");
        }

        btnSave = new JButton("Salvează");
        btnCancel = new JButton("Renunță");

        // Panel pentru câmpurile formularului
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        formPanel.add(new JLabel("Nume:"));
        formPanel.add(txtNume);
        formPanel.add(new JLabel("Țara:"));
        formPanel.add(txtTara);
        formPanel.add(new JLabel("Oraș:"));
        formPanel.add(txtOras);
        formPanel.add(new JLabel("Lungime KM:"));
        formPanel.add(txtLungimeKM);
        formPanel.add(btnSave);
        formPanel.add(btnCancel);

        // Label de titlu adăugat vizual sub titlul ferestrei
        JLabel lblTitlu = new JLabel(circuitDoc == null ? "Completează informațiile pentru a adăuga un circuit nou:" : "Modifică datele circuitului:");
        lblTitlu.setHorizontalAlignment(JLabel.CENTER);
        lblTitlu.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        // Layout principal
        this.setLayout(new BorderLayout());
        this.add(lblTitlu, BorderLayout.NORTH);
        this.add(formPanel, BorderLayout.CENTER);

        this.pack();
        this.setLocationRelativeTo(parent);

        btnSave.addActionListener(e -> saveCircuit());
        btnCancel.addActionListener(e -> this.dispose());
    }

    private void saveCircuit() {
        String nume = txtNume.getText().trim();
        String tara = txtTara.getText().trim();
        String oras = txtOras.getText().trim();
        double lungime;

        if (nume.isEmpty() || tara.isEmpty() || oras.isEmpty() || txtLungimeKM.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completează toate câmpurile.");
            return;
        }

        try {
            lungime = Double.parseDouble(txtLungimeKM.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Lungime KM trebuie să fie un număr valid.");
            return;
        }

        MongoCollection<Document> coll = db.getCircuitCollection();

        try {
            if (circuitDoc == null) {
                // Adaugă document nou
                Document newDoc = new Document()
                        .append("nume", nume)
                        .append("tara", tara)
                        .append("oras", oras)
                        .append("lungime_km", lungime);
                coll.insertOne(newDoc);
            } else {
                // Verificăm că avem _id înainte de update
                if (!circuitDoc.containsKey("_id")) {
                    JOptionPane.showMessageDialog(this, "Documentul nu conține _id, nu se poate edita.");
                    return;
                }

                // Actualizează document existent
                coll.updateOne(Filters.eq("_id", circuitDoc.getObjectId("_id")),
                        new Document("$set", new Document("nume", nume)
                                .append("tara", tara)
                                .append("oras", oras)
                                .append("lungime_km", lungime)));
            }
            saved = true;
            this.dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la salvare: " + ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}

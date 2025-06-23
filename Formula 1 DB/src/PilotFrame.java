import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;

public class PilotFrame extends JFrame {
    private Formula1DAO db;
    private JTextField txtFilter;
    private JButton btnFilter, btnPrev, btnNext, btnAdd, btnUpdate, btnDelete;
    private JButton btnRefreshPuncte, btnVerificaExistenta, btnTransferMasiv, btnClasamentGeneral;
    private JTable tblPiloti;
    private int page = 1, pageSize = 5;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public PilotFrame(Formula1DAO db) {
        super("Administrare Piloți");
        this.db = db;

        // Inițializare componente
        txtFilter = new JTextField(15);
        btnFilter = new JButton("Filtrează");
        btnPrev = new JButton("Pagina anterioară");
        btnNext = new JButton("Pagina următoare");
        btnAdd = new JButton("Adaugă");
        btnUpdate = new JButton("Editează");
        btnDelete = new JButton("Șterge");

        // New buttons for stored functions
        btnRefreshPuncte = new JButton("Actualizează Puncte");
        btnVerificaExistenta = new JButton("Verifică Existența");
        btnTransferMasiv = new JButton("Transfer în Masă");
        btnClasamentGeneral = new JButton("Clasament General");

        tblPiloti = new JTable();
        JScrollPane scrollPane = new JScrollPane(tblPiloti);

        // Panel sus: căutare + filtre
        JPanel pnlTop = new JPanel();
        pnlTop.add(new JLabel("Caută pilot:"));
        pnlTop.add(txtFilter);
        pnlTop.add(btnFilter);

        // Panel mijloc: butoane pentru funcții stocate
        JPanel pnlMiddle = new JPanel();
        pnlMiddle.setBorder(BorderFactory.createTitledBorder("Funcții Avansate"));
        pnlMiddle.add(btnRefreshPuncte);
        pnlMiddle.add(btnVerificaExistenta);
        pnlMiddle.add(btnTransferMasiv);
        pnlMiddle.add(btnClasamentGeneral);

        // Panel jos: butoane standard
        JPanel pnlBottom = new JPanel();
        pnlBottom.add(btnPrev);
        pnlBottom.add(btnNext);
        pnlBottom.add(btnAdd);
        pnlBottom.add(btnUpdate);
        pnlBottom.add(btnDelete);

        // Aranjare fereastră
        this.setLayout(new BorderLayout());
        this.add(pnlTop, BorderLayout.NORTH);
        this.add(scrollPane, BorderLayout.CENTER);

        // Create a panel to hold both middle and bottom panels
        JPanel pnlSouth = new JPanel(new BorderLayout());
        pnlSouth.add(pnlMiddle, BorderLayout.NORTH);
        pnlSouth.add(pnlBottom, BorderLayout.SOUTH);
        this.add(pnlSouth, BorderLayout.SOUTH);

        // Funcționalitate butoane existente
        btnFilter.addActionListener(e -> { page = 1; loadData(); });
        btnPrev.addActionListener(e -> { if (page > 1) { page--; loadData(); } });
        btnNext.addActionListener(e -> { page++; loadData(); });
        btnAdd.addActionListener(e -> editPilot(null));
        btnUpdate.addActionListener(e -> editSelected());
        btnDelete.addActionListener(e -> deleteSelected());

        // Funcționalitate butoane noi pentru funcții stocate
        btnRefreshPuncte.addActionListener(e -> refreshAllPuncte());
        btnVerificaExistenta.addActionListener(e -> verificaExistentaMultipla());
        btnTransferMasiv.addActionListener(e -> transferMasiv());
        btnClasamentGeneral.addActionListener(e -> afiseazaClasamentGeneral());

        loadData();

        this.setSize(1200, 600); // Increased size to accommodate new buttons
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void loadData() {
        String filter = txtFilter.getText().trim();
        MongoCollection<Document> colPiloti = db.getPilotiCollection();
        MongoCollection<Document> colEchipe = db.getEchipeCollection();

        List<Document> pilots;
        if (filter.isEmpty()) {
            pilots = colPiloti.find()
                    .sort(Sorts.ascending("nume"))
                    .skip((page - 1) * pageSize)
                    .limit(pageSize)
                    .into(new ArrayList<>());
        } else {
            pilots = colPiloti.find(Filters.regex("nume", ".*" + filter + ".*", "i"))
                    .sort(Sorts.ascending("nume"))
                    .skip((page - 1) * pageSize)
                    .limit(pageSize)
                    .into(new ArrayList<>());
        }

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"ID", "Nume Complet", "Naționalitate", "Data Nașterii", "Număr Mașină", "Echipă", "Puncte Totale"}, 0);

        for (Document doc : pilots) {
            String id = doc.getObjectId("_id").toHexString();
            String nume = doc.getString("nume");
            String nationalitate = doc.getString("nationalitate");

            // Formatăm data nașterii din Date în String
            String dataNasterii = "";
            Object dataObj = doc.get("data_nasterii");
            if (dataObj instanceof java.util.Date) {
                dataNasterii = sdf.format((java.util.Date) dataObj);
            }

            // Număr mașină (presupunem că e stocat ca String sau Number)
            String numarMasina = "";
            Object numarObj = doc.get("numar_masina");
            if (numarObj != null) {
                numarMasina = numarObj.toString();
            }

            // Obținem numele echipei din colecția echipe, după echipa_id
            String echipaNume = "";
            ObjectId echipaId = doc.getObjectId("echipa_id");
            if (echipaId != null) {
                Document echipaDoc = colEchipe.find(Filters.eq("_id", echipaId)).first();
                if (echipaDoc != null) {
                    echipaNume = echipaDoc.getString("nume");
                }
            }

            // Calculate total points using DAO function
            int totalPuncte = 0;
            try {
                totalPuncte = db.getTotalPunctePilot(doc.getObjectId("_id").toHexString());
            } catch (Exception ex) {
                System.err.println("Eroare la calcularea punctelor pentru pilotul " + nume + ": " + ex.getMessage());
            }

            model.addRow(new Object[]{
                    id,
                    nume,
                    nationalitate,
                    dataNasterii,
                    numarMasina,
                    echipaNume,
                    totalPuncte
            });
        }

        tblPiloti.setModel(model);

        // Ascundem coloana ID (index 0)
        if (tblPiloti.getColumnModel().getColumnCount() > 0) {
            tblPiloti.getColumnModel().getColumn(0).setMinWidth(0);
            tblPiloti.getColumnModel().getColumn(0).setMaxWidth(0);
            tblPiloti.getColumnModel().getColumn(0).setWidth(0);
        }
    }

    private void refreshAllPuncte() {
        try {
            SwingUtilities.invokeLater(() -> {
                // Show progress dialog
                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                JDialog progressDialog = new JDialog(this, "Actualizare Puncte", true);
                JPanel dialogPanel = new JPanel(new BorderLayout());
                dialogPanel.add(new JLabel("Se actualizează punctele pentru toți piloții..."), BorderLayout.CENTER);
                dialogPanel.add(progressBar, BorderLayout.SOUTH);
                progressDialog.add(dialogPanel);
                progressDialog.setSize(300, 100);
                progressDialog.setLocationRelativeTo(this);

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        // Small delay to show the progress dialog
                        Thread.sleep(1000);
                        return null;
                    }

                    @Override
                    protected void done() {
                        progressDialog.dispose();
                        // Reload data to refresh points
                        loadData();
                        JOptionPane.showMessageDialog(PilotFrame.this,
                                "Punctele au fost actualizate cu succes!",
                                "Actualizare Completă",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                };

                worker.execute();
                progressDialog.setVisible(true);
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la actualizarea punctelor: " + ex.getMessage(),
                    "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void verificaExistentaMultipla() {
        String input = JOptionPane.showInputDialog(this,
                "Introduceți numele piloților separați prin virgulă\n(ex: Hamilton, Verstappen, Leclerc):",
                "Verificare Existență Piloți După Nume",
                JOptionPane.QUESTION_MESSAGE);

        if (input == null || input.trim().isEmpty()) {
            return;
        }

        String[] numePiloti = input.split(",");
        StringBuilder rezultat = new StringBuilder("Rezultate verificare existență:\n\n");

        for (String nume : numePiloti) {
            nume = nume.trim();
            if (!nume.isEmpty()) {
                try {
                    boolean exists = db.existaPilotDupaNume(nume); // căutare după nume

                    // Afișează informații suplimentare dacă există
                    String pilotLabel = "Nume: " + nume;
                    if (exists) {
                        Document pilotInfo = db.getPilotInfoDupaNume(nume);
                        if (pilotInfo != null) {
                            String prenume = pilotInfo.getString("prenume") != null ? pilotInfo.getString("prenume") : "";
                            pilotLabel = pilotInfo.getString("nume") + " " + prenume;
                        }
                    }

                    rezultat.append("• ").append(pilotLabel).append(": ")
                            .append(exists ? "EXISTĂ" : "NU EXISTĂ").append("\n");
                } catch (Exception ex) {
                    rezultat.append("• ").append(nume).append(": EROARE - ").append(ex.getMessage()).append("\n");
                }
            }
        }

        JTextArea textArea = new JTextArea(rezultat.toString());
        textArea.setEditable(false);
        textArea.setRows(10);
        textArea.setColumns(50);
        JScrollPane scrollPane = new JScrollPane(textArea);

        JOptionPane.showMessageDialog(this, scrollPane, "Rezultate Verificare", JOptionPane.INFORMATION_MESSAGE);
    }

    private void transferMasiv() {
        // Get list of teams for selection
        List<Document> echipe = db.getAllEchipe();

        if (echipe.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nu există echipe în baza de date.",
                    "Eroare", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create combo box with teams
        JComboBox<String> cmbEchipe = new JComboBox<>();
        for (Document echipa : echipe) {
            cmbEchipe.addItem(echipa.getString("nume"));
        }

        // Create dialog for team selection
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("Selectați echipa de destinație:"));
        panel.add(cmbEchipe);
        panel.add(new JLabel("Această operație va transfera toți piloții selectați."));
        panel.add(new JLabel(""));

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Transfer în Masă", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String echipaSelectata = (String) cmbEchipe.getSelectedItem();

            // Find the ObjectId of the selected team
            ObjectId echipaId = null;
            for (Document echipa : echipe) {
                if (echipa.getString("nume").equals(echipaSelectata)) {
                    echipaId = echipa.getObjectId("_id");
                    break;
                }
            }

            if (echipaId == null) {
                JOptionPane.showMessageDialog(this, "Eroare la identificarea echipei selectate.",
                        "Eroare", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get selected pilots from table
            int[] selectedRows = tblPiloti.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(this, "Selectați cel puțin un pilot pentru transfer.",
                        "Avertisment", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Confirm transfer
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Sigur doriți să transferați " + selectedRows.length + " piloți la echipa '" + echipaSelectata + "'?",
                    "Confirmare Transfer în Masă", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                int succesCount = 0;
                int errorCount = 0;
                StringBuilder errors = new StringBuilder();

                for (int row : selectedRows) {
                    try {
                        String pilotIdStr = (String) tblPiloti.getModel().getValueAt(row, 0);
                        String pilotNume = (String) tblPiloti.getModel().getValueAt(row, 1);
                        ObjectId pilotId = new ObjectId(pilotIdStr);

                        boolean success = db.transferaPilot(pilotId, echipaId);
                        if (success) {
                            succesCount++;
                        } else {
                            errorCount++;
                            errors.append("• ").append(pilotNume).append(": Transfer eșuat\n");
                        }
                    } catch (Exception ex) {
                        errorCount++;
                        String pilotNume = (String) tblPiloti.getModel().getValueAt(row, 1);
                        errors.append("• ").append(pilotNume).append(": ").append(ex.getMessage()).append("\n");
                    }
                }

                // Show results
                StringBuilder message = new StringBuilder();
                message.append("Transfer completat!\n\n");
                message.append("Transferuri reușite: ").append(succesCount).append("\n");
                message.append("Transferuri eșuate: ").append(errorCount).append("\n");

                if (errorCount > 0) {
                    message.append("\nErori:\n").append(errors.toString());
                }

                JTextArea textArea = new JTextArea(message.toString());
                textArea.setEditable(false);
                textArea.setRows(10);
                textArea.setColumns(50);
                JScrollPane scrollPane = new JScrollPane(textArea);

                JOptionPane.showMessageDialog(this, scrollPane, "Rezultat Transfer",
                        errorCount == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

                // Refresh data
                loadData();
            }
        }
    }

    private void afiseazaClasamentGeneral() {
        try {
            List<Document> clasament = db.getPuncteTotalePiloti();

            if (clasament.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nu există rezultate pentru clasament.",
                        "Informație", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Create new frame for standings
            JFrame clasamentFrame = new JFrame("Clasament General Piloți");

            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Poziție", "Pilot", "Echipă", "Puncte Totale"}, 0);

            int pozitie = 1;
            for (Document doc : clasament) {
                String nume = doc.getString("nume");
                String prenume = doc.getString("prenume");
                String numeComplet = nume + (prenume != null ? " " + prenume : "");
                String echipa = doc.getString("echipa");
                int puncte = doc.getInteger("total_puncte", 0);

                model.addRow(new Object[]{
                        pozitie++,
                        numeComplet,
                        echipa,
                        puncte
                });
            }

            JTable tblClasament = new JTable(model);
            tblClasament.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // Set column widths
            tblClasament.getColumnModel().getColumn(0).setPreferredWidth(80);  // Poziție
            tblClasament.getColumnModel().getColumn(1).setPreferredWidth(200); // Pilot
            tblClasament.getColumnModel().getColumn(2).setPreferredWidth(150); // Echipă
            tblClasament.getColumnModel().getColumn(3).setPreferredWidth(100); // Puncte

            JScrollPane scrollPane = new JScrollPane(tblClasament);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("Clasament General - Campionatul Piloților", SwingConstants.CENTER), BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);

            JButton btnClose = new JButton("Închide");
            btnClose.addActionListener(e -> clasamentFrame.dispose());
            JPanel bottomPanel = new JPanel();
            bottomPanel.add(btnClose);
            panel.add(bottomPanel, BorderLayout.SOUTH);

            clasamentFrame.add(panel);
            clasamentFrame.setSize(600, 400);
            clasamentFrame.setLocationRelativeTo(this);
            clasamentFrame.setVisible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la afișarea clasamentului: " + ex.getMessage(),
                    "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editPilot(String pilotId) {
        PilotDialog dlg = new PilotDialog(this, db, pilotId);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadData();
    }

    private void editSelected() {
        int row = tblPiloti.getSelectedRow();
        if (row != -1) {
            String pilotId = (String) tblPiloti.getModel().getValueAt(row, 0);
            editPilot(pilotId);
        } else {
            JOptionPane.showMessageDialog(this, "Selectați un pilot pentru editare.");
        }
    }

    private void deleteSelected() {
        int row = tblPiloti.getSelectedRow();
        if (row != -1) {
            int confirm = JOptionPane.showConfirmDialog(this, "Sigur doriți să ștergeți acest pilot?", "Confirmare", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String id = (String) tblPiloti.getModel().getValueAt(row, 0);
                MongoCollection<Document> col = db.getPilotiCollection();
                col.deleteOne(Filters.eq("_id", new ObjectId(id)));
                loadData();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Selectați un pilot pentru ștergere.");
        }
    }
}
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import org.bson.Document;

public class RaceFrame extends JFrame {
    private JTable table;
    private JButton btnAdd, btnEdit, btnDelete, btnDurataMedie, btnTop3;
    private Formula1DAO db;

    public RaceFrame(Formula1DAO db) {
        super("Curse");
        this.db = db;

        table = new JTable();
        JScrollPane scroll = new JScrollPane(table);

        btnAdd = new JButton("Adaugă");
        btnEdit = new JButton("Editează");
        btnDelete = new JButton("Șterge");
        btnDurataMedie = new JButton("Durata Medie Cursă");
        btnTop3 = new JButton("Top 3 Podium");

        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        btnPanel.add(btnDurataMedie);
        btnPanel.add(btnTop3);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            RaceDialog dialog = new RaceDialog(this, db, null);
            dialog.setVisible(true);
            if (dialog.isSaved()) refresh();
        });

        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                String id = table.getModel().getValueAt(modelRow, 0).toString();
                RaceDialog dialog = new RaceDialog(this, db, id);
                dialog.setVisible(true);
                if (dialog.isSaved()) refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Selectați o cursă pentru editare.");
            }
        });

        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                String id = table.getModel().getValueAt(modelRow, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(this, "Sigur doriți să ștergeți această cursă?",
                        "Confirmare ștergere", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    db.getRacesCollection().deleteOne(new org.bson.Document("_id", new org.bson.types.ObjectId(id)));
                    refresh();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selectați o cursă pentru ștergere.");
            }
        });

        btnDurataMedie.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                Document race = getRaceByRow(modelRow);
                if (race != null && race.containsKey("circuit_id")) {
                    String circuitId = race.getString("circuit_id"); // aici circuitId e String
                    Double durataMedie = db.calculeazaDurataMedieTimpFinal(circuitId);
                    if (durataMedie != null) {
                        double durataMinute = durataMedie / 60000.0;
                        JOptionPane.showMessageDialog(this,
                                String.format("Durata medie a curselor pe circuitul selectat: %.2f minute", durataMinute),
                                "Durata Medie Cursă",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Nu există date pentru durata medie pe acest circuit.");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Circuitul nu este disponibil pentru cursa selectată.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selectați o cursă pentru a vedea durata medie.");
            }
        });

        btnTop3.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                String cursaId = table.getModel().getValueAt(modelRow, 0).toString();

                List<Document> podium = db.afiseazaPodiumCursa(cursaId);
                if (podium.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Nu există rezultate pentru cursa selectată.");
                    return;
                }

                StringBuilder sb = new StringBuilder("Top 3 Pilot(i) pentru cursa selectată:\n\n");
                for (Document d : podium) {
                    int pozitie = d.getInteger("pozitie_finala", -1);
                    String nume = d.getString("pilot_nume");
                    String prenume = d.getString("pilot_prenume");
                    int puncte = d.getInteger("puncte", 0);
                    String timp = d.getString("timp_final");

                    sb.append(String.format("%d. %s %s - Puncte: %d - Timp: %s\n",
                            pozitie, nume, prenume, puncte, timp != null ? timp : "N/A"));
                }

                JOptionPane.showMessageDialog(this, sb.toString(), "Podium Cursa", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Selectați o cursă pentru a vedea Top 3.");
            }
        });

        setSize(700, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        refresh();
    }

    private void refresh() {
        List<Document> races = db.getRacesCollection()
                .find()
                .sort(new Document("data", -1))
                .into(new java.util.ArrayList<>());

        String[] cols = {"_id", "nume", "data", "circuit_id", "nume circuit"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        for (Document race : races) {
            String id = race.getObjectId("_id").toHexString();
            String nume = race.getString("nume");
            String data = race.getString("data");
            String circuitId = race.getString("circuit_id"); // acum e String
            String circuitName = "";

            if (circuitId != null) {
                Document circuitDoc = db.getCircuitCollection().find(new org.bson.Document("_id", new org.bson.types.ObjectId(circuitId))).first();
                if (circuitDoc != null) {
                    circuitName = circuitDoc.getString("nume");
                }
            }

            model.addRow(new Object[]{id, nume, data, circuitId != null ? circuitId : "", circuitName});
        }

        table.setModel(model);

        if (table.getColumnModel().getColumnCount() >= 4) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setWidth(0);

            table.getColumnModel().getColumn(3).setMinWidth(0);
            table.getColumnModel().getColumn(3).setMaxWidth(0);
            table.getColumnModel().getColumn(3).setWidth(0);
        }
    }

    private Document getRaceByRow(int modelRow) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        String idHex = (String) model.getValueAt(modelRow, 0);
        return db.getRacesCollection().find(new org.bson.Document("_id", new org.bson.types.ObjectId(idHex))).first();
    }
}

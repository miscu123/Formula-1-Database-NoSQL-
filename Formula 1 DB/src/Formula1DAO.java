import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Formula1DAO {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017/";
    private static final String DB_NAME = "Formula1";
    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public Formula1DAO() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DB_NAME);
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public MongoCollection<Document> getUtilizatoriCollection() {
        return database.getCollection("utilizatori");
    }

    public void close() {
        mongoClient.close();
    }

    public MongoCollection<Document> getEchipeCollection() {
        return database.getCollection("echipe");
    }

    public MongoCollection<Document> getPilotiCollection() {
        return database.getCollection("piloti");
    }

    public MongoCollection<Document> getCircuitCollection() {
        return database.getCollection("circuite");
    }

    public MongoCollection<Document> getRacesCollection() {
        return database.getCollection("curse");
    }

    public ObjectId toObjectId(String id) {
        return new ObjectId(id);
    }

    public MongoCollection<Document> getResultsCollection() {
        return database.getCollection("rezultate");
    }

    public int getTotalPunctePilot(String pilotId) {
        try {
            // Verifică dacă colecția "rezultate" există
            if (!database.listCollectionNames().into(new ArrayList<>()).contains("rezultate")) {
                System.out.println("Colecția 'rezultate' nu există!");
                return 0;
            }

            // Pipeline de agregare pentru a aduna punctele pilotului cu id-ul specificat
            List<Bson> pipeline = Arrays.asList(
                    Aggregates.match(Filters.eq("pilot_id", pilotId)),
                    Aggregates.group(null, Accumulators.sum("total", "$puncte"))
            );

            // Execută agregarea și extrage rezultatul
            Document result = database.getCollection("rezultate")
                    .aggregate(pipeline)
                    .first();

            // Returnează totalul punctelor sau 0 dacă nu s-a găsit nimic
            return result != null ? result.getInteger("total", 0) : 0;

        } catch (Exception e) {
            System.err.println("Eroare la calculul punctelor pentru pilotul cu ID-ul: " + pilotId);
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Calculează durata medie a curselor pentru un circuit specific
     */
    public Double calculeazaDurataMedieTimpFinal(String circuitId) {
        // 1. Găsește cursele care au circuit_id = circuitId
        List<String> curseIds = new ArrayList<>();
        MongoCollection<Document> curse = getRacesCollection();

        for (Document race : curse.find(Filters.eq("circuit_id", circuitId))) {
            curseIds.add(race.getObjectId("_id").toHexString());
        }
        if (curseIds.isEmpty()) return null;

        // 2. Găsește rezultatele care au cursa_id în curseIds
        MongoCollection<Document> rezultate = getResultsCollection();
        List<Document> rezultateLista = rezultate.find(Filters.in("cursa_id", curseIds)).into(new ArrayList<>());

        if (rezultateLista.isEmpty()) return null;

        // 3. Parcurge rezultatele, convertește "timp_final" din String ("HH:mm:ss.SSS") în milisecunde și calculează media
        long totalMillis = 0;
        int count = 0;

        for (Document rez : rezultateLista) {
            String timpFinalStr = rez.getString("timp_final");
            if (timpFinalStr == null || timpFinalStr.isEmpty()) continue;
            try {
                long millis = convertTimpFinalToMillis(timpFinalStr);
                totalMillis += millis;
                count++;
            } catch (Exception e) {
                // ignoră formatele greșite
            }
        }

        if (count == 0) return null;

        return totalMillis / (double) count;
    }

    private long convertTimpFinalToMillis(String timp) {
        // timp format "1:31:45.312" -> ore:min:sec.millis
        String[] parts = timp.split(":");
        if (parts.length != 3) throw new IllegalArgumentException("Format timp invalid");

        int ore = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        String[] secParts = parts[2].split("\\.");
        int secunde = Integer.parseInt(secParts[0]);
        int milisecunde = secParts.length > 1 ? Integer.parseInt(secParts[1]) : 0;

        return ore * 3600000L + minute * 60000L + secunde * 1000L + milisecunde;
    }


    // Parsează stringul de forma "H:mm:ss.SSS" în milisecunde
    private long parseTimeToMillis(String timeStr) throws ParseException {
        // Dacă ai ore sub 10, să recunoască și fără 0 la început
        SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss.SSS");
        Date date = sdf.parse(timeStr);
        // Returnează doar milisecundele din ora, minut, secunde
        long millis = (date.getHours() * 3600 + date.getMinutes() * 60 + date.getSeconds()) * 1000 + date.getTime() % 1000;
        return millis;
    }

    private String formatMillisToTime(long millis) {
        long hours = millis / 3600000;
        millis = millis % 3600000;
        long minutes = millis / 60000;
        millis = millis % 60000;
        long seconds = millis / 1000;
        millis = millis % 1000;

        return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    public List<Document> afiseazaPodiumCursa(String cursaId) {
        try {
            List<Bson> pipeline = Arrays.asList(
                    Aggregates.match(Filters.eq("cursa_id", cursaId)),  // filtrare după string
                    // convertim pilot_id din string în ObjectId ca să putem face lookup:
                    Aggregates.addFields(new Field<>("pilotObjectId", new Document("$toObjectId", "$pilot_id"))),
                    Aggregates.sort(Sorts.ascending("pozitie_finala")),
                    Aggregates.limit(3),
                    Aggregates.lookup("piloti", "pilotObjectId", "_id", "pilot_info"),
                    Aggregates.unwind("$pilot_info"),
                    Aggregates.project(new Document()
                            .append("pozitie_finala", 1)
                            .append("pilot_nume", "$pilot_info.nume")
                            .append("puncte", 1)
                            .append("timp_final", 1)
                    )
            );

            List<Document> podium = new ArrayList<>();
            getResultsCollection().aggregate(pipeline).into(podium);
            return podium;
        } catch (Exception e) {
            System.err.println("Eroare la afișarea podiumului pentru cursa " + cursaId);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public boolean transferaPilot(ObjectId pilotId, ObjectId novaEchipaId) {
        try {
            // Verifică dacă pilotul există
            if (!existaPilot(pilotId)) {
                System.out.println("Pilotul cu ID-ul " + pilotId + " nu există!");
                return false;
            }

            // Verifică dacă noua echipă există
            Document echipa = getEchipeCollection().find(Filters.eq("_id", novaEchipaId)).first();
            if (echipa == null) {
                System.out.println("Echipa cu ID-ul " + novaEchipaId + " nu există!");
                return false;
            }

            // Actualizează echipa pilotului
            Document updateResult = getPilotiCollection().findOneAndUpdate(
                    Filters.eq("_id", pilotId),
                    Updates.set("echipa_id", novaEchipaId)
            );

            if (updateResult != null) {
                System.out.println("Pilotul a fost transferat cu succes la echipa " + echipa.getString("nume"));
                return true;
            } else {
                System.out.println("Transferul nu a putut fi realizat!");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Eroare la transferul pilotului " + pilotId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obține numărul de curse pentru o echipă specifică
     */
    public int getNumarCurseEchipa(ObjectId echipaId) {
        List<Document> pipeline = Arrays.asList(
                // Convertim pilot_id (string) în ObjectId pentru join
                new Document("$addFields", new Document("pilotIdObj",
                        new Document("$toObjectId", "$pilot_id"))),

                // Join cu piloti
                new Document("$lookup", new Document("from", "piloti")
                        .append("localField", "pilotIdObj")
                        .append("foreignField", "_id")
                        .append("as", "pilot_info")),

                new Document("$unwind", "$pilot_info"),

                // Filtrăm după echipa_id
                new Document("$match", new Document("pilot_info.echipa_id", echipaId)),

                // Grupăm după cursa_id (ca să nu numărăm mai multe rezultate din aceeași cursă)
                new Document("$group", new Document("_id", "$cursa_id")),

                // Numărăm cursele
                new Document("$count", "numar_curse")
        );

        AggregateIterable<Document> result = database
                .getCollection("rezultate")
                .aggregate(pipeline);

        Document doc = result.first();
        return doc != null ? doc.getInteger("numar_curse", 0) : 0;
    }



    /**
     * Verifică dacă un pilot există în baza de date
     */
    public boolean existaPilot(ObjectId pilotId) {
        try {
            Document pilot = getPilotiCollection().find(Filters.eq("_id", pilotId)).first();
            return pilot != null;
        } catch (Exception e) {
            System.err.println("Eroare la verificarea existenței pilotului " + pilotId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obține punctele totale pentru toți piloții (top clasament)
     */
    public List<Document> getPuncteTotalePiloti() {
        try {
            List<Bson> pipeline = Arrays.asList(
                    // Convertim pilot_id din string în ObjectId
                    Aggregates.addFields(new Field<>("pilot_id_obj", new Document("$toObjectId", "$pilot_id"))),

                    // Grupează după pilot_id_obj
                    Aggregates.group("$pilot_id_obj", Accumulators.sum("total_puncte", "$puncte")),

                    // Redenumește _id în pilot_id pentru claritate
                    Aggregates.addFields(new Field<>("pilot_id", "$_id")),

                    // Join cu colecția "piloti"
                    Aggregates.lookup("piloti", "pilot_id", "_id", "pilot_info"),
                    Aggregates.unwind("$pilot_info"),

                    // Join cu colecția "echipe"
                    Aggregates.lookup("echipe", "pilot_info.echipa_id", "_id", "echipa_info"),
                    Aggregates.unwind("$echipa_info"),

                    // Proiectare finală
                    Aggregates.project(new Document()
                            .append("nume", "$pilot_info.nume")
                            .append("prenume", "$pilot_info.prenume")
                            .append("echipa", "$echipa_info.nume")
                            .append("total_puncte", 1)),

                    // Sortare descrescătoare
                    Aggregates.sort(Sorts.descending("total_puncte"))
            );

            List<Document> clasament = new ArrayList<>();
            getResultsCollection().aggregate(pipeline).into(clasament);
            return clasament;

        } catch (Exception e) {
            System.err.println("Eroare la obținerea clasamentului piloților");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Obține informațiile complete despre un pilot
     */
    public Document getPilotInfo(ObjectId pilotId) {
        try {
            List<Bson> pipeline = Arrays.asList(
                    Aggregates.match(Filters.eq("_id", pilotId)),
                    Aggregates.lookup("echipe", "echipa_id", "_id", "echipa_info"),
                    Aggregates.unwind("$echipa_info")
            );

            return getPilotiCollection().aggregate(pipeline).first();
        } catch (Exception e) {
            System.err.println("Eroare la obținerea informațiilor pentru pilotul " + pilotId);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obține toate echipele disponibile
     */
    public List<Document> getAllEchipe() {
        try {
            List<Document> echipe = new ArrayList<>();
            getEchipeCollection().find().into(echipe);
            return echipe;
        } catch (Exception e) {
            System.err.println("Eroare la obținerea listei de echipe");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Obține toate cursele disponibile
     */
    public List<Document> getAllCurse() {
        try {
            List<Bson> pipeline = Arrays.asList(
                    Aggregates.lookup("circuite", "circuit_id", "_id", "circuit_info"),
                    Aggregates.unwind("$circuit_info"),
                    Aggregates.project(new Document()
                            .append("_id", 1)
                            .append("nume", 1)
                            .append("data", 1)
                            .append("circuit_nume", "$circuit_info.nume")
                            .append("tara", "$circuit_info.tara"))
            );

            List<Document> curse = new ArrayList<>();
            getRacesCollection().aggregate(pipeline).into(curse);
            return curse;
        } catch (Exception e) {
            System.err.println("Eroare la obținerea listei de curse");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Obține toți piloții disponibili
     */
    public List<Document> getAllPiloti() {
        try {
            List<Bson> pipeline = Arrays.asList(
                    Aggregates.lookup("echipe", "echipa_id", "_id", "echipa_info"),
                    Aggregates.unwind("$echipa_info"),
                    Aggregates.project(new Document()
                            .append("_id", 1)
                            .append("nume", 1)
                            .append("prenume", 1)
                            .append("nationalitate", 1)
                            .append("varsta", 1)
                            .append("echipa_nume", "$echipa_info.nume"))
            );

            List<Document> piloti = new ArrayList<>();
            getPilotiCollection().aggregate(pipeline).into(piloti);
            return piloti;
        } catch (Exception e) {
            System.err.println("Eroare la obținerea listei de piloți");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public boolean existaPilotDupaNume(String nume) {
        MongoCollection<Document> col = getPilotiCollection();
        Document doc = col.find(Filters.eq("nume", nume)).first();
        return doc != null;
    }

    public Document getPilotInfoDupaNume(String nume) {
        MongoCollection<Document> col = getPilotiCollection();
        return col.find(Filters.eq("nume", nume)).first();
    }

}
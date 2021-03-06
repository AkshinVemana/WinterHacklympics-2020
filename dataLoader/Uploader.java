import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class Uploader {

    private static final boolean DEBUG = false;
    private static TreeMap<String, TreeSet<String>> routes;
    private static HashMap<String, String> airportLocations;
    private static Firestore db;

    public static void main(String[] args) throws Exception {
        upload();
    }

    // loads airport data into Firebase
    public static void upload() throws IOException {
        getRoutes();
        getLocations();
        initDB();
        assert db != null : "Database cannot be null.";

        debug("Adding all routes...");

        CollectionReference colRef = db.collection("airports");
        for (String route : routes.keySet()) {
            HashMap<String, Object> fields = new HashMap<>();
            if (airportLocations.containsKey(route)) {
                fields.put("connections", new ArrayList<>(routes.get(route)));
                fields.put("state", airportLocations.get(route));
                ApiFuture<WriteResult> result = colRef.document(route).set(fields);
                while (!result.isDone()) {
                    // hi! if you're reading this, we apologize
                    // this ensures we wait until the request is fulfilled.
                    continue;
                }
                debug("\tAdded " + route);
            }
        }

        debug("Added all valid routes.");
    }

    // Authorities and initializes our Firebase Database
    private static void initDB() throws IOException {
        InputStream serviceAccount = new FileInputStream("dataLoader/service_account_credentials.json");
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);
        db = FirestoreClient.getFirestore();
    }

    private static void getRoutes() {
        debug("Preprocessing data...");
        routes = Preprocessor.preprocess();
        debug("Preprocessed data.");
    }

    private static void getLocations() {
        debug("Getting abbreviations...");
        airportLocations = Preprocessor.getAirportLocation();
        debug("Stored abbreviations.");
    }

    private static void debug(String text) {
        if (DEBUG)
            System.out.println(text);
    }
}
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class generateCustomDBpedia {

    public static final String TYPES = "types";
    public static final String LITERALS = "literals";
    public static final String OBJECTS = "objects";

    public static void main(String[] args) {
//        generateCustomDBpediaBasedOnSize(args);
        generateCustomDBpediaBasedOnType(args);
    }

    public static void generateCustomDBpediaBasedOnType(String[] args) {
        String[] fileTypes = {TYPES, LITERALS, OBJECTS};
        Model[] typesModels = {ModelFactory.createDefaultModel(), ModelFactory.createDefaultModel(), ModelFactory.createDefaultModel()};
        Model[] objectsModels = {ModelFactory.createDefaultModel(), ModelFactory.createDefaultModel(), ModelFactory.createDefaultModel()};
        Model[] literalsModels = {ModelFactory.createDefaultModel(), ModelFactory.createDefaultModel(), ModelFactory.createDefaultModel()};
        int[] sizes = new int[args.length];
        for (int index = 0; index < sizes.length; index++) {
            sizes[index] = Integer.parseInt(args[index]);
        }
        HashMap<String, Integer> vertexTypesHistogram = new HashMap<>();
        HashMap<String, HashSet<String>> vertexMap = new HashMap<>();
        ArrayList<Map.Entry<String, Integer>> sortedVertexTypesHistogram = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            for (String fileType : fileTypes) {
                String fileName = "201" + (i+5) + fileType + ".ttl";
                System.out.println("Processing " + fileName);
                Path input = Paths.get(fileName);
                switch (fileType) {
                    case TYPES -> typesModels[i].read(input.toUri().toString());
                    case OBJECTS -> objectsModels[i].read(input.toUri().toString());
                    case LITERALS -> literalsModels[i].read(input.toUri().toString());
                }
                System.out.println("Processed " + fileName);
            }
        }

        System.out.println("Computing frequency of vertex types...");
        for (int i = 0; i < 3; i++) {
            System.out.println("Processing 201" + (i+5) + TYPES);
            StmtIterator stmtIterator = typesModels[i].listStatements();
            int counter = 0;
            while (stmtIterator.hasNext()){
                counter++;
                Statement stmt = stmtIterator.nextStatement();
                String vertexName = stmt.getSubject().asResource().getLocalName().toLowerCase();
                String vertexType = stmt.getObject().asResource().getLocalName().toLowerCase();
                if (vertexName.length() > 28) {
                    vertexName = vertexName.substring(28);
                }
                vertexTypesHistogram.merge(vertexType, 1, Integer::sum);
                if (vertexMap.containsKey(vertexName)) {
                    vertexMap.get(vertexName).add(vertexType);
                } else {
                    HashSet<String> vertexTypes = new HashSet<>();
                    vertexTypes.add(vertexType);
                    vertexMap.put(vertexName, vertexTypes);
                }
//                if (counter % 100000 == 0) System.out.println("Processed "+counter+" statements for 201" + i + TYPES);
            }
            sortedVertexTypesHistogram = new ArrayList<>(vertexTypesHistogram.entrySet());
            sortedVertexTypesHistogram.sort((o1, o2) -> o2.getValue() - o1.getValue());
        }

        for (int numOfTypes : sizes) {

            List<Map.Entry<String, Integer>> typesEntriesToConsider = sortedVertexTypesHistogram.subList(0, numOfTypes);
            HashSet<String> typesToConsider = new HashSet<>();
            for (Map.Entry<String, Integer> typeEntry : typesEntriesToConsider) {
                typesToConsider.add(typeEntry.getKey());
            }
            System.out.println("Vertex types: "+typesToConsider);

            int G = 0;
            for (String types : typesToConsider) {
                G += vertexTypesHistogram.get(types);
            }
            G = G / 3;
            System.out.println("|G| = "+G);

//            Model[] newObjectsModels = {null, null, null};
            HashSet<String> edgesSet = new HashSet<>();
            for (int i = 0; i < 3; i++) {
                System.out.println("Processing 201" + (i+5) + OBJECTS);
                StmtIterator stmtIterator = objectsModels[i].listStatements();
                Model newModel = ModelFactory.createDefaultModel();
                int counter = 0;
                while (stmtIterator.hasNext()) {
                    Statement stmt = stmtIterator.nextStatement();
                    String subjectName = stmt.getSubject().asResource().getLocalName().toLowerCase();
                    HashSet<String> subjectTypes = vertexMap.containsKey(subjectName) ? vertexMap.get(subjectName) : new HashSet<>();
                    if (subjectTypes.size() == 0) continue;
                    String predicate = stmt.getPredicate().getLocalName().toLowerCase();
                    String objectName = stmt.getObject().asResource().getLocalName().toLowerCase();
                    HashSet<String> objectTypes = vertexMap.containsKey(objectName) ? vertexMap.get(objectName) : new HashSet<>();
                    if (objectTypes.size() == 0) continue;
                    if (typesToConsider.containsAll(subjectTypes) && typesToConsider.containsAll(objectTypes)) {
                        newModel.add(stmt);
                        edgesSet.add(subjectTypes + " " + predicate + " " + objectTypes);
                        counter++;
//                        if (counter % 100000 == 0) System.out.println("Processed "+counter+" statements for 201" + i + OBJECTS);
                    }
                }
                System.out.println("Number of statements = " + counter);
                try {
                    String newFileName = "201" + (i+5) + OBJECTS + "-" + G + ".ttl";
                    newModel.write(new PrintStream(newFileName), "N3");
                    System.out.println("Wrote to " + newFileName);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Number of edge types: "+edgesSet.size());

            for (int i = 5; i < 8; i++) {
                for (String fileType : Arrays.asList(TYPES, LITERALS)) {
                    Model model = ModelFactory.createDefaultModel();
                    switch (fileType) {
                        case TYPES -> model = typesModels[i-5];
//                        case OBJECTS -> model = objectsModels[i-5];
                        case LITERALS -> model = literalsModels[i-5];
                    }
                    StmtIterator stmtIterator = model.listStatements();
                    Model newModel = ModelFactory.createDefaultModel();
                    int counter = 0;
                    System.out.println("Processing statements for 201" + i + fileType + ".ttl");
                    while (stmtIterator.hasNext()) {
                        Statement stmt = stmtIterator.nextStatement();
                        switch (fileType) {
                            case TYPES -> {
                                String vertexType = stmt.getObject().asResource().getLocalName().toLowerCase();
                                if (!typesToConsider.contains(vertexType)) continue;
                            }
//                            case OBJECTS -> {
//                                String subjectName = stmt.getSubject().asResource().getLocalName().toLowerCase();
//                                String objectName = stmt.getObject().asResource().getLocalName().toLowerCase();
//                                boolean skip = (vertexMap.containsKey(subjectName) && !typesToConsider.containsAll(vertexMap.get(subjectName)))
//                                        || (vertexMap.containsKey(objectName) && !typesToConsider.containsAll(vertexMap.get(objectName)));
//                                if (skip) continue;
//                            }
                            case LITERALS -> { // TO-DO: use the active attributes set to reduce size of literals file?
                                String subjectName = stmt.getSubject().asResource().getLocalName().toLowerCase();
                                boolean skip = vertexMap.containsKey(subjectName) && !typesToConsider.containsAll(vertexMap.get(subjectName));
                                if (skip) continue;
                            }
                        }
                        newModel.add(stmt);
                        counter++;
//                        if (counter % 100000 == 0) System.out.println("Processed "+counter+" statements for 201" + i + fileType + ".ttl");
                    }
                    System.out.println("Number of statements = " + counter);
                    try {
                        String newFileName = "201" + i + fileType + "-" + G + ".ttl";
                        newModel.write(new PrintStream(newFileName), "N3");
                        System.out.println("Wrote to " + newFileName);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void generateCustomDBpediaBasedOnSize(String[] args) {
        String[] fileTypes = {TYPES, LITERALS, OBJECTS};
        long[] sizes = new long[args.length];
        for (int index = 0; index < sizes.length; index++) {
            sizes[index] = Long.parseLong(args[index]);
        }
        for (int i = 5; i < 8; i++) {
            HashSet<String> vertexSet = new HashSet<>();
            for (String fileType : fileTypes) {
                Model model = ModelFactory.createDefaultModel();
                String fileName = "201" + i + fileType + ".ttl";
                System.out.println("Processing " + fileName);
                Path input = Paths.get(fileName);
                model.read(input.toUri().toString());
                for (long size : sizes) {
                    StmtIterator stmtIterator = model.listStatements();
                    System.out.println("Outputting size: " + size);
                    int limit = fileType.equals(TYPES) ? Math.toIntExact(size) : (Math.toIntExact(size)*3);
                    Model newModel = ModelFactory.createDefaultModel();
                    int counter = 0;
                    while (stmtIterator.hasNext() && counter <= limit ) {
                        Statement stmt = stmtIterator.nextStatement();
                        switch (fileType) {
                            case TYPES -> {
                                String vertexName = stmt.getSubject().getURI().toLowerCase();
                                if (vertexName.length() > 28) {
                                    vertexName = vertexName.substring(28);
                                }
                                vertexSet.add(vertexName);
                            }
                            case OBJECTS -> {
                                String subjectName = stmt.getSubject().getURI().toLowerCase();
                                if (subjectName.length() > 28) {
                                    subjectName = subjectName.substring(28);
                                }
                                String objectName = stmt.getObject().toString().substring(stmt.getObject().toString().lastIndexOf("/") + 1).toLowerCase();
                                if (!vertexSet.contains(subjectName) || !vertexSet.contains(objectName)) {
                                    continue;
                                }
                            }
                            case LITERALS -> {
                                String subjectName = stmt.getSubject().getURI().toLowerCase();
                                if (subjectName.length() > 28) {
                                    subjectName = subjectName.substring(28);
                                }
                                if (!vertexSet.contains(subjectName)) {
                                    continue;
                                }
                            }
                        }
                        newModel.add(stmt);
                        counter++;
                    }
                    System.out.println("Number of statements = " + counter);
                    try {
                        String newFileName = "201" + i + fileType + "-" + size + ".ttl";
                        newModel.write(new PrintStream(newFileName), "N3");
                        System.out.println("Wrote to " + newFileName);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

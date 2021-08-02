import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

public class generateCustomDBpedia {

    public static final String TYPES = "types";
    public static final String LITERALS = "literals";
    public static final String OBJECTS = "objects";

    public static void main(String[] args) {
//        String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
//        PrintStream logStream = null;
//        try {
//            logStream = new PrintStream("graph-creation-log-" + timeAndDateStamp + ".txt");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        System.setOut(logStream);
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
                StmtIterator stmtIterator = model.listStatements();
//                List<Statement> fullList = stmtIterator.toList();
//                System.out.println("Number of statements = " + fullList.size());
                for (long size : sizes) {
                    System.out.println("Outputting size: " + size);
//                    int limit = Math.min(fullList.size(), (fileType.equals("types") ? Math.toIntExact(size) : (Math.toIntExact(size)*3)));
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
//                    List<Statement> statements = fullList.subList(0, limit);
//                    System.out.println("Number of statements = " + statements.size());
//                    Model newModel = ModelFactory.createDefaultModel();
//                    newModel.add(statements);
                    try {
                        String newFileName = "201" + i + fileType + "-" + size + ".ttl";
                        newModel.write(new PrintStream(newFileName), "N3");
                        System.out.println("Wrote to " + newFileName);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    System.gc();
                }
                System.gc();
            }
        }
    }
}

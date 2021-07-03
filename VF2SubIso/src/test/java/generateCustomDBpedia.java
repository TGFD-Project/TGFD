import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class generateCustomDBpedia {
    public static void main(long[] sizes) {
        String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
        PrintStream logStream = null;
        try {
            logStream = new PrintStream("graph-creation-log-" + timeAndDateStamp + ".txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.setOut(logStream);
        String[] fileTypes = {"types", "literals", "objects"};
        for (long size : sizes) {
            for (int i = 5; i < 8; i++) {
                for (String fileType : fileTypes) {
                    Model model = ModelFactory.createDefaultModel();
                    String fileName = "201" + i + fileType + ".ttl";
                    System.out.println("Processing " + fileName);
                    Path input = Paths.get(fileName);
                    model.read(input.toUri().toString());
                    StmtIterator stmtIterator = model.listStatements();
                    List<Statement> statements;
//					if (fileType.equals(fileTypes[0])) {
                    statements = stmtIterator.toList().subList(0, Math.toIntExact(size));
//					} else {
//						statements = stmtIterator.toList().subList(0, Math.toIntExact(size*2));
//					}
                    Model newModel = ModelFactory.createDefaultModel();
                    newModel.add(statements);
                    try {
                        String newFileName = "201" + i + fileType + "-" + size + ".ttl";
                        newModel.write(new PrintStream(newFileName), "N3");
                        System.out.println("Wrote to " + newFileName);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    System.gc();
                }
            }
        }
    }
}

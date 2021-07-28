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
    public static void main(String[] args) {
//        String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
//        PrintStream logStream = null;
//        try {
//            logStream = new PrintStream("graph-creation-log-" + timeAndDateStamp + ".txt");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        System.setOut(logStream);
        String[] fileTypes = {"types", "literals", "objects"};
        long[] sizes = new long[args.length];
        for (int index = 0; index < sizes.length; index++) {
            sizes[index] = Long.parseLong(args[index]);
        }
        for (int i = 5; i < 8; i++) {
            for (String fileType : fileTypes) {
                Model model = ModelFactory.createDefaultModel();
                String fileName = "201" + i + fileType + ".ttl";
                System.out.println("Processing " + fileName);
                Path input = Paths.get(fileName);
                model.read(input.toUri().toString());
                StmtIterator stmtIterator = model.listStatements();
                List<Statement> fullList = stmtIterator.toList();
                System.out.println("Number of statements = " + fullList.size());
                for (long size : sizes) {
                    System.out.println("Outputting size: " + size);
                    int limit = Math.min(fullList.size(), (fileType.equals("types") ? Math.toIntExact(size) : (Math.toIntExact(size)*3)));
                    List<Statement> statements = fullList.subList(0, limit);
                    System.out.println("Number of statements = " + statements.size());
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
                System.gc();
            }
        }
    }
}

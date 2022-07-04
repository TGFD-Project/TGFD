

import BatchViolation.NaiveBatchTED;
import BatchViolation.OptBatchTED;
import ICs.TGFD;
import Loader.TGFDGenerator;
import VF2Runner.VF2SubgraphIsomorphism;
import Violations.Violation;
import ChangeExploration.Change;
import ChangeExploration.ChangeLoader;
import Loader.IMDBLoader;
import Infra.*;
import org.jgrapht.GraphMapping;
import Util.Config;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class testIMDBBatch
{
    public static void main(String []args) throws FileNotFoundException {

        long wallClockStart=System.currentTimeMillis();

        Config.parse(args[0]);

        System.out.println("Test IMDB subgraph isomorphism");

        System.out.println(Arrays.toString(Config.getFirstDataFilePath().toArray()));
        System.out.println(Config.getDiffFilesPath().keySet() + " *** " + Config.getDiffFilesPath().values());

        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        List<TGFD> allTGFDs=generator.getTGFDs();

        //Create the match collection for all the TGFDs in the list
        HashMap<String, MatchCollection> matchCollectionHashMap=new HashMap<>();
        for (TGFD tgfd:allTGFDs) {
            matchCollectionHashMap.put(tgfd.getName(),new MatchCollection(tgfd.getPattern(),tgfd.getDependency(),tgfd.getDelta().getGranularity()));
        }

        //Load the first timestamp
        System.out.println("===========Snapshot 1 (" + Config.getTimestamps().get(1) + ")===========");

        long startTime=System.currentTimeMillis();
        LocalDate currentSnapshotDate= Config.getTimestamps().get(1);
        // load first snapshot of the dbpedia graph
        //TODO: Fix this error, no null
        IMDBLoader imdb = new IMDBLoader(allTGFDs, Config.getFirstDataFilePath());
        printWithTime("Load graph 1 (" + Config.getTimestamps().get(1) + ")", System.currentTimeMillis()-startTime);

        // Finding the matches of the first snapshot for each TGFD
        for (TGFD tgfd:allTGFDs) {
            VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
            System.out.println("\n###########"+tgfd.getName()+"###########");
            Iterator<GraphMapping<Vertex, RelationshipEdge>> results= VF2.execute(imdb.getGraph(), tgfd.getPattern(),false);

            //Retrieving and storing the matches of each timestamp.
            System.out.println("Retrieving the matches");
            startTime=System.currentTimeMillis();
            matchCollectionHashMap.get(tgfd.getName()).addMatches(currentSnapshotDate,results);
            printWithTime("Match retrieval", System.currentTimeMillis()-startTime);
        }

        //Load the change files
        Object[] ids= Config.getDiffFilesPath().keySet().toArray();
        Arrays.sort(ids);
        for (int i=0;i<ids.length;i++) {
            System.out.println("-----------Snapshot (" + ids[i] + ")-----------");

            startTime = System.currentTimeMillis();
            currentSnapshotDate = Config.getTimestamps().get((int) ids[i]);
            ChangeLoader changeLoader = new ChangeLoader(Config.getDiffFilesPath().get(ids[i]));
            List <Change> changes = changeLoader.getAllChanges();

            //update the dbpedia graph with the changes.
            imdb.updateGraphWithChanges(changes);
            printWithTime("Load changes (" + ids[i] + ")", System.currentTimeMillis() - startTime);
            System.out.println("Total number of changes: " + changes.size());

            for (TGFD tgfd:allTGFDs) {
                VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
                System.out.println("\n###########"+tgfd.getName()+"###########");
                Iterator<GraphMapping<Vertex, RelationshipEdge>> results= VF2.execute(imdb.getGraph(), tgfd.getPattern(),false);

                //Retrieving and storing the matches of each timestamp.
                System.out.println("Retrieving the matches");
                startTime=System.currentTimeMillis();
                matchCollectionHashMap.get(tgfd.getName()).addMatches(currentSnapshotDate,results);
                printWithTime("Match retrieval", System.currentTimeMillis()-startTime);
            }

        }

        for (TGFD tgfd:allTGFDs) {
            // Now, we need to find all the violations
            //First, we run the Naive Batch TED
            System.out.println("==========="+tgfd.getName()+"===========");
            System.out.println("Running the naive TED");
            startTime=System.currentTimeMillis();

            NaiveBatchTED naive=new NaiveBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
            Set<Violation> allViolationsNaiveBatchTED=naive.findViolations();
            System.out.println("Number of violations: " + allViolationsNaiveBatchTED.size());
            printWithTime("Naive Batch TED", System.currentTimeMillis()-startTime);
            if(Config.saveViolations)
                saveViolations("naive",allViolationsNaiveBatchTED,tgfd);

            // Next, we need to find all the violations using the optimize method
            System.out.println("Running the optimized TED");
            startTime=System.currentTimeMillis();
            OptBatchTED optimize=new OptBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
            Set<Violation> allViolationsOptBatchTED=optimize.findViolations();
            System.out.println("Number of violations (Optimized method): " + allViolationsOptBatchTED.size());
            printWithTime("Optimized Batch TED", System.currentTimeMillis()-startTime);

            if(Config.saveViolations)
                saveViolations("optimized",allViolationsOptBatchTED,tgfd);
        }

        printWithTime("Total wall clock time: ", System.currentTimeMillis()-wallClockStart);
    }

    private static void printWithTime(String message, long runTimeInMS)
    {
        System.out.println(message + " time: " + runTimeInMS + "(ms) ** " +
                TimeUnit.MILLISECONDS.toSeconds(runTimeInMS) + "(sec) ** " +
                TimeUnit.MILLISECONDS.toMinutes(runTimeInMS) +  "(min)");
    }

    private static void saveViolations(String path, Set<Violation> violations, TGFD tgfd)
    {
        try {
            FileWriter file = new FileWriter(path +"_" + tgfd.getName() + ".txt");
            file.write("***************TGFD***************\n");
            file.write(tgfd.toString());
            file.write("\n===============Violations===============\n");
            for (Violation vio:violations) {
                file.write(vio.toString() +
                        "\n---------------------------------------------------\n");
            }
            file.close();
            System.out.println("Successfully wrote to the file: " + path);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
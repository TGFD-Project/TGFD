import BatchViolation.NaiveBatchTED;
import BatchViolation.OptBatchTED;
import TGFDLoader.TGFDGenerator;
import VF2Runner.VF2SubgraphIsomorphism;
import changeExploration.Change;
import graphLoader.ChangeLoader;
import graphLoader.IMDBLoader;
import infra.*;
import org.jgrapht.GraphMapping;
import util.myConsole;
import util.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class testIMDBBatch
{
    /**
     * Arguments: -p <patternFile> [-t<snapshotId> <typeFile>] [-d<snapshotId> <dataFile>]
     *
     * Example:
     *   TestIMDB \
     *     -p "D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\pattern1.txt" \
     *     -d1 "imdb\\2000.nt" \
     *     -d2 "imdb\\2001.nt" \
     */
    public static void main(String []args) throws FileNotFoundException {

        long wallClockStart=System.currentTimeMillis();

        var snapshots = new ArrayList<String>();
        String patternPath = "";
        HashMap<Integer,LocalDate> timestamps=new HashMap<>();
        HashMap<Integer, String> changeFiles=new HashMap<>();

        System.out.println("Test IMDB subgraph isomorphism");

        Scanner scanner = new Scanner(new File(args[0]));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String []conf=line.split(" ");
            if(conf.length!=2)
                continue;
            if (conf[0].toLowerCase().startsWith("-d"))
            {
                var snapshotId = Integer.parseInt(conf[0].substring(2));
                snapshots.add(conf[1]);
            }
            else if (conf[0].toLowerCase().startsWith("-p"))
            {
                patternPath = conf[1];
            }
            else if (conf[0].toLowerCase().startsWith("-s"))
            {
                var snapshotId = Integer.parseInt(conf[0].substring(2));
                timestamps.put(snapshotId,LocalDate.parse(conf[1]));
            }
            else if (conf[0].toLowerCase().startsWith("-c"))
            {
                var snapshotId = Integer.parseInt(conf[0].substring(2));
                if(snapshotId!=1)
                    changeFiles.put(snapshotId, conf[1]);
            }
            else if(conf[0].toLowerCase().startsWith("-optgraphload"))
            {
                properties.myProperties.optimizedLoadingBasedOnTGFD=Boolean.parseBoolean(conf[1]);
            }
        }
        // TODO: check that typesPaths.keySet == dataPaths.keySet [2021-02-14]

        //System.out.println(Arrays.toString(firstTypesPath.toArray()) + " *** " + Arrays.toString(firstDataPath.toArray()));
        System.out.println(changeFiles.keySet() + " *** " + changeFiles.values());

        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator(patternPath);
        List<TGFD> allTGFDs=generator.getTGFDs();

        //Create the match collection for all the TGFDs in the list
        HashMap<String, MatchCollection> matchCollectionHashMap=new HashMap<>();
        for (TGFD tgfd:allTGFDs) {
            matchCollectionHashMap.put(tgfd.getName(),new MatchCollection(tgfd.getPattern(),tgfd.getDependency(),tgfd.getDelta().getGranularity()));
        }

        //Load the first timestamp
        myConsole.print("-----------Snapshot (1)-----------");

        long startTime=System.currentTimeMillis();
        LocalDate currentSnapshotDate=timestamps.get(1);
        // load first snapshot of the dbpedia graph
        IMDBLoader dbpedia = new IMDBLoader(allTGFDs,snapshots[0]);
        myConsole.print("Load graph (1)", System.currentTimeMillis()-startTime);

        // Finding the matches of the first snapshot for each TGFD
        for (TGFD tgfd:allTGFDs) {
            VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
            myConsole.print("\n###########"+tgfd.getName()+"###########");
            Iterator<GraphMapping<Vertex, RelationshipEdge>> results= VF2.execute(dbpedia.getGraph(), tgfd.getPattern(),false);

            //Retrieving and storing the matches of each timestamp.
            myConsole.print("Retrieving the matches");
            startTime=System.currentTimeMillis();
            matchCollectionHashMap.get(tgfd.getName()).addMatches(currentSnapshotDate,results);
            myConsole.print("Match retrieval", System.currentTimeMillis()-startTime);
        }

        //Load the change files
        Object[] ids=changeFiles.keySet().toArray();
        Arrays.sort(ids);
        for (int i=0;i<ids.length;i++) {
            myConsole.print("-----------Snapshot (" + ids[i] + ")-----------");

            startTime = System.currentTimeMillis();
            currentSnapshotDate = timestamps.get((int) ids[i]);
            ChangeLoader changeLoader = new ChangeLoader(changeFiles.get(ids[i]));
            List <Change> changes = changeLoader.getAllChanges();

            //update the dbpedia graph with the changes.
            dbpedia.updateGraphWithChanges(changes);
            myConsole.print("Load changes (" + ids[i] + ")", System.currentTimeMillis() - startTime);
            myConsole.print("Total number of changes: " + changes.size());

            for (TGFD tgfd:allTGFDs) {
                VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
                myConsole.print("\n###########"+tgfd.getName()+"###########");
                Iterator<GraphMapping<Vertex, RelationshipEdge>> results= VF2.execute(dbpedia.getGraph(), tgfd.getPattern(),false);

                //Retrieving and storing the matches of each timestamp.
                myConsole.print("Retrieving the matches");
                startTime=System.currentTimeMillis();
                matchCollectionHashMap.get(tgfd.getName()).addMatches(currentSnapshotDate,results);
                myConsole.print("Match retrieval", System.currentTimeMillis()-startTime);
            }

        }

        for (TGFD tgfd:allTGFDs) {
            // Now, we need to find all the violations
            //First, we run the Naive Batch TED
            myConsole.print("==========="+tgfd.getName()+"===========");
            myConsole.print("Running the naive TED");
            startTime=System.currentTimeMillis();

            NaiveBatchTED naive=new NaiveBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
            Set<Violation> allViolationsNaiveBatchTED=naive.findViolations();
            System.out.println("Number of violations: " + allViolationsNaiveBatchTED.size());
            myConsole.print("Naive Batch TED", System.currentTimeMillis()-startTime);
            if(properties.myProperties.saveViolations)
                saveViolations("naive",allViolationsNaiveBatchTED,tgfd);

            // Next, we need to find all the violations using the optimize method
            myConsole.print("Running the optimized TED");
            startTime=System.currentTimeMillis();
            OptBatchTED optimize=new OptBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
            Set<Violation> allViolationsOptBatchTED=optimize.findViolations();
            System.out.println("Number of violations (Optimized method): " + allViolationsOptBatchTED.size());
            myConsole.print("Optimized Batch TED", System.currentTimeMillis()-startTime);

            if(properties.myProperties.saveViolations)
                saveViolations("optimized",allViolationsOptBatchTED,tgfd);
        }

        myConsole.print("Total wall clock time: ", System.currentTimeMillis()-wallClockStart);
        myConsole.saveLogs("run_"+ LocalDateTime.now().toString() + ".txt");
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
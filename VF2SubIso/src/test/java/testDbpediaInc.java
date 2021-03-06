import BatchViolation.NaiveBatchTED;
import BatchViolation.OptBatchTED;
import IncrementalRunner.IncUpdates;
import IncrementalRunner.IncrementalChange;
import TGFDLoader.TGFDGenerator;
import VF2Runner.VF2SubgraphIsomorphism;
import changeExploration.Change;
import graphLoader.ChangeLoader;
import graphLoader.DBPediaLoader;
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

public class testDbpediaInc
{
    // TODO: input a JSON file for arguments [2021-02-14]
    // {
    //   "patternFile": "D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\pattern1.txt",
    //   "snapshots: [
    //     {
    //       "date": "2017",
    //       "literalFile": "2017-literals-0.txt",
    //       "objectFiles": [
    //         "2017-mappings-0.ttl",
    //         "2017-mappings-1.ttl"
    //       ],
    //       typefiles: [
    //         "2017-types-0.ttl",
    //         "2017-types-1.ttl"
    //       ],
    //     }
    //   ]
    // }
    /**
     * Arguments: -p <patternFile> [-t<snapshotId> <typeFile>] [-d<snapshotId> <dataFile>]
     *
     * Example:
     *   TestDBPedia \
     *     -p "D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\pattern1.txt" \
     *     -t1 "F:\\MorteZa\\Datasets\\Statistical\\2016\\types.ttl" \
     *     -t1 "F:\\MorteZa\\Datasets\\Statistical\\2016\\types2.ttl" \
     *     -d1 "F:\\MorteZa\\Datasets\\Statistical\\2016\\mappingbased_objects_en.ttl" \
     *     -d1 "F:\\MorteZa\\Datasets\\Statistical\\2016\\mappingbased_objects_en2.ttl" \
     *     -t2 "F:\\MorteZa\\Datasets\\Statistical\\2017\\types.ttl" \
     *     -t2 "F:\\MorteZa\\Datasets\\Statistical\\2017\\types2.ttl" \
     *     -d2 "F:\\MorteZa\\Datasets\\Statistical\\2017\\mappingbased_objects_en.ttl" \
     *     -d2 "F:\\MorteZa\\Datasets\\Statistical\\2017\\mappingbased_objects_en2.ttl"
     */
    public static void main(String []args) throws FileNotFoundException {
        //Expected arguments:
        // arges[0]: Type file,             sample ->  "F:\\MorteZa\\Datasets\\Statistical\\2016\\types.ttl"
        // arges[1]: Object mapping file,   sample ->  "F:\\MorteZa\\Datasets\\Statistical\\2016\\mappingbased_objects_en.ttl"
        // arges[2]: Literal mapping file,  sample ->  "F:\\MorteZa\\Datasets\\Statistical\\2016\\literals.ttl"
        // arges[3]: Graph pattern file,    sample ->  "D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\pattern1.txt"

        // TODO: input literals [2021-02-14]

        long wallClockStart=System.currentTimeMillis();

        ArrayList<String> firstDataPath=new ArrayList<>();
        ArrayList<String> firstTypesPath=new ArrayList<>();
        HashMap<Integer, String> changeFiles=new HashMap<>();

        String patternPath = "";
        HashMap<Integer,LocalDate> timestamps=new HashMap<>();

        // This will force the dbpediaLoader to only load entities of certain types in the TGFD
        // Set to be false if want to load the whole graph


        System.out.println("Test DBPedia subgraph isomorphism");

        Scanner scanner = new Scanner(new File(args[0]));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String []conf=line.split(" ");
            if(conf.length!=2)
                continue;
            if (conf[0].toLowerCase().startsWith("-t"))
            {
                var snapshotId = Integer.parseInt(conf[0].substring(2));
                if(snapshotId==1)
                    firstTypesPath.add(conf[1]);
            }
            else if (conf[0].toLowerCase().startsWith("-d"))
            {
                var snapshotId = Integer.parseInt(conf[0].substring(2));
                if(snapshotId==1)
                    firstDataPath.add(conf[1]);
            }
            else if (conf[0].toLowerCase().startsWith("-c"))
            {
                var snapshotId = Integer.parseInt(conf[0].substring(2));
                if(snapshotId!=1)
                    changeFiles.put(snapshotId, conf[1]);
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
            else if(conf[0].toLowerCase().startsWith("-optgraphload"))
            {
                properties.myProperties.optimizedLoadingBasedOnTGFD=Boolean.parseBoolean(conf[1]);
            }
        }
        // TODO: check that typesPaths.keySet == dataPaths.keySet [2021-02-14]

        // Test whether we loaded all the paths correctly

        System.out.println(Arrays.toString(firstTypesPath.toArray()) + " *** " + Arrays.toString(firstDataPath.toArray()));

        System.out.println(changeFiles.keySet() + " *** " + changeFiles.values());

        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator(patternPath);
        List<TGFD> allTGFDs=generator.getTGFDs();

        TGFD firstTGFD=allTGFDs.get(0);

        //Create the match collection for all the TGFDs in the list
        MatchCollection matchCollection=new MatchCollection(
                firstTGFD.getPattern(),firstTGFD.getDependency(),firstTGFD.getDelta().getGranularity());

        //Load the first timestamp

        System.out.println("Diameter: " + firstTGFD.getPattern().getDiameter());
        System.out.println("===========Snapshot (1)===========");

        long startTime=System.currentTimeMillis();

        LocalDate currentSnapshotDate=timestamps.get(1);

        DBPediaLoader dbpedia = new DBPediaLoader(allTGFDs,firstTypesPath,firstDataPath);

        myConsole.print("Load graph (1)", System.currentTimeMillis()-startTime);

        // Now, we need to find the matches for each snapshot.
        // Finding the matches...

        VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
        System.out.println("\n########## Graph pattern ##########");
        System.out.println(firstTGFD.getPattern().toString());
        Iterator<GraphMapping<Vertex, RelationshipEdge>> results= VF2.execute(dbpedia.getGraph(), firstTGFD.getPattern(),false);


        //Retrieving and storing the matches of each timestamp.
        System.out.println("Retrieving the matches");

        startTime=System.currentTimeMillis();

        matchCollection.addMatches(currentSnapshotDate,results);

        myConsole.print("Match retrieval", System.currentTimeMillis()-startTime);

        //Load the change files

        Object[] ids=changeFiles.keySet().toArray();
        Arrays.sort(ids);
        for (int i=0;i<ids.length;i++)
        {
            System.out.println("===========Snapshot (" + ids[i] +")===========");

            startTime=System.currentTimeMillis();

            currentSnapshotDate=timestamps.get((int)ids[i]);

            ChangeLoader changeLoader=new ChangeLoader(changeFiles.get(ids[i]));

            List<Change> allChanges=changeLoader.getAllChanges();

            myConsole.print("Load changes ("+ids[i] + ")", System.currentTimeMillis()-startTime);
            myConsole.print("Total number of changes: " + allChanges.size());

            // Now, we need to find the matches for each snapshot.
            // Finding the matches...

            startTime=System.currentTimeMillis();

            VF2 = new VF2SubgraphIsomorphism();
            System.out.println("\n########## Graph pattern ##########");
            System.out.println(firstTGFD.getPattern().toString());

            System.out.println("Updating the graph");

            IncUpdates incUpdatesOnDBpedia=new IncUpdates(dbpedia.getGraph(),firstTGFD.getPattern());

            incUpdatesOnDBpedia.AddNewVertices(allChanges);

            Set<String> newMatchesSignatures=new HashSet <>();
            Set<String> removedMatchesSignatures=new HashSet <>();
            for (Change change:allChanges) {

                IncrementalChange incChangesInMatches=incUpdatesOnDBpedia.updateGraph(change);

                if(incChangesInMatches==null)
                    continue;
                newMatchesSignatures.addAll(incChangesInMatches.getNewMatches().keySet());
                removedMatchesSignatures.addAll(incChangesInMatches.getRemovedMatchesSignatures());
                matchCollection.addMatches(currentSnapshotDate,incChangesInMatches.getNewMatches());
            }
            matchCollection.addTimestamp(currentSnapshotDate,newMatchesSignatures,removedMatchesSignatures);
            myConsole.print("Update and retrieve matches ", System.currentTimeMillis()-startTime);
            myConsole.print("#new matches: " + newMatchesSignatures.size()  + " - #removed matches: " + removedMatchesSignatures.size());
        }

        // Now, we need to find all the violations
        //First, we run the Naive Batch TED
        System.out.println("======================================================");
        System.out.println("Running the naive TED");
        startTime=System.currentTimeMillis();

        NaiveBatchTED naive=new NaiveBatchTED(matchCollection,firstTGFD);
        Set<Violation> allViolationsNaiveBatchTED=naive.findViolations();
        System.out.println("Number of violations: " + allViolationsNaiveBatchTED.size());

        myConsole.print("Naive Batch TED", System.currentTimeMillis()-startTime);

        saveViolations("naive",allViolationsNaiveBatchTED,firstTGFD);


        // Next, we need to find all the violations using the optimize method
        System.out.println("Running the optimized TED");
        startTime=System.currentTimeMillis();

        OptBatchTED optimize=new OptBatchTED(matchCollection,firstTGFD);
        Set<Violation> allViolationsOptBatchTED=optimize.findViolations();
        System.out.println("Number of violations (Optimized method): " + allViolationsOptBatchTED.size());

        myConsole.print("Optimized Batch TED", System.currentTimeMillis()-startTime);

        if(properties.myProperties.saveViolations)
            saveViolations("optimized",allViolationsOptBatchTED,firstTGFD);

        myConsole.print("Total wall clock time: ", System.currentTimeMillis()-wallClockStart);

        myConsole.saveLogs("run_Incremental_"+ LocalDateTime.now().toString() + ".txt");
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
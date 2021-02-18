import BatchViolation.NaiveBatchTED;
import BatchViolation.OptBatchTED;
import TGFDLoader.TGFDGenerator;
import VF2Runner.VF2SubgraphIsomorphism;
import graphLoader.dbPediaLoader;
import infra.*;
import org.jgrapht.GraphMapping;
import util.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class testDbpedia
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

        HashMap<Integer, ArrayList<String>> typePathsById = new HashMap<>();
        HashMap<Integer, ArrayList<String>> dataPathsById = new HashMap<>();
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
                if (!typePathsById.containsKey(snapshotId))
                    typePathsById.put(snapshotId, new ArrayList<String>());
                typePathsById.get(snapshotId).add(conf[1]);
            }
            else if (conf[0].toLowerCase().startsWith("-d"))
            {
                var snapshotId = Integer.parseInt(conf[0].substring(2));
                if (!dataPathsById.containsKey(snapshotId))
                    dataPathsById.put(snapshotId, new ArrayList<String>());
                dataPathsById.get(snapshotId).add(conf[1]);
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
                properties.dbpediaProperties.optimizedLoadingBasedOnTGFD=Boolean.parseBoolean(conf[1]);
            }
        }
        // TODO: check that typesPaths.keySet == dataPaths.keySet [2021-02-14]

        // Test whether we loaded all the paths correctly

        System.out.println(dataPathsById.keySet() + " *** " + dataPathsById.values());

        System.out.println(typePathsById.keySet() + " *** " + typePathsById.values());

        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator(patternPath);
        List<TGFD> allTGFDs=generator.getTGFDs();
        //TGFD firstTGFD=allTGFDs.get(0);

        //Create the match collection for all the TGFDs in the list
        HashMap<TGFD, MatchCollection> allMatchCollections=new HashMap<>();
        for (TGFD tgfd:allTGFDs) {
            allMatchCollections.put(tgfd,new MatchCollection(tgfd.getPattern(),tgfd.getDependency(),tgfd.getDelta().getGranularity()));
        }

        //Load all the graph snapshots...

        Object[] ids=dataPathsById.keySet().toArray();
        Arrays.sort(ids);
        for (int i=0;i<ids.length;i++)
        {
            LocalDate currentSnapshotDate=timestamps.get(ids[i]);

            dbPediaLoader dbpedia = new dbPediaLoader(
                    typePathsById.get(ids[i]),
                    dataPathsById.get(ids[i]),allTGFDs);

            // Now, we need to find the matches for each snapshot.
            // Finding the matches...

            for (TGFD tgfd:allTGFDs) {

                VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
                System.out.println("\n########## Graph pattern ##########");
                System.out.println(tgfd.getPattern().toString());
                Iterator<GraphMapping<Vertex, RelationshipEdge>> results= VF2.execute(dbpedia.getGraph(), tgfd.getPattern(),false);


                //Retrieving and storing the matches of each timestamp.
                System.out.println("Retrieving the matches");

                long startTime=System.currentTimeMillis();

                allMatchCollections.get(tgfd).addMatches(currentSnapshotDate,results);

                printTime("Match retrieval", System.currentTimeMillis()-startTime);
            }
        }

        for (TGFD tgfd:allTGFDs) {
            // Now, we need to find all the violations
            //First, we run the Naive Batch TED
            System.out.println("Running the naive TED");
            long startTime=System.currentTimeMillis();

            NaiveBatchTED naive=new NaiveBatchTED(allMatchCollections.get(tgfd),tgfd);
            Set<Violation> allViolationsNaiveBatchTED=naive.findViolations();
            System.out.println("Number of violations: " + allViolationsNaiveBatchTED.size());

            printTime("Naive Batch TED", System.currentTimeMillis()-startTime);


            // Next, we need to find all the violations using the optimize method
            System.out.println("Running the optimized TED");
            startTime=System.currentTimeMillis();

            OptBatchTED optimize=new OptBatchTED(allMatchCollections.get(tgfd),tgfd);
            Set<Violation> allViolationsOptBatchTED=optimize.findViolations();
            System.out.println("Number of violations (Optimized method): " + allViolationsOptBatchTED.size());

            printTime("Optimized Batch TED", System.currentTimeMillis()-startTime);
        }

        printTime("Total wall clock time: ", System.currentTimeMillis()-wallClockStart);
    }

    private static void printTime(String message, long miliseconds)
    {
        System.out.println(message + " time: " + miliseconds + "(ms) ** " +
                TimeUnit.MILLISECONDS.toSeconds(miliseconds) + "(sec) ** " +
                TimeUnit.MILLISECONDS.toMinutes(miliseconds) +  "(min)");
    }
}


import BatchViolation.NaiveBatchTED;
import ChangeExploration.Change;
import ChangeExploration.ChangeLoader;
import ICs.TGFD;
import IncrementalRunner.IncUpdates;
import IncrementalRunner.IncrementalChange;
import Infra.*;
import Loader.GraphLoader;
import Loader.PDDLoader;
import Loader.TGFDGenerator;
import Util.Config;
import VF2Runner.VF2SubgraphIsomorphism;
import Violations.Violation;
import Violations.ViolationCollection;
import org.jgrapht.GraphMapping;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class testPDDInc
{

    public static void main(String []args) throws FileNotFoundException {

        long wallClockStart=System.currentTimeMillis();

        Config.parse(args[0]);
        // Create violation collection
        Violations.ViolationCollection collection=new Violations.ViolationCollection();

        // Test whether we loaded all the files correctly
        System.out.println(Arrays.toString(Config.getFirstDataFilePath().toArray()));
        System.out.println(Config.getDiffFilesPath().keySet() + " *** " + Config.getDiffFilesPath().values());

        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        List<TGFD> allTGFDs=generator.getTGFDs();

        //Create the match collection for all the TGFDs in the list
        HashMap<String, MatchCollection> matchCollectionHashMap=new HashMap <>();
        for (TGFD tgfd:allTGFDs) {
            matchCollectionHashMap.put(tgfd.getName(),new MatchCollection(tgfd.getPattern(),tgfd.getDependency(),tgfd.getDelta().getGranularity()));
        }

        //Load the first timestamp
        System.out.println("===========Snapshot 1 (" + Config.getTimestamps().get(1) + ")===========");
        long startTime=System.currentTimeMillis();
        LocalDate currentSnapshotDate= Config.getTimestamps().get(1);
        GraphLoader pdd = new PDDLoader(allTGFDs, Config.getFirstDataFilePath());
        printWithTime("Load graph 1 (" + Config.getTimestamps().get(1) + ")", System.currentTimeMillis()-startTime);

        if(Config.filterGraph) {
            for (TGFD tgfd : allTGFDs) {
                pdd.getGraph().filterGraphBasedOnTGFD(tgfd);
            }
        }

        // Now, we need to find the matches for the first snapshot.
        for (TGFD tgfd:allTGFDs) {
            VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
            System.out.println("\n###########"+tgfd.getName()+"###########");
            Iterator<GraphMapping<Vertex, RelationshipEdge>> results= VF2.execute(pdd.getGraph(), tgfd.getPattern(),false);

            //Retrieving and storing the matches of each timestamp.
            System.out.println("Retrieving the matches");
            startTime=System.currentTimeMillis();
            matchCollectionHashMap.get(tgfd.getName()).addMatches(currentSnapshotDate,results);
            printWithTime("Match retrieval", System.currentTimeMillis()-startTime);
        }

        //Load the change files
        Object[] ids= Config.getDiffFilesPath().keySet().toArray();
        Arrays.sort(ids);
        for (int i=0;i<ids.length;i++)
        {

            System.out.println("===========Snapshot "+ids[i]+" (" + Config.getTimestamps().get(ids[i]) + ")===========");

            startTime=System.currentTimeMillis();
            currentSnapshotDate= Config.getTimestamps().get((int)ids[i]);
            LocalDate prevTimeStamp=Config.getTimestamps().get(((int)ids[i])-1);
            ChangeLoader changeLoader=new ChangeLoader(Config.getDiffFilesPath().get(ids[i]));
            List<Change> changes=changeLoader.getAllChanges();

            printWithTime("Load changes "+ids[i]+" (" + Config.getTimestamps().get(ids[i]) + ")", System.currentTimeMillis()-startTime);
            System.out.println("Total number of changes: " + changes.size());

            // Now, we need to find the matches for each snapshot.
            // Finding the matches...

            startTime=System.currentTimeMillis();
            System.out.println("Updating the graph");
            IncUpdates incUpdatesOnPDD=new IncUpdates(pdd.getGraph(),allTGFDs);
            incUpdatesOnPDD.AddNewVertices(changes);

            HashMap<String,ArrayList<String>> newMatchesSignaturesByTGFD=new HashMap <>();
            HashMap<String,ArrayList<String>> removedMatchesSignaturesByTGFD=new HashMap <>();
            HashMap<String,TGFD> tgfdsByName=new HashMap <>();
            System.out.println("Done1");
            for (TGFD tgfd:allTGFDs) {
                newMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
                removedMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
                tgfdsByName.put(tgfd.getName(),tgfd);
            }
            System.out.println("Done2");
            for (Change change:changes) {

                //System.out.print("\n" + change.getId() + " --> ");
                HashMap<String,IncrementalChange> incrementalChangeHashMap=incUpdatesOnPDD.updateGraph(change,tgfdsByName);
                if(incrementalChangeHashMap==null)
                    continue;
                for (String tgfdName:incrementalChangeHashMap.keySet()) {
                    newMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getNewMatches().keySet());
                    removedMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getRemovedMatchesSignatures());
                    matchCollectionHashMap.get(tgfdName).addMatches(currentSnapshotDate,incrementalChangeHashMap.get(tgfdName).getNewMatches());
                }
            }
            System.out.println("Done3");
            for (TGFD tgfd:allTGFDs) {
                matchCollectionHashMap.get(tgfd.getName()).addTimestamp(currentSnapshotDate, prevTimeStamp,
                        newMatchesSignaturesByTGFD.get(tgfd.getName()),removedMatchesSignaturesByTGFD.get(tgfd.getName()));
                System.out.println("New matches ("+tgfd.getName()+"): " +
                        newMatchesSignaturesByTGFD.get(tgfd.getName()).size() + " ** " + removedMatchesSignaturesByTGFD.get(tgfd.getName()).size());
            }
            printWithTime("Update and retrieve matches ", System.currentTimeMillis()-startTime);
            //myConsole.print("#new matches: " + newMatchesSignatures.size()  + " - #removed matches: " + removedMatchesSignatures.size());
        }

        for (TGFD tgfd:allTGFDs) {

            // Now, we need to find all the violations
            //First, we run the Naive Batch TED
            System.out.println("==========="+tgfd.getName()+"===========");

            /*
            System.out.println("Running the naive TED");
            startTime=System.currentTimeMillis();
            NaiveBatchTED naive=new NaiveBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
            Set<Violation> allViolationsNaiveBatchTED=naive.findViolations();
            System.out.println("Number of violations: " + allViolationsNaiveBatchTED.size());
            myConsole.print("Naive Batch TED", System.currentTimeMillis()-startTime);
            if(properties.myProperties.saveViolations)
                saveViolations("naive",allViolationsNaiveBatchTED,tgfd);
            */


            // we only need to run optimize method to find the violations

            System.out.println("Running the naive TED");

            startTime=System.currentTimeMillis();
            NaiveBatchTED naive=new NaiveBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
            Set<Violation> allViolationsOptBatchTED=naive.findViolations();
            System.out.println("Number of violations (Naive method): " + allViolationsOptBatchTED.size());
            collection.addViolations(tgfd, allViolationsOptBatchTED); // Add violation into violation collection !!!!!!!!!!!!
            printWithTime("Naive Batch TED", System.currentTimeMillis()-startTime);
            if(Config.saveViolations)
                saveViolations("/Users/lexie/Desktop/Master_Project/Violations/P1612/naive","/Users/lexie/Desktop/Master_Project/Violations/P1612/match",allViolationsOptBatchTED,tgfd,collection, matchCollectionHashMap);
        }
        printWithTime("Total wall clock time: ", System.currentTimeMillis()-wallClockStart);
    }

    private static void printWithTime(String message, long runTimeInMS)
    {
        System.out.println(message + " time: " + runTimeInMS + "(ms) ** " +
                TimeUnit.MILLISECONDS.toSeconds(runTimeInMS) + "(sec) ** " +
                TimeUnit.MILLISECONDS.toMinutes(runTimeInMS) +  "(min)");
    }

    private static void saveViolations(String path1, String path2, Set<Violation> violations, TGFD tgfd, ViolationCollection collection, HashMap<String, MatchCollection> matchCollectionHashMap)
    {
        try {
            FileWriter file1 = new FileWriter(path1 +"_" + tgfd.getName() + ".txt");
            file1.write("***************TGFD***************\n");
            file1.write(tgfd.toString());
            file1.write("\n===============Violations===============\n");
            int i =1;
            for (Violation vio:violations) {
                file1.write(i+".");
                file1.write(vio.toString() +
                        "\nPatters1: " + vio.getMatch1().getSignatureFromPattern() +
                        "\nPatters2: " + vio.getMatch2().getSignatureFromPattern() +
                        "\n---------------------------------------------------\n");
                i++;
            }
            file1.close();
//            file.write("\n===============Sorted Violation Collection===============\n");
//            ArrayList<Match> sort_list = collection.sortViolationList();
//            for(Match match:sort_list){
//
//                file.write(match.getIntervals()+
//                        "\n---------------------------------------------------\n");
//
////                List<Violation> vio_list = collection.getViolation(match);
////                for (Violation vio:vio_list) {
////                    file.write(vio.toString() +
////                            "\n---------------------------------------------------\n");
////                }
//
//
//            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        try {
            FileWriter file2 = new FileWriter(path2 +"_" + tgfd.getName() + ".txt");
            file2.write("\n===============Unique Error Matches (Frequency of Occurrence)===============\n");
//            int j =1;
//            for (Match match:collection.getKeySet()) {
//                System.out.println("match!!"+match.toString());
//                file2.write(j+".");
//                file2.write(match.toString());
//                file2.write("\n---------------------------------------------------\n");
//                j++;
//
//
//            }
            int j =1;
            for(Map.Entry<String, MatchCollection> entry:matchCollectionHashMap.entrySet()){
                MatchCollection matchCollection = entry.getValue();
                for(Match match:matchCollection.getMatches()){
                    System.out.println("match!!"+match.toString());
                    file2.write(j+".");
                    file2.write(match.toString());
                    file2.write("\n---------------------------------------------------\n");
                    j++;
                }
            }





//            file.write("\n===============Sorted Error Matches (Frequency of Occurrence)===============\n");
//            /*Problems for multiple TGFDs*/
//            ArrayList<Match> sort_list = collection.sortMatchList();
//            for(Match match:sort_list){
//
//                file.write(match.getSignatureX()+
//                        "\n---------------------------------------------------\n");
//
////                List<Violation> vio_list = collection.getViolation(match);
////                for (Violation vio:vio_list) {
////                    file.write(vio.toString() +
////                            "\n---------------------------------------------------\n");
////                }
//
//
//            }
//
//
//            file.close();
            file2.close();
            System.out.println("Successfully wrote to the file: " + path1+", "+path2);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

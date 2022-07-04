

import BatchViolation.NaiveBatchTED;
import ICs.TGFD;
import IncrementalRunner.IncUpdates;
import IncrementalRunner.IncrementalChange;
import Loader.TGFDGenerator;
import VF2Runner.VF2SubgraphIsomorphism;
import Violations.Violation;
import Violations.ViolationCollection;
import ChangeExploration.Change;
import ChangeExploration.ChangeLoader;
import Loader.DBPediaLoader;
import Infra.*;
import org.jgrapht.GraphMapping;
import Util.Config;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class testDbpediaInc
{
    public static void main(String []args) throws FileNotFoundException {

        long wallClockStart=System.currentTimeMillis();

        Config.parse(args[0]);

        System.out.println("Test DBPedia incremental");

        // Create violation collection
        Violations.ViolationCollection collection=new Violations.ViolationCollection();

        // Test whether we loaded all the files correctly

        System.out.println(Arrays.toString(Config.getFirstTypesFilePath().toArray()) + " *** " + Arrays.toString(Config.getFirstDataFilePath().toArray()));
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
        System.out.println("-----------Snapshot (1)-----------");
        long startTime=System.currentTimeMillis();
        LocalDate currentSnapshotDate= Config.getTimestamps().get(1);
        DBPediaLoader dbpedia = new DBPediaLoader(allTGFDs, Config.getFirstTypesFilePath(), Config.getFirstDataFilePath());

//        for (Vertex v:dbpedia.getGraph().getGraph().vertexSet()) {
//            DataVertex vv=(DataVertex) v;
//            if(vv.getAllAttributesHashMap().get("uri").getAttrValue().equals("ahmad_jamal"))
//                for (Attribute attribute:vv.getAllAttributesList()) {
//                    System.out.println(attribute);
//                }
//            if(vv.getAllAttributesHashMap().get("uri").getAttrValue().equals("rossiter_road")) {
//                for (Attribute attribute : vv.getAllAttributesList()) {
//                    System.out.println(attribute);
//                }
//                for (RelationshipEdge e:dbpedia.getGraph().getGraph().outgoingEdgesOf(vv)) {
//                    System.out.println(e.getLabel());
//                }
//            }
//        }

        printWithTime("Load graph (1)", System.currentTimeMillis()-startTime);

        // Now, we need to find the matches for each snapshot.
        // Finding the matches...

        for (TGFD tgfd:allTGFDs) {
            VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
            System.out.println("\n###########"+tgfd.getName()+"###########");
            Iterator<GraphMapping<Vertex, RelationshipEdge>> results= VF2.execute(dbpedia.getGraph(), tgfd.getPattern(),false);

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
            System.out.println("-----------Snapshot (" + ids[i] + ")-----------");

            startTime=System.currentTimeMillis();
            currentSnapshotDate= Config.getTimestamps().get((int)ids[i]);
            LocalDate prevTimeStamp=Config.getTimestamps().get(((int)ids[i])-1);
            ChangeLoader changeLoader=new ChangeLoader(Config.getDiffFilesPath().get(ids[i]));
            List<Change> changes=changeLoader.getAllChanges();

            printWithTime("Load changes ("+ids[i] + ")", System.currentTimeMillis()-startTime);
            System.out.println("Total number of changes: " + changes.size());

            // Now, we need to find the matches for each snapshot.
            // Finding the matches...

            startTime=System.currentTimeMillis();
            System.out.println("Updating the graph");
            IncUpdates incUpdatesOnDBpedia=new IncUpdates(dbpedia.getGraph(),allTGFDs);
            incUpdatesOnDBpedia.AddNewVertices(changes);

            HashMap<String,ArrayList<String>> newMatchesSignaturesByTGFD=new HashMap <>();
            HashMap<String,ArrayList<String>> removedMatchesSignaturesByTGFD=new HashMap <>();
            HashMap<String,TGFD> tgfdsByName=new HashMap <>();
            for (TGFD tgfd:allTGFDs) {
                newMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
                removedMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
                tgfdsByName.put(tgfd.getName(),tgfd);
            }
            for (Change change:changes) {

                //System.out.print("\n" + change.getId() + " --> ");
                HashMap<String,IncrementalChange> incrementalChangeHashMap=incUpdatesOnDBpedia.updateGraph(change,tgfdsByName);
                if(incrementalChangeHashMap==null)
                    continue;
                for (String tgfdName:incrementalChangeHashMap.keySet()) {
                    newMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getNewMatches().keySet());
                    removedMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getRemovedMatchesSignatures());
                    matchCollectionHashMap.get(tgfdName).addMatches(currentSnapshotDate,incrementalChangeHashMap.get(tgfdName).getNewMatches());
                }
            }
            for (TGFD tgfd:allTGFDs) {
                matchCollectionHashMap.get(tgfd.getName()).addTimestamp(currentSnapshotDate,prevTimeStamp,
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


            System.out.println("Running the naive TED");
            startTime=System.currentTimeMillis();
            NaiveBatchTED naive=new NaiveBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
            Set<Violation> allViolationsNaiveBatchTED=naive.findViolations();
            System.out.println("Number of violations: " + allViolationsNaiveBatchTED.size());
            collection.addViolations(tgfd, allViolationsNaiveBatchTED); // Add violation into violation collection !!!!!!!!!!!!
            printWithTime("Naive Batch TED", System.currentTimeMillis()-startTime);
            if(Config.saveViolations)
//                saveViolations("naive",allViolationsNaiveBatchTED,tgfd,collection);
                saveViolations("naive",allViolationsNaiveBatchTED,tgfd,collection);



            // we only need to run optimize method to find the violations

//            System.out.println("Running the optimized TED");
//
//            startTime=System.currentTimeMillis();
//            OptBatchTED optimize=new OptBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
//            Set<Violation> allViolationsOptBatchTED=optimize.findViolations();
//            System.out.println("Number of violations (Optimized method): " + allViolationsOptBatchTED.size());
//            printWithTime("Optimized Batch TED", System.currentTimeMillis()-startTime);
//            if(Config.saveViolations)
//                saveViolations("optimized",allViolationsOptBatchTED,tgfd);
        }

        printWithTime("Total wall clock time: ", System.currentTimeMillis()-wallClockStart);
    }

    private static void printWithTime(String message, long runTimeInMS)
    {
        System.out.println(message + " time: " + runTimeInMS + "(ms) ** " +
                TimeUnit.MILLISECONDS.toSeconds(runTimeInMS) + "(sec) ** " +
                TimeUnit.MILLISECONDS.toMinutes(runTimeInMS) +  "(min)");
    }

    private static void saveViolations(String path, Set<Violation> violations, TGFD tgfd, ViolationCollection collection)
    {
        try {
            FileWriter file = new FileWriter(path +"_" + tgfd.getName() + ".txt");
            file.write("***************TGFD***************\n");
            file.write(tgfd.toString());
            file.write("\n===============Violations===============\n");
            int i =1;
            for (Violation vio:violations) {
                file.write(i+".");
                file.write(vio.toString() +
                        "\n---------------------------------------------------\n");
                i++;
            }

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



            file.close();
            System.out.println("Successfully wrote to the file: " + path);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
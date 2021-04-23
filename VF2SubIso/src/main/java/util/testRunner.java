package util;

import BatchViolation.OptBatchTED;
import IncrementalRunner.IncUpdates;
import IncrementalRunner.IncrementalChange;
import TGFDLoader.TGFDGenerator;
import VF2Runner.VF2SubgraphIsomorphism;
import changeExploration.Change;
import changeExploration.ChangeLoader;
import graphLoader.GraphLoader;
import graphLoader.IMDBLoader;
import infra.*;
import org.jgrapht.GraphMapping;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class testRunner {


    public testRunner()
    {
    }

    public String run()
    {
        long wallClockStart=System.currentTimeMillis();

        StringBuilder msg=new StringBuilder();

        System.out.println("Test Incremental algorithm for the IMDB dataset from testRunner");

        // Test whether we loaded all the files correctly
        System.out.println(Arrays.toString(ConfigParser.getFirstDataFilePath().toArray()));
        System.out.println(ConfigParser.getDiffFilesPath().keySet() + " *** " + ConfigParser.getDiffFilesPath().values());

        TGFDGenerator generator = new TGFDGenerator(ConfigParser.patternPath);
        List<TGFD> tgfds=generator.getTGFDs();

        //Create the match collection for all the TGFDs in the list
        HashMap <String, MatchCollection> matchCollectionHashMap=new HashMap <>();
        for (TGFD tgfd:tgfds) {
            matchCollectionHashMap.put(tgfd.getName(),new MatchCollection(tgfd.getPattern(),tgfd.getDependency(),tgfd.getDelta().getGranularity()));
        }

        //Load the first timestamp
        System.out.println("===========Snapshot 1 (" + ConfigParser.getTimestamps().get(1) + ")===========");
        long startTime=System.currentTimeMillis();
        LocalDate currentSnapshotDate=ConfigParser.getTimestamps().get(1);
        GraphLoader imdb = new IMDBLoader(tgfds,ConfigParser.getFirstDataFilePath());
        printWithTime("Load graph 1 (" + ConfigParser.getTimestamps().get(1) + ")", System.currentTimeMillis()-startTime);

        // Now, we need to find the matches for the first snapshot.
        for (TGFD tgfd:tgfds) {
            VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
            System.out.println("\n###########"+tgfd.getName()+"###########");
            Iterator <GraphMapping <Vertex, RelationshipEdge>> results= VF2.execute(imdb.getGraph(), tgfd.getPattern(),false);

            //Retrieving and storing the matches of each timestamp.
            System.out.println("Retrieving the matches");
            startTime=System.currentTimeMillis();
            matchCollectionHashMap.get(tgfd.getName()).addMatches(currentSnapshotDate,results);
            printWithTime("Match retrieval", System.currentTimeMillis()-startTime);
        }

        //Load the change files
        Object[] ids=ConfigParser.getDiffFilesPath().keySet().toArray();
        Arrays.sort(ids);
        for (int i=0;i<ids.length;i++)
        {
            System.out.println("===========Snapshot "+ids[i]+" (" + ConfigParser.getTimestamps().get(ids[i]) + ")===========");

            startTime=System.currentTimeMillis();
            currentSnapshotDate=ConfigParser.getTimestamps().get((int)ids[i]);
            ChangeLoader changeLoader=new ChangeLoader(ConfigParser.getDiffFilesPath().get(ids[i]));
            List<Change> changes=changeLoader.getAllChanges();

            printWithTime("Load changes "+ids[i]+" (" + ConfigParser.getTimestamps().get(ids[i]) + ")", System.currentTimeMillis()-startTime);
            System.out.println("Total number of changes: " + changes.size());

            // Now, we need to find the matches for each snapshot.
            // Finding the matches...

            startTime=System.currentTimeMillis();
            System.out.println("Updating the graph");
            IncUpdates incUpdatesOnDBpedia=new IncUpdates(imdb.getGraph(),tgfds);
            incUpdatesOnDBpedia.AddNewVertices(changes);

            HashMap<String, ArrayList <String>> newMatchesSignaturesByTGFD=new HashMap <>();
            HashMap<String,ArrayList<String>> removedMatchesSignaturesByTGFD=new HashMap <>();
            HashMap<String,TGFD> tgfdsByName=new HashMap <>();
            for (TGFD tgfd:tgfds) {
                newMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
                removedMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
                tgfdsByName.put(tgfd.getName(),tgfd);
            }
            for (Change change:changes) {

                //System.out.print("\n" + change.getId() + " --> ");
                HashMap<String, IncrementalChange> incrementalChangeHashMap=incUpdatesOnDBpedia.updateGraph(change,tgfdsByName);
                if(incrementalChangeHashMap==null)
                    continue;
                for (String tgfdName:incrementalChangeHashMap.keySet()) {
                    newMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getNewMatches().keySet());
                    removedMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getRemovedMatchesSignatures());
                    matchCollectionHashMap.get(tgfdName).addMatches(currentSnapshotDate,incrementalChangeHashMap.get(tgfdName).getNewMatches());
                }
            }
            for (TGFD tgfd:tgfds) {
                matchCollectionHashMap.get(tgfd.getName()).addTimestamp(currentSnapshotDate,
                        newMatchesSignaturesByTGFD.get(tgfd.getName()),removedMatchesSignaturesByTGFD.get(tgfd.getName()));
                System.out.println("New matches ("+tgfd.getName()+"): " +
                        newMatchesSignaturesByTGFD.get(tgfd.getName()).size() + " ** " + removedMatchesSignaturesByTGFD.get(tgfd.getName()).size());
            }
            printWithTime("Update and retrieve matches ", System.currentTimeMillis()-startTime);
            //myConsole.print("#new matches: " + newMatchesSignatures.size()  + " - #removed matches: " + removedMatchesSignatures.size());
        }

        for (TGFD tgfd:tgfds) {

            System.out.println("==========="+tgfd.getName()+"===========");

            System.out.println("Running the optimized TED");

            startTime=System.currentTimeMillis();
            OptBatchTED optimize=new OptBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
            Set<Violation> allViolationsOptBatchTED=optimize.findViolations();
            System.out.println("Number of violations (Optimized method): " + allViolationsOptBatchTED.size());
            printWithTime("Optimized Batch TED", System.currentTimeMillis()-startTime);
            msg.append(getViolationsMessage(allViolationsOptBatchTED,tgfd));
        }
        printWithTime("Total wall clock time: ", System.currentTimeMillis()-wallClockStart);
        return msg.toString();
    }

    private String getViolationsMessage(Set<Violation> violations, TGFD tgfd)
    {
        StringBuilder msg=new StringBuilder();
        msg.append("***************TGFD***************\n");
        msg.append(tgfd.toString());
        msg.append("\n===============Violations===============\n");
        for (Violation vio:violations) {
            msg.append(vio.toString() +
                    "\n---------------------------------------------------\n");
        }
        return msg.toString();
    }

    private void printWithTime(String message, long runTimeInMS)
    {
        System.out.println(message + " time: " + runTimeInMS + "(ms) ** " +
                TimeUnit.MILLISECONDS.toSeconds(runTimeInMS) + "(sec) ** " +
                TimeUnit.MILLISECONDS.toMinutes(runTimeInMS) +  "(min)");
    }

}

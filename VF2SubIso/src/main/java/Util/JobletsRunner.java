package Util;

import BatchViolation.OptBatchTED;
import IncrementalRunner.IncUpdates;
import IncrementalRunner.IncrementalChange;
import Infra.*;
import Loader.DBPediaLoader;
import Loader.GraphLoader;
import Loader.IMDBLoader;
import Loader.SyntheticLoader;
import TGFDLoader.TGFDGenerator;
import VF2Runner.VF2SubgraphIsomorphism;
import Workload.Joblet;
import changeExploration.Change;
import changeExploration.ChangeLoader;
import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class JobletsRunner {

    private GraphLoader loader=null;
    private List<TGFD> tgfds;
    private HashMap<String, TGFD> tgfdByName=new HashMap<>();
    private long wallClockTime=0;
    private HashSet<Joblet> assignedJoblets;
    private String jobletsInRawString;

    private HashMap <String, MatchCollection> matchCollectionHashMap;

    public JobletsRunner()
    {
        System.out.println("Test Incremental algorithm for the "+ Config.dataset+" dataset from testRunner");
        assignedJoblets=new HashSet<>();
    }

    public void load()
    {
        long startTime=System.currentTimeMillis();

        // Test whether we loaded all the files correctly
        System.out.println(Arrays.toString(Config.getFirstDataFilePath().toArray()));
        System.out.println(Config.getDiffFilesPath().keySet() + " *** " + Config.getDiffFilesPath().values());

        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        tgfds=generator.getTGFDs();
        tgfds.forEach(tgfd -> tgfdByName.put(tgfd.getName(), tgfd));

        //Load the first timestamp
        System.out.println("===========Snapshot 1 (" + Config.getTimestamps().get(1) + ")===========");

        if(Config.dataset.equals("dbpedia"))
        {
            loader = new DBPediaLoader(tgfds, Config.getFirstTypesFilePath(), Config.getFirstDataFilePath());
        }
        else if(Config.dataset.equals("synthetic"))
        {
            loader = new SyntheticLoader(tgfds, Config.getFirstDataFilePath());
        }
        else // default is imdb
        {
            loader = new IMDBLoader(tgfds, Config.getFirstDataFilePath());
        }
        printWithTime("Load graph 1 (" + Config.getTimestamps().get(1) + ")", System.currentTimeMillis()-startTime);

        wallClockTime+=System.currentTimeMillis()-startTime;

    }

    public void setJobletsInRawString(String jobletsInRawString) {
        this.jobletsInRawString = jobletsInRawString;
    }

    public void generateJoblets()
    {
        if(jobletsInRawString!=null)
        {
            int id=0;
            String []temp=jobletsInRawString.split("\n");
            for (int i=1;i<temp.length;i++)
            {
                String []arr=temp[i].split("#");
                if(arr.length==2)
                {
                    id++;
                    Joblet joblet=new Joblet(id,(DataVertex) loader.getGraph().getNode(arr[0]),tgfdByName.get(arr[1]),tgfdByName.get(arr[1]).getPattern().getDiameter(),0);
                    assignedJoblets.add(joblet);
                }
            }
        }
    }

    public void runTheFirstSnapshot()
    {
        if(loader==null)
        {
            System.out.println("Graph is not loaded yet");
            return;
        }
        StringBuilder msg=new StringBuilder();

        long startTime, functionWallClockTime=System.currentTimeMillis();
        LocalDate currentSnapshotDate= Config.getTimestamps().get(1);

        //Create the match collection for all the TGFDs in the list
        matchCollectionHashMap=new HashMap <>();
        for (TGFD tgfd:tgfds) {
            matchCollectionHashMap.put(tgfd.getName(),new MatchCollection(tgfd.getPattern(),tgfd.getDependency(),tgfd.getDelta().getGranularity()));
        }

        // Now, we need to find the matches for the first snapshot.
        System.out.println("Retrieving matches for all the joblets.");
        VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();

        startTime=System.currentTimeMillis();
        for (Joblet joblet:assignedJoblets) {

            Graph<Vertex, RelationshipEdge> subgraph = loader.getGraph().getSubGraphWithinDiameter(joblet.getCenterNode(), joblet.getDiameter());
            Iterator <GraphMapping <Vertex, RelationshipEdge>> results= VF2.execute(subgraph, joblet.getTGFD().getPattern(),false);
            matchCollectionHashMap.get(joblet.getTGFD().getName()).addMatches(currentSnapshotDate,results);
        }
        printWithTime("Match retrieval", System.currentTimeMillis()-startTime);

    }

    public void runTheNextTimestamp(List<Change> changes)
    {
        //Load the change files
        Object[] ids= Config.getDiffFilesPath().keySet().toArray();
        Arrays.sort(ids);
        for (int i=0;i<ids.length;i++)
        {
            System.out.println("===========Snapshot "+ids[i]+" (" + Config.getTimestamps().get(ids[i]) + ")===========");

            long startTime=System.currentTimeMillis();
            LocalDate currentSnapshotDate= Config.getTimestamps().get((int)ids[i]);

            printWithTime("Load changes "+ids[i]+" (" + Config.getTimestamps().get(ids[i]) + ")", System.currentTimeMillis()-startTime);
            System.out.println("Total number of changes: " + changes.size());

            // Now, we need to find the matches for each snapshot.
            // Finding the matches...

            startTime=System.currentTimeMillis();
            System.out.println("Updating the graph");
            IncUpdates incUpdatesOnDBpedia=new IncUpdates(loader.getGraph(),tgfds);
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
    }

    public GraphLoader getLoader() {
        return loader;
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

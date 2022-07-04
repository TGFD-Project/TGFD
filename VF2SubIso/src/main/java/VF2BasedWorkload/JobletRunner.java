package VF2BasedWorkload;

import ICs.TGFD;
import IncrementalRunner.IncrementalChange;
import IncrementalRunner.IncUpdates;
import Infra.*;
import Loader.*;
import Util.Config;
import VF2Runner.VF2SubgraphIsomorphism;
import ChangeExploration.Change;
import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class JobletRunner {

    private GraphLoader loader=null;
    private List<TGFD> tgfds;
    private HashMap<String, TGFD> tgfdByName=new HashMap<>();
    private long wallClockTime=0;
    private HashMap<Integer, Joblet> assignedJoblets;
    private String jobletsInRawString;

    private HashMap <String, MatchCollection> matchCollectionHashMap;

    public JobletRunner()
    {
        System.out.println("Test Incremental algorithm for the "+ Config.datasetName +" dataset from testRunner");
        assignedJoblets=new HashMap<>();
    }

    public void load()
    {
        long startTime=System.currentTimeMillis();

        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        tgfds=generator.getTGFDs();
        tgfds.forEach(tgfd -> tgfdByName.put(tgfd.getName(), tgfd));

        //Load the first timestamp
        System.out.println("===========Snapshot 1 (" + Config.getTimestamps().get(1) + ")===========");

        if(Config.datasetName== Config.dataset.dbpedia)
            loader = new DBPediaLoader(tgfds, Config.getFirstTypesFilePath(), Config.getFirstDataFilePath());
        else if(Config.datasetName == Config.dataset.synthetic)
            loader = new SyntheticLoader(tgfds, Config.getFirstDataFilePath());
        else if(Config.datasetName == Config.dataset.pdd)
            loader = new PDDLoader(tgfds, Config.getFirstDataFilePath());
        else if(Config.datasetName == Config.dataset.imdb) // default is imdb
            loader = new IMDBLoader(tgfds, Config.getFirstDataFilePath());

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
            String []temp=jobletsInRawString.split("\n");
            for (int i=1;i<temp.length;i++)
            {
                String []arr=temp[i].split("#");
                if(arr.length==3)
                {
                    Joblet joblet=new Joblet(Integer.parseInt(arr[0]),(DataVertex) loader.getGraph().getNode(arr[1]),tgfdByName.get(arr[2]),tgfdByName.get(arr[2]).getPattern().getDiameter(),0);
                    assignedJoblets.put(joblet.getId(), joblet);
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
        for (Joblet joblet:assignedJoblets.values()) {
            Graph<Vertex, RelationshipEdge> subgraph = loader.getGraph().getSubGraphWithinDiameter(joblet.getCenterNode(), joblet.getDiameter(),joblet.getTGFD());
            joblet.setSubgraph(subgraph);
            Iterator <GraphMapping <Vertex, RelationshipEdge>> results= VF2.execute(subgraph, joblet.getTGFD().getPattern(),false);
            matchCollectionHashMap.get(joblet.getTGFD().getName()).addMatches(currentSnapshotDate,results);
        }
        printWithTime("Match retrieval", System.currentTimeMillis()-startTime);
    }

    public void runTheNextTimestamp(List<Change> changes, int superstep)
    {
        //Load the change files
        System.out.println("===========Snapshot "+superstep+" (" + Config.getTimestamps().get(superstep) + ")===========");

        long startTime=System.currentTimeMillis();
        LocalDate currentSnapshotDate= Config.getTimestamps().get(superstep);
        System.out.println("Total number of changes: " + changes.size());

        // Now, we need to find the matches for each snapshot.
        // Finding the matches...

        System.out.println("Updating the graph");
//        IncUpdates incUpdatesOnDBpedia=new IncUpdates(loader.getGraph(),tgfds);
//        incUpdatesOnDBpedia.AddNewVertices(changes);

        HashMap<String, ArrayList <String>> newMatchesSignaturesByTGFD=new HashMap <>();
        HashMap<String,ArrayList<String>> removedMatchesSignaturesByTGFD=new HashMap <>();
        HashMap<String,TGFD> tgfdsByName=new HashMap <>();
        for (TGFD tgfd:tgfds) {
            newMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
            removedMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
            tgfdsByName.put(tgfd.getName(),tgfd);
        }

        for (Change change:changes) {
            for (int jobletID:change.getJobletIDs()) {
                if(assignedJoblets.containsKey(jobletID))
                {
                    IncUpdates incUpdatesOnDBpedia=new IncUpdates(assignedJoblets.get(jobletID).getSubgraph(),tgfds);
                    HashMap<String, IncrementalChange> incrementalChangeHashMap=incUpdatesOnDBpedia.updateGraph(change,tgfdsByName);
                    if(incrementalChangeHashMap==null)
                        continue;
                    for (String tgfdName:incrementalChangeHashMap.keySet()) {
                        newMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getNewMatches().keySet());
                        removedMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getRemovedMatchesSignatures());
                        matchCollectionHashMap.get(tgfdName).addMatches(currentSnapshotDate,incrementalChangeHashMap.get(tgfdName).getNewMatches());
                    }
                }
            }
        }
        for (TGFD tgfd:tgfds) {
            matchCollectionHashMap.get(tgfd.getName())
                    .addTimestamp(currentSnapshotDate, newMatchesSignaturesByTGFD.get(tgfd.getName()),removedMatchesSignaturesByTGFD.get(tgfd.getName()));

            System.out.println("New matches ("+tgfd.getName()+"): " +
                    newMatchesSignaturesByTGFD.get(tgfd.getName()).size() + " ** " + removedMatchesSignaturesByTGFD.get(tgfd.getName()).size());
        }
        printWithTime("Update and retrieve matches ", System.currentTimeMillis()-startTime);
    }

    public GraphLoader getLoader() {
        return loader;
    }


    private void printWithTime(String message, long runTimeInMS)
    {
        System.out.println(message + " time: " + runTimeInMS + "(ms) ** " +
                TimeUnit.MILLISECONDS.toSeconds(runTimeInMS) + "(sec) ** " +
                TimeUnit.MILLISECONDS.toMinutes(runTimeInMS) +  "(min)");
    }

}

package QPathBasedWorkload;

import ICs.TGFD;
import Infra.*;
import Loader.*;
import Util.Config;
import VF2Runner.VF2SubgraphIsomorphism;
import ChangeExploration.Change;
import org.jgrapht.Graph;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JobRunner {

    private GraphLoader loader=null;
    private List<TGFD> tgfds;
    //private HashMap<String, TGFD> tgfdByName=new HashMap<>();
    private HashMap<String, Query> queriesByName=new HashMap<>();
    private long wallClockTime=0;
    private HashMap<Integer, Job> assignedJobs;
    private String jobsInRawString;

    private HashMap <String, MatchCollection> matchCollectionHashMap;

    public JobRunner()
    {
        System.out.println("Test Incremental algorithm for the "+ Config.datasetName +" dataset from testRunner");
        assignedJobs=new HashMap<>();
    }

    public void load()
    {
        long startTime=System.currentTimeMillis();

        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        tgfds=generator.getTGFDs();
        tgfds.forEach(tgfd -> queriesByName.put(tgfd.getName(), new Query(tgfd)));

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

    public void setJobsInRawString(String jobsInRawString) {
        this.jobsInRawString = jobsInRawString;
    }

    public void generateJobs()
    {
        if(jobsInRawString!=null)
        {
            String []temp=jobsInRawString.split("\n");
            for (int i=1;i<temp.length;i++)
            {
                String []arr=temp[i].split("#");
                if(arr.length==3)
                {
                    Job job=new Job(Integer.parseInt(arr[0]),(DataVertex) loader.getGraph().getNode(arr[1]),queriesByName.get(arr[2]),queriesByName.get(arr[2]).getTGFD().getPattern().getDiameter(),0);
                    assignedJobs.put(job.getJobID(), job);
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
        for (Job job:assignedJobs.values()) {
            Graph<Vertex, RelationshipEdge> subgraph = loader.getGraph().getSubGraphWithinDiameter(job.getCenterNode(), job.getDiameter(),job.getQuery().getTGFD());
            job.setSubgraph(subgraph);
            job.runTheFirstSnapshot();
            var results= job.findMatchMapping();
            matchCollectionHashMap.get(job.getQuery().getTGFD().getName()).addMatches(currentSnapshotDate,results);
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
            for (int jobID:change.getJobletIDs()) {
                if(assignedJobs.containsKey(jobID))
                {
                    // Previous Matches
                    HashSet<String> prevMatches = new HashSet<>(assignedJobs.get(jobID).getLatestMappingSignatures().keySet());

                    // Apply the change
                    assignedJobs.get(jobID).applyChangeForTheNextSnapshot(change);

                    //Removed matches signature
                    HashSet<String> removedMatchesSignature=new HashSet<>();

                    //New matches to be stored in the match collection
                    HashMap <String,VertexMapping> newMatches=new HashMap<>();

                    for (String signature:assignedJobs.get(jobID).getLatestMappingSignatures().keySet()) {
                        if(!prevMatches.contains(signature))
                            newMatches.put(signature,assignedJobs.get(jobID).getLatestMappingSignatures().get(signature));
                    }
                    for (String key:prevMatches) {
                        if(!assignedJobs.get(jobID).getLatestMappingSignatures().containsKey(key))
                            removedMatchesSignature.add(key);
                    }

                    String tgfdName=assignedJobs.get(jobID).getQuery().getTGFD().getName();
                    newMatchesSignaturesByTGFD.get(tgfdName).addAll(newMatches.keySet());
                    removedMatchesSignaturesByTGFD.get(tgfdName).addAll(removedMatchesSignature);
                    matchCollectionHashMap.get(tgfdName).addMatches(currentSnapshotDate,newMatches.values());
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

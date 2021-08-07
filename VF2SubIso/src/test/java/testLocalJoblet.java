import IncrementalRunner.IncUpdates;
import IncrementalRunner.IncrementalChange;
import Infra.*;
import Loader.DBPediaLoader;
import Loader.GraphLoader;
import TGFDLoader.TGFDGenerator;
import Util.Config;
import VF2Runner.VF2SubgraphIsomorphism;
import Workload.Joblet;
import changeExploration.Change;
import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

public class testLocalJoblet {

    public static HashMap <String, MatchCollection> matchCollectionHashMap;

    public static void main(String []args) throws FileNotFoundException
    {
        Config.parse(args[0]);

        System.out.println(Arrays.toString(Config.getFirstTypesFilePath().toArray()) + " *** " + Arrays.toString(Config.getFirstDataFilePath().toArray()));
        System.out.println(Config.getDiffFilesPath().keySet() + " *** " + Config.getDiffFilesPath().values());

        matchCollectionHashMap=new HashMap <>();

        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        List<TGFD> allTGFDs=generator.getTGFDs();

        DBPediaLoader dbpedia = new DBPediaLoader(allTGFDs, Config.getFirstTypesFilePath(), Config.getFirstDataFilePath());

        HashMap<Integer, Joblet> joblets=defineJoblets(dbpedia,allTGFDs);
    }

    public static HashMap<Integer, Joblet> defineJoblets(GraphLoader loader, List<TGFD> tgfds)
    {
        HashMap<Integer, Joblet> jobletsByID=new HashMap<>();
        int jobletID=0;
        for (TGFD tgfd:tgfds) {
            System.out.println("TGFD: " + tgfd.getName() + " with the center type: " + tgfd.getPattern().getCenterVertexType());
            String centerNodeType=tgfd.getPattern().getCenterVertexType();
            for (Vertex v: loader.getGraph().getGraph().vertexSet()) {
                if(v.getTypes().contains(centerNodeType))
                {
                    jobletID++;
                    DataVertex dataVertex=(DataVertex) v;
                    Joblet joblet=new Joblet(jobletID,dataVertex,tgfd,tgfd.getPattern().getDiameter(),0);
                    Graph<Vertex, RelationshipEdge> subgraph = loader.getGraph().getSubGraphWithinDiameter(dataVertex, tgfd.getPattern().getDiameter());
                    joblet.setSubgraph(subgraph);
                    jobletsByID.put(jobletID,joblet);
                }
            }
        }
        return jobletsByID;
    }

    public static void runTheFirstSnapshot(GraphLoader loader, List<TGFD> tgfds, HashMap<Integer, Joblet> joblets)
    {
        LocalDate currentSnapshotDate= Config.getTimestamps().get(1);

        //Create the match collection for all the TGFDs in the list
        for (TGFD tgfd:tgfds) {
            matchCollectionHashMap.put(tgfd.getName(),new MatchCollection(tgfd.getPattern(),tgfd.getDependency(),tgfd.getDelta().getGranularity()));
        }

        // Now, we need to find the matches for the first snapshot.
        System.out.println("Retrieving matches for all the joblets.");
        VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();

        for (Joblet joblet:joblets.values()) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> results= VF2.execute(joblet.getSubgraph(), joblet.getTGFD().getPattern(),false);
            matchCollectionHashMap.get(joblet.getTGFD().getName()).addMatches(currentSnapshotDate,results);
        }
    }

    public void runTheNextTimestamp(GraphLoader loader, List<TGFD> tgfds, HashMap<Integer, Joblet> joblets, List<Change> changes, int superstep)
    {
        //Load the change files
        System.out.println("===========Snapshot "+superstep+" (" + Config.getTimestamps().get(superstep) + ")===========");

        long startTime=System.currentTimeMillis();
        LocalDate currentSnapshotDate= Config.getTimestamps().get(superstep);
        System.out.println("Total number of changes: " + changes.size());

        // Now, we need to find the matches for each snapshot.
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
                if(joblets.containsKey(jobletID))
                {
                    IncUpdates incUpdatesOnDBpedia=new IncUpdates(joblets.get(jobletID).getSubgraph(),tgfds);
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
    }

}

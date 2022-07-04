

import Infra.*;

import java.io.FileNotFoundException;
import java.util.*;

public class testLocalJoblet {

    public static HashMap <String, MatchCollection> matchCollectionHashMap;

    public static void main(String []args) throws FileNotFoundException
    {
        /*
        Config.parse(args[0]);

        System.out.println(Arrays.toString(Config.getFirstTypesFilePath().toArray()) + " *** " + Arrays.toString(Config.getFirstDataFilePath().toArray()));
        System.out.println(Config.getDiffFilesPath().keySet() + " *** " + Config.getDiffFilesPath().values());

        matchCollectionHashMap=new HashMap <>();

        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        List<TGFD> allTGFDs=generator.getTGFDs();

        DBPediaLoader dbpedia = new DBPediaLoader(allTGFDs, Config.getFirstTypesFilePath(), Config.getFirstDataFilePath());

        HashMap<Integer, Joblet> joblets=defineJoblets(dbpedia,allTGFDs);
         */
    }

    /*
    public static void addNewVertices(List<Change> changes, GraphLoader loader, List<TGFD> tgfds)
    {
        IncUpdates incUpdatesOnDBpedia = new IncUpdates(loader.getGraph(), tgfds);
        incUpdatesOnDBpedia.AddNewVertices(changes);
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
        // Now, we need to find the matches for the first snapshot.
        System.out.println("Retrieving matches for all the joblets.");
        VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();

        ArrayList<HashSet<ConstantLiteral>> matches = new ArrayList<>();


        for (Joblet joblet:joblets.values()) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> results= VF2.execute(joblet.getSubgraph(), joblet.getTGFD().getPattern(),false);
            if (results.isomorphismExists()) {
                extractMatches(results.getMappings(), matches, patternTreeNode);
            }
        }
    }

    public void runTheNextTimestamp(GraphLoader loader, List<TGFD> tgfds, HashMap<Integer, Joblet> joblets, List<HashMap<Integer,HashSet<Change>>> changes, int superstep)
    {


        List<Change> allChangesAsList=new ArrayList<>();
        for (HashMap<Integer,HashSet<Change>> changesByFile:changes) {
            for (HashSet<Change> c:changesByFile.values()) {
                allChangesAsList.addAll(c);
            }
        }
        System.out.println("Total number of changes: " + allChangesAsList.size());

        HashMap<String, TGFD> tgfdsByName = new HashMap<>();
        for (TGFD tgfd : tgfds) {
            tgfdsByName.put(tgfd.getName(), tgfd);
        }

        ArrayList<HashSet<ConstantLiteral>> newMatches = new ArrayList<>();
        ArrayList<HashSet<ConstantLiteral>> removedMatches = new ArrayList<>();
        int numOfNewMatchesFoundInSnapshot = 0;

        for (HashMap<Integer,HashSet<Change>> changesByFile:changes) {
            for (int changeID : changesByFile.keySet()) {
                Change change = changesByFile.get(changeID).iterator().next();
                for (int jobletID : change.getJobletIDs()) {
                    if (joblets.containsKey(jobletID)) {
                        IncUpdates incUpdatesOnDBpedia=new IncUpdates(joblets.get(jobletID).getSubgraph(),tgfds);
                        HashMap<String, IncrementalChange> incrementalChangeHashMap = incUpdatesOnDBpedia.updateGraphByGroupOfChanges(changesByFile.get(changeID), tgfdsByName);
                        if (incrementalChangeHashMap == null)
                            continue;
                        for (String tgfdName : incrementalChangeHashMap.keySet()) {
                            for (GraphMapping<Vertex, RelationshipEdge> mapping : incrementalChangeHashMap.get(tgfdName).getNewMatches().values()) {
                                numOfNewMatchesFoundInSnapshot++;
                                HashSet<ConstantLiteral> match = new HashSet<>();
                                extractMatch(mapping, patternTreeNode, match);
                                if (match.size() == 0) continue;
                                newMatches.add(match);
                            }

                            for (GraphMapping<Vertex, RelationshipEdge> mapping : incrementalChangeHashMap.get(tgfdName).getRemovedMatches().values()) {
                                HashSet<ConstantLiteral> match = new HashSet<>();
                                extractMatch(mapping, patternTreeNode, match);
                                if (match.size() == 0) continue;
                                removedMatches.add(match);
                            }
                        }
                    }
                }
            }
        }
    }
     */

}

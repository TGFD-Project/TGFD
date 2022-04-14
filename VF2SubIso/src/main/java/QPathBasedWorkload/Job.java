package QPathBasedWorkload;

import Infra.*;
import ChangeExploration.AttributeChange;
import ChangeExploration.Change;
import ChangeExploration.ChangeType;
import ChangeExploration.EdgeChange;
import org.jgrapht.Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Job {

    private int jobID;
    private int diameter;
    private DataVertex centerNode;
    private Query query;
    private int fragmentID;
    private ArrayList<RelationshipEdge> edges;
    private VF2DataGraph subgraph;
    private int matchID=0;
    private int tripleIDCount=0;

    private HashMap<String, VertexMapping> latestMappingSignatures=new HashMap<>();
    
    private HashMap<Integer,HashMap<Integer,QPathMatch>> localMatches;
    private HashMap<Integer,QPathMatch> localQueryPathMatches;

    public Job(int jobID, DataVertex centerNode, Query query, int diameter, int fragmentID)
    {
        this.jobID =jobID;
        this.diameter=diameter;
        this.centerNode=centerNode;
        this.query=query;
        this.fragmentID=fragmentID;
        this.localMatches=new HashMap<>();
        this.localQueryPathMatches=new HashMap<>();
    }

    public void setEdges(ArrayList<RelationshipEdge> edges) {
        this.edges = edges;
        edges.forEach(edge -> {
            edge.getSource().addJobletID(jobID);
            edge.getTarget().addJobletID(jobID);
        });
    }

    public void setSubgraph(Graph<Vertex, RelationshipEdge> inducedGraph) {
        this.subgraph=new VF2DataGraph(inducedGraph);
        this.subgraph.getGraph().vertexSet().forEach(vertex -> vertex.addJobletID(jobID));
    }

    public void runTheFirstSnapshot()
    {
        for (int qPathID=0;qPathID<query.getQueryPaths().size();qPathID++) {
            findMatchForAQPath(qPathID);
        }
    }

    private void findMatchForAQPath(int qPathID)
    {
        HashMap<Integer, HashSet<Triple>> matchedTriples=new HashMap<>();
        QueryPath path=query.getQueryPaths().get(qPathID);
        for (int j=0;j<path.getTriples().size();j++) {
            matchedTriples.put(j, new HashSet<>());
            Triple patternTriple = path.getTriples().get(j);
            if (j == 0) {
                for (Vertex v : subgraph.getGraph().vertexSet()) {
                    if (v.getTypes().containsAll(patternTriple.getSrc().getTypes())) {
                        for (RelationshipEdge edge : subgraph.getGraph().outgoingEdgesOf(v)) {
                            if (edge.getLabel().equals(patternTriple.getEdge()) && edge.getTarget().getTypes().containsAll(patternTriple.getDst().getTypes())) {
                                Triple newMatchedTriple = new Triple(edge.getSource(), edge.getTarget(), edge.getLabel()
                                        , -1, tripleIDCount++);
                                newMatchedTriple.getUnSatSRC(edge.getSource());
                                newMatchedTriple.getUnSatDST(edge.getTarget());
                                matchedTriples.get(j).add(newMatchedTriple);
                            }
                        }
                    }
                }
            }
            else
            {
                for (Triple dataTriple : matchedTriples.get(j - 1)) {
                    if (dataTriple.getDst().getTypes().containsAll(patternTriple.getSrc().getTypes())) {
                        for (RelationshipEdge edge : subgraph.getGraph().outgoingEdgesOf(dataTriple.getDst())) {
                            if (edge.getLabel().equals(patternTriple.getEdge()) && edge.getTarget().getTypes().containsAll(patternTriple.getDst().getTypes())) {
                                Triple newMatchedTriple = new Triple(edge.getSource(), edge.getTarget(), edge.getLabel()
                                        , dataTriple.getTripleID(), tripleIDCount++);
                                newMatchedTriple.getUnSatSRC(edge.getSource());
                                newMatchedTriple.getUnSatDST(edge.getTarget());
                                matchedTriples.get(j).add(newMatchedTriple);
                            }
                        }
                    }
                }
            }
        }

        HashMap<Integer, ArrayList<ArrayList<Triple>>> tempIndexedTriples;
        tempIndexedTriples=new HashMap<>();
        localMatches.put(qPathID,new HashMap<>());
        for (Triple firstTriple: matchedTriples.get(0)) {
            ArrayList<Triple> tempArr=new ArrayList<>();
            tempArr.add(firstTriple);
            tempIndexedTriples.put(firstTriple.getTripleID(),new ArrayList<>());
            tempIndexedTriples.get(firstTriple.getTripleID()).add(tempArr);
        }
        for (int j=1;j<path.getTriples().size();j++)
        {
            for (Triple nextTriple: matchedTriples.get(j)) {
                if(tempIndexedTriples.containsKey(nextTriple.getPrecTripleID()))
                {
                    tempIndexedTriples.put(nextTriple.getTripleID(),new ArrayList<>());
                    for (ArrayList<Triple> prevArrayList:tempIndexedTriples.get(nextTriple.getPrecTripleID())) {
                        ArrayList<Triple> tempArr=new ArrayList<>();
                        for (int count=0;count<j;count++)
                        {
                            tempArr.add(prevArrayList.get(count));
                        }
                        tempIndexedTriples.get(nextTriple.getTripleID()).add(tempArr);
                    }
                }
            }
        }
        for (Triple lastTriple: matchedTriples.get(path.getTriples().size()-1)) {
            if (tempIndexedTriples.containsKey(lastTriple.getTripleID())) {
                for (ArrayList<Triple> arr : tempIndexedTriples.get(lastTriple.getTripleID())) {
                    QPathMatch match = new QPathMatch(++matchID, arr,qPathID);
                    localMatches.get(qPathID).put(matchID, match);
                    localQueryPathMatches.put(matchID,match);
                }
            }
        }
    }

    private void deleteMatchesForAQPathByDeletingAnEdge(int qPathID, DataVertex v1, DataVertex v2, String edgeLabel)
    {
        HashSet<Integer> toBeDeleted=new HashSet<>();
        for (QPathMatch qPathMatch:localMatches.get(qPathID).values()) {
            for (Triple triple:qPathMatch.getMatchesInTriple()) {
                if(
                        ((DataVertex)triple.getSrc()).getVertexURI().equals(v1.getVertexURI())
                        && ((DataVertex)triple.getDst()).getVertexURI().equals(v2.getVertexURI())
                        && triple.getEdge().equals(edgeLabel)
                )
                {
                    toBeDeleted.add(qPathMatch.getMatchID());
                    break;
                }
            }
        }
        for (int qPathMatchID: toBeDeleted) {
            localMatches.get(qPathID).remove(qPathMatchID);
            localQueryPathMatches.remove(qPathMatchID);
        }
    }

    private void updateOrDeleteAttributeForAQPath(int qPathID,DataVertex v1)
    {
        for (QPathMatch qPathMatch:localMatches.get(qPathID).values()) {
            for (Triple triple:qPathMatch.getMatchesInTriple()) {
                if(((DataVertex)triple.getSrc()).getVertexURI().equals(v1.getVertexURI()))
                    triple.getUnSatSRC(v1);
                else if (((DataVertex)triple.getDst()).getVertexURI().equals(v1.getVertexURI()))
                    triple.getUnSatDST(v1);
            }
        }
    }

    public void applyChangeForTheNextSnapshot(Change change)
    {
        if(change instanceof EdgeChange)
        {
            EdgeChange edgeChange=(EdgeChange) change;
            DataVertex v1= (DataVertex) this.subgraph.getNode(edgeChange.getSrc());
            DataVertex v2= (DataVertex) this.subgraph.getNode(edgeChange.getDst());
            if(v1==null || v2==null)
                return;

            if(edgeChange.getTypeOfChange()== ChangeType.insertEdge)
            {
                if(!subgraph.getGraph().containsVertex(v2))
                    subgraph.getGraph().addVertex(v2);
                subgraph.getGraph().addEdge(v1,v2,new RelationshipEdge(edgeChange.getLabel()));
                ArrayList<Integer> qPathIDs=findRelevantQPath(v1,v2,edgeChange.getLabel());
                for (int qPathID:qPathIDs)
                    findMatchForAQPath(qPathID);
            }
            else if(edgeChange.getTypeOfChange()== ChangeType.deleteEdge)
            {
                // Now, perform the change and remove the edge from the subgraph
                for (RelationshipEdge e:subgraph.getGraph().outgoingEdgesOf(v1)) {
                    DataVertex target=(DataVertex) e.getTarget();
                    if(target.getVertexURI().equals(v2.getVertexURI()) && edgeChange.getLabel().equals(e.getLabel()))
                    {
                        subgraph.getGraph().removeEdge(e);
                        break;
                    }
                }
                ArrayList<Integer> qPathIDs=findRelevantQPath(v1,v2,edgeChange.getLabel());
                for (int qPathID:qPathIDs)
                    deleteMatchesForAQPathByDeletingAnEdge(qPathID,v1,v2,edgeChange.getLabel());
            }
            else
                throw new IllegalArgumentException("The change is instance of EdgeChange, but type of change is: " + edgeChange.getTypeOfChange());
        }
        else if(change instanceof AttributeChange)
        {
            AttributeChange attributeChange=(AttributeChange) change;
            DataVertex v1=(DataVertex) this.subgraph.getNode(attributeChange.getUri());
            if(v1==null)
                return;
            if(attributeChange.getTypeOfChange()==ChangeType.changeAttr || attributeChange.getTypeOfChange()==ChangeType.insertAttr)
            {
                v1.setOrAddAttribute(attributeChange.getAttribute());
                ArrayList<Integer> qPathIDs=findRelevantQPath(v1,attributeChange.getAttribute());
                for (int qPathID:qPathIDs)
                    updateOrDeleteAttributeForAQPath(qPathID,v1);
            }
            else if(attributeChange.getTypeOfChange()==ChangeType.deleteAttr)
            {
                v1.deleteAttribute(attributeChange.getAttribute());
                ArrayList<Integer> qPathIDs=findRelevantQPath(v1,attributeChange.getAttribute());
                for (int qPathID:qPathIDs)
                    updateOrDeleteAttributeForAQPath(qPathID,v1);
            }
            else
                throw new IllegalArgumentException("The change is instance of AttributeChange, but type of change is: " + attributeChange.getTypeOfChange());
        }
    }

    public HashSet<VertexMapping> findMatchMapping()
    {
        HashSet<VertexMapping> mappings=new HashSet<>();
        HashMap<Integer,HashSet<ArrayList<Integer>>> mappingIndices=new HashMap<>();
        mappingIndices.put(0,new HashSet<>());
        for (QPathMatch match:localMatches.get(0).values()) {
            ArrayList<Integer> temp=new ArrayList<>();
            temp.add(match.getMatchID());
            mappingIndices.get(0).add(temp);
        }
        for (int qPathID=1;qPathID<query.getQueryPaths().size();qPathID++) {
            mappingIndices.put(qPathID,new HashSet<>());
            for (ArrayList<Integer> prevMatches:mappingIndices.get(qPathID-1)) {
                for (QPathMatch match:localMatches.get(qPathID).values()) {
                    ArrayList<Integer> temp = new ArrayList<>(prevMatches);
                    temp.add(match.getMatchID());
                    mappingIndices.get(qPathID).add(temp);
                }
            }
        }

        for (ArrayList<Integer> matches:mappingIndices.get(query.getQueryPaths().size()-1))
        {
            VertexMapping mapping = new VertexMapping();
            for (int qPathMatchID:matches) {
                QPathMatch qPathMatch=localQueryPathMatches.get(qPathMatchID);
                for (int i=0;i<qPathMatch.getMatchesInTriple().size();i++)
                {
                    Triple dataTriple=qPathMatch.getMatchesInTriple().get(i);
                    PatternVertex correspondingPatternVertex =(PatternVertex) query.getQueryPaths().get(qPathMatch.getqPathID()).getTriples().get(i).getSrc();
                    mapping.addMapping(dataTriple.getSrc(),correspondingPatternVertex);
                }
            }
            mappings.add(mapping);
        }
        latestMappingSignatures.clear();
        for (VertexMapping vertexMapping:mappings) {
            latestMappingSignatures.put(Match.signatureFromPattern(getQuery().getTGFD().getPattern(),vertexMapping),vertexMapping);
        }
        return mappings;
    }

    public VF2DataGraph getSubgraph() {
        return subgraph;
    }

    public ArrayList<RelationshipEdge> getEdges() {
        return edges;
    }

    public int getDiameter() {
        return diameter;
    }

    public DataVertex getCenterNode() {
        return centerNode;
    }

    public Query getQuery() {
        return query;
    }

    public int getJobID() {
        return jobID;
    }

    public int getFragmentID() {
        return fragmentID;
    }

    public double getSize()
    {
        if(edges!=null)
            return Math.pow(edges.size(),query.getTGFD().getPattern().getSize());
        else
            return 0;
    }

    public HashMap<String, VertexMapping> getLatestMappingSignatures() {
        return latestMappingSignatures;
    }

    private ArrayList<Integer> findRelevantQPath(DataVertex v1, DataVertex v2, String edgeLabel)
    {
        ArrayList<Integer> qPathIndices=new ArrayList<>();
        for (int i=0;i<query.getQueryPaths().size();i++) {
            QueryPath path = query.getQueryPaths().get(i);
            for (int j = 0; j < path.getTriples().size(); j++) {
                Triple patternTriple = path.getTriples().get(j);
                if(patternTriple.isTopologicalMappedToSRC(v1) && patternTriple.isTopologicalMappedToDST(v2) &&
                patternTriple.getEdge().equals(edgeLabel))
                {
                    qPathIndices.add(i);
                    break;
                }
            }
        }
        return qPathIndices;
    }

    private ArrayList<Integer> findRelevantQPath(DataVertex v1, Attribute attribute)
    {
        ArrayList<Integer> qPathIndices=new ArrayList<>();
        for (int i=0;i<query.getQueryPaths().size();i++) {
            QueryPath path = query.getQueryPaths().get(i);
            for (int j = 0; j < path.getTriples().size(); j++) {
                Triple patternTriple = path.getTriples().get(j);
                if((patternTriple.isTopologicalMappedToSRC(v1) && patternTriple.hasAttrSRC(attribute)) ||
                        (patternTriple.isTopologicalMappedToDST(v1) && patternTriple.hasAttrDST(attribute)))
                {
                    qPathIndices.add(i);
                    break;
                }
            }
        }
        return qPathIndices;
    }
}

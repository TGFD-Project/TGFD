package QPath;

import Infra.*;
import org.jgrapht.Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Job {

    private int id;
    private int diameter;
    private DataVertex centerNode;
    private Query query;
    private int fragmentID;
    private ArrayList<RelationshipEdge> edges;
    private VF2DataGraph subgraph;
    private ArrayList<boolean []>  matches=new ArrayList<>();
    private ArrayList<HashMap<Integer, HashMap<Vertex, Literal>>>  unSat=new ArrayList<>();

    public Job(int id, DataVertex centerNode, Query query, int diameter, int fragmentID)
    {
        this.id=id;
        this.diameter=diameter;
        this.centerNode=centerNode;
        this.query=query;
        this.fragmentID=fragmentID;
    }

    public void setEdges(ArrayList<RelationshipEdge> edges) {
        this.edges = edges;
        edges.forEach(edge -> {
            edge.getSource().addJobletID(id);
            edge.getTarget().addJobletID(id);
        });
    }

    public void setSubgraph(Graph<Vertex, RelationshipEdge> inducedGraph) {
        this.subgraph=new VF2DataGraph(inducedGraph);
        this.subgraph.getGraph().vertexSet().forEach(vertex -> vertex.addJobletID(id));
    }

    public void findMatchesForTheFirstSnapshot()
    {
        for (int i=0;i<query.getQueryPaths().size();i++)
        {
            QueryPath path=query.getQueryPaths().get(i);
            HashMap<Integer, HashSet<Triple>> matchedTriples=new HashMap<>();
            for (int j=0;j<path.getTriples().size();j++)
            {
                matchedTriples.put(j,new HashSet<>());
                Triple patternTriple=path.getTriples().get(j);
                if(j==0)
                {
                    for (Vertex v:subgraph.getGraph().vertexSet()) {
                        if(v.getTypes().containsAll(patternTriple.getSrc().getTypes()))
                        {
                            for (RelationshipEdge edge:subgraph.getGraph().outgoingEdgesOf(v)) {
                                if(edge.getLabel().equals(patternTriple.getEdge()) && edge.getTarget().getTypes().containsAll(patternTriple.getDst().getTypes()))
                                {
                                    Triple triple = new Triple(edge.getSource(),edge.getTarget(),edge.getLabel());
                                    triple.getUnSatSRC(edge.getSource());
                                    triple.getUnSatDST(edge.getTarget());
                                    matchedTriples.get(j).add(triple);
                                }
                            }
                        }
                    }
                }
                else
                {
                    for (Triple dataTriple:matchedTriples.get(j-1)) {
                        if(dataTriple.getDst().getTypes().containsAll(patternTriple.getSrc().getTypes()))
                        {
                            for (RelationshipEdge edge:subgraph.getGraph().outgoingEdgesOf(dataTriple.getDst())) {
                                if(edge.getLabel().equals(patternTriple.getEdge()) && edge.getTarget().getTypes().containsAll(patternTriple.getDst().getTypes()))
                                {
                                    Triple triple = new Triple(edge.getSource(),edge.getTarget(),edge.getLabel());
                                    triple.getUnSatSRC(edge.getSource());
                                    triple.getUnSatDST(edge.getTarget());
                                    matchedTriples.get(j).add(triple);
                                }
                            }
                        }
                    }
                }
            }
        }
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

    public Query getًQuery() {
        return query;
    }

    public int getId() {
        return id;
    }

    public int getFragmentID() {
        return fragmentID;
    }

    public double getSize()
    {
        if(edges!=null)
            return Math.pow(edges.size(),query.getTgfd().getPattern().getSize());
        else
            return 0;
    }

}

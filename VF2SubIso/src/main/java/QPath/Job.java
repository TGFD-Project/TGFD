package QPath;

import Infra.*;
import org.jgrapht.Graph;

import java.util.ArrayList;

public class Job {

    private int id;
    private int diameter;
    private DataVertex centerNode;
    private Query query;
    private int fragmentID;
    private ArrayList<RelationshipEdge> edges;
    private VF2DataGraph subgraph;

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

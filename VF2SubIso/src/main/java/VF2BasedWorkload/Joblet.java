package VF2BasedWorkload;

import ICs.TGFD;
import Infra.*;
import org.jgrapht.Graph;

import java.util.ArrayList;

public class Joblet {

    private int id;
    private int diameter;
    private DataVertex centerNode; //a ->b //10,000 joblets -> 10,000 instances of a// 50 of them had a match
    private TGFD tgfd;             //a->b a->c //10,000 joblets -> 50 joblets and check if there is any match of the new pattern
    private int fragmentID;        //a->b and b->c -> start off with 50 joblets, then extract the subgraph within diameter 2 and then do the matching
    private ArrayList<RelationshipEdge> edges;
    private VF2DataGraph subgraph;

    public Joblet(int id, DataVertex centerNode, TGFD tgfd, int diameter, int fragmentID)
    {
        this.id=id;
        this.diameter=diameter;
        this.centerNode=centerNode;
        this.tgfd=tgfd;
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

    public TGFD getTGFD() {
        return tgfd;
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
            return Math.pow(edges.size(),tgfd.getPattern().getSize());
        else
            return 0;
    }

}

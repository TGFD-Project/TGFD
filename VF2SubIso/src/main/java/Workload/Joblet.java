package Workload;

import Infra.DataVertex;
import Infra.RelationshipEdge;
import Infra.TGFD;

import java.util.ArrayList;

public class Joblet {

    private int id;
    private int diameter;
    private DataVertex centerNode;
    private TGFD tgfd;
    private int fragmentID;
    private ArrayList<RelationshipEdge> edges;

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

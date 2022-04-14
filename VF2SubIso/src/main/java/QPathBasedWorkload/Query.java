package QPathBasedWorkload;

import Infra.RelationshipEdge;
import ICs.TGFD;
import Infra.Vertex;

import java.util.*;

public class Query {

    private TGFD tgfd;
    private ArrayList<QueryPath> queryPaths;

    public Query(TGFD tgfd)
    {
        queryPaths=new ArrayList<>();
        this.tgfd=tgfd;
        ArrayList<ArrayList<Triple>> paths=generateQueryPaths();
        for (ArrayList<Triple> path:paths) {
            queryPaths.add(new QueryPath(path));
        }
    }

    private ArrayList<ArrayList<Triple>> generateQueryPaths()
    {
        String centerVertexType=tgfd.getPattern().getCenterVertexType();
        Vertex centerNode=null;
        for (Vertex v:tgfd.getPattern().getPattern().vertexSet()) {
            if(v.getTypes().contains(centerVertexType)) {
                centerNode = v;
                break;
            }
        }
        HashSet<RelationshipEdge> visited=new HashSet<>();
        HashSet<Vertex> notLeaf=new HashSet<>();
        HashMap<Vertex, ArrayList<Triple>> toTheRoot=new HashMap<>();
        ArrayList<ArrayList<Triple>> paths=new ArrayList<>();
        Stack<Vertex> stack=new Stack<>();
        stack.add(centerNode);
        toTheRoot.put(centerNode,new ArrayList<>());
        ArrayList<Triple> path=new ArrayList<>();
        HashMap<Vertex, ArrayList<Vertex>> edgeTo=new HashMap<>();
        while (!stack.isEmpty())
        {
            Vertex v=stack.pop();
            boolean done=false;
            for (RelationshipEdge edge:tgfd.getPattern().getPattern().outgoingEdgesOf(v)) {
                if(!visited.contains(edge))
                {
                    visited.add(edge);
                    ArrayList<Triple> soFar= (ArrayList<Triple>) toTheRoot.get(v).clone();
                    Triple triple=new Triple(v,edge.getTarget(),edge.getLabel());
                    soFar.add(triple);
                    toTheRoot.put(edge.getTarget(),soFar);
                    //if(!edgeTo.containsKey(edge.getTarget()))
                    //    edgeTo.put(edge.getTarget(),new ArrayList<>());
                    //edgeTo.get(edge.getTarget()).add(v);
                    stack.add(v);
                    notLeaf.add(v);
                    stack.add(edge.getTarget());
                    done=true;
                    break;
                }
            }
            if(!done && !notLeaf.contains(v))
            {
                paths.add(toTheRoot.get(v));
                //leaves.add(v);
            }
        }
        return paths;
    }

    public TGFD getTGFD() {
        return tgfd;
    }

    public ArrayList<QueryPath> getQueryPaths() {
        return queryPaths;
    }
}

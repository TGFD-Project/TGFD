package QPath;

import Infra.RelationshipEdge;
import Infra.VF2PatternGraph;
import Infra.Vertex;
import changeExploration.EdgeChange;

import java.util.*;

public class Query {

    private VF2PatternGraph patternGraph;
    private ArrayList<QueryPath> queryPaths;

    public Query(VF2PatternGraph patternGraph)
    {
        this.patternGraph=patternGraph;
    }






    private void generateQueryPaths()
    {
        String centerVertexType=patternGraph.getCenterVertexType();
        Vertex centerNode=null;
        for (Vertex v:patternGraph.getPattern().vertexSet()) {
            if(v.getTypes().contains(centerVertexType)) {
                centerNode = v;
                break;
            }
        }
        HashSet<RelationshipEdge> visited=new HashSet<>();
        HashSet<Vertex> leaves=new HashSet<>();
        ArrayList<ArrayList<Triple>> paths=new ArrayList<>();
        Stack<Vertex> stack=new Stack<>();
        stack.add(centerNode);
        ArrayList<Triple> path=new ArrayList<>();
        HashMap<Vertex, ArrayList<Vertex>> edgeTo=new HashMap<>();
        while (!stack.isEmpty())
        {
            Vertex v=stack.pop();
            boolean done=false;
            for (RelationshipEdge edge:patternGraph.getPattern().outgoingEdgesOf(v)) {
                if(!visited.contains(edge))
                {
                    visited.add(edge);
                    if(!edgeTo.containsKey(edge.getTarget()))
                        edgeTo.put(edge.getTarget(),new ArrayList<>());
                    edgeTo.get(edge.getTarget()).add(v);
                    stack.add(edge.getTarget());
                    done=true;
                    break;
                }
            }
            if(!done)
            {
                leaves.add(v);
            }
        }

        for (Vertex leaf:leaves) {
            path=new ArrayList<>();
            while (true)
            {

            }
        }
    }

}

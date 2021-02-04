package infra;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.HashMap;

public class VF2Graph {

    private Graph<vertex, relationshipEdge> graph;

    private HashMap<Integer,vertex> nodeMap;

    public VF2Graph()
    {
        graph= new DefaultDirectedGraph<>(relationshipEdge.class);
        nodeMap=new HashMap<Integer, vertex>();
    }

    public Graph<vertex, relationshipEdge> getGraph() {
        return graph;
    }

    public void addVertex(vertex v)
    {
        graph.addVertex(v);
        //if(!nodeMap.containsKey(v.))
    }

    public void addEdge(vertex v1, vertex v2, relationshipEdge edge)
    {
        graph.addEdge(v1,v2,edge);
    }
}

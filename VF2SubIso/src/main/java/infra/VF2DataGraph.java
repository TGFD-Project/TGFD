package infra;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.HashMap;

public class VF2DataGraph {

    private Graph<Vertex, relationshipEdge> graph = new DefaultDirectedGraph<>(relationshipEdge.class);

    private HashMap<String, Vertex> nodeMap;

    public VF2DataGraph()
    {
        nodeMap= new HashMap<>();
    }

    public Graph<Vertex, relationshipEdge> getGraph() {
        return graph;
    }

    public void addVertex(dataVertex v)
    {
        if(!nodeMap.containsKey(v.getVertexURI()))
        {
            graph.addVertex(v);
            nodeMap.put(v.getVertexURI(),v);
        }
        else
        {
            System.out.println("Node already existed, Vertex URI: " + v.getVertexURI());
        }
    }

    public Vertex getNode(String vertexURI)
    {
        return nodeMap.getOrDefault(vertexURI, null);
    }

    public void addEdge(dataVertex v1, dataVertex v2, relationshipEdge edge)
    {
        graph.addEdge(v1,v2,edge);
    }

    public int getSize()
    {
        return nodeMap.size();
    }
}

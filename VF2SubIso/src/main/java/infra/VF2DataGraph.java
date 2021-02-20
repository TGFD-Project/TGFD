package infra;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.HashMap;

public class VF2DataGraph {

    private Graph<Vertex, RelationshipEdge> graph = new DefaultDirectedGraph<>(RelationshipEdge.class);

    private HashMap<String, Vertex> nodeMap;

    public VF2DataGraph()
    {
        nodeMap= new HashMap<>();
    }

    public Graph<Vertex, RelationshipEdge> getGraph() {
        return graph;
    }

    public void addVertex(DataVertex v)
    {
        if(!nodeMap.containsKey(v.getVertexURI().hashCode()))
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

    public void addEdge(DataVertex v1, DataVertex v2, RelationshipEdge edge)
    {
        graph.addEdge(v1,v2,edge);
    }

    public int getSize()
    {
        return nodeMap.size();
    }
}

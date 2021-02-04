package infra;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import util.myExceptions;

import java.util.HashMap;

public class VF2DataGraph {

    private Graph<vertex, relationshipEdge> graph;

    private HashMap<Integer,vertex> nodeMap;

    public VF2DataGraph()
    {
        graph= new DefaultDirectedGraph<>(relationshipEdge.class);
        nodeMap= new HashMap<>();
    }

    public Graph<vertex, relationshipEdge> getGraph() {
        return graph;
    }

    public void addVertex(dataVertex v) throws myExceptions.NodeAlreadyExistsException
    {

        if(!nodeMap.containsKey(v.getHashValue()))
        {
            graph.addVertex(v);
            nodeMap.put(v.getHashValue(),v);
        }
        else
        {
            throw new myExceptions.NodeAlreadyExistsException("Vertex URI: " + v.getVertexURI());
        }

    }

    public void addEdge(dataVertex v1, dataVertex v2, relationshipEdge edge)
    {
        graph.addEdge(v1,v2,edge);
    }
}

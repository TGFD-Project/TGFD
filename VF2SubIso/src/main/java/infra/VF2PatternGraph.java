package infra;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.HashMap;

public class VF2PatternGraph {

    private Graph<vertex, relationshipEdge> graph;

    private HashMap<Integer,vertex> nodeMap;

    public VF2PatternGraph()
    {
        graph= new DefaultDirectedGraph<>(relationshipEdge.class);
        nodeMap= new HashMap<>();
    }

    public Graph<vertex, relationshipEdge> getGraph() {
        return graph;
    }

    public void addVertex(patternVertex v)
    {
        graph.addVertex(v);
    }

    public void addEdge(patternVertex v1, patternVertex v2, relationshipEdge edge)
    {
        graph.addEdge(v1,v2,edge);
    }

}

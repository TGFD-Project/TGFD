package infra;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

public class VF2PatternGraph {

    private Graph<Vertex, RelationshipEdge> graph;

    private int diameter;

    public VF2PatternGraph(int diameter)
    {
        graph= new DefaultDirectedGraph<>(RelationshipEdge.class);
        this.diameter=diameter;
    }

    public VF2PatternGraph()
    {
        graph= new DefaultDirectedGraph<>(RelationshipEdge.class);
    }

    public Graph<Vertex, RelationshipEdge> getGraph() {
        return graph;
    }

    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }

    public int getDiameter() {
        return diameter;
    }

    public void addVertex(PatternVertex v)
    {
        graph.addVertex(v);
    }

    public void addEdge(PatternVertex v1, PatternVertex v2, RelationshipEdge edge)
    {
        graph.addEdge(v1,v2,edge);
    }

    @Override
    public String toString() {
        String res="VF2PatternGraph{";
        for (RelationshipEdge edge:graph.edgeSet()) {
            res+=edge.toString();
        }
        res+='}';
        return res;
    }

}

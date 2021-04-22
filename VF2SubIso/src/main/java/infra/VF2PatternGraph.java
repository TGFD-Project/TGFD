package infra;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.HashMap;
import java.util.LinkedList;

public class VF2PatternGraph {

    private Graph<Vertex, RelationshipEdge> pattern;

    private int diameter;

    private String centerVertexType="";

    public VF2PatternGraph(int diameter)
    {
        pattern = new DefaultDirectedGraph<>(RelationshipEdge.class);
        this.diameter=diameter;
    }

    public VF2PatternGraph(int diameter,String centerVertexType)
    {
        pattern = new DefaultDirectedGraph<>(RelationshipEdge.class);
        this.diameter=diameter;
        this.centerVertexType=centerVertexType;
    }

    public VF2PatternGraph()
    {
        pattern = new DefaultDirectedGraph<>(RelationshipEdge.class);
    }

    public Graph<Vertex, RelationshipEdge> getPattern() {
        return pattern;
    }

    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }

    public int getDiameter() {
        return diameter;
    }

    public void addVertex(PatternVertex v)
    {
        pattern.addVertex(v);
    }

    public void addEdge(PatternVertex v1, PatternVertex v2, RelationshipEdge edge)
    {
        pattern.addEdge(v1,v2,edge);
    }

    public String getCenterVertexType()
    {
        if(!centerVertexType.equals(""))
            return centerVertexType;
        else
        {
            findCenterNode();
            return centerVertexType;
        }
    }

    public int getSize()
    {
        return this.pattern.edgeSet().size();
    }

    private void findCenterNode()
    {
        int patternDiameter=0;
        Vertex centerNode=null;
        for (Vertex v:this.pattern.vertexSet()) {
            // Define a HashMap to store visited vertices
            HashMap <Vertex,Integer> visited=new HashMap<>();

            // Create a queue for BFS
            LinkedList <Vertex> queue = new LinkedList<>();
            int d=0;
            // Mark the current node as visited with distance 0 and then enqueue it
            visited.put(v,d);
            queue.add(v);

            //temp variables
            Vertex x,w;

            while (queue.size() != 0)
            {
                // Dequeue a vertex from queue and get its distance
                x = queue.poll();
                int distance=visited.get(x);

                // Outgoing edges
                for (RelationshipEdge edge : pattern.outgoingEdgesOf(v)) {
                    w = edge.getTarget();

                    // Check if the vertex is not visited
                    if (!visited.containsKey(w)) {
                        // Check if the vertex is within the diameter
                        if (distance + 1 < d) {
                            d = distance + 1;
                        }
                        //Enqueue the vertex and add it to the visited set
                        visited.put(w, distance + 1);
                        queue.add(w);
                    }
                }
                // Incoming edges
                for (RelationshipEdge edge : pattern.incomingEdgesOf(v)) {
                    w = edge.getSource();

                    // Check if the vertex is not visited
                    if (!visited.containsKey(w)) {
                        // Check if the vertex is within the diameter
                        if (distance + 1 < d) {
                            d = distance + 1;
                        }
                        //Enqueue the vertex and add it to the visited set
                        visited.put(w, distance + 1);
                        queue.add(w);
                    }
                }
            }
            if(d>patternDiameter)
            {
                patternDiameter=d;
                centerNode=v;
            }
        }
        if(!centerNode.getTypes().isEmpty())
            this.centerVertexType= centerNode.getTypes().iterator().next();
        else
            this.centerVertexType="NoType";
        this.diameter=patternDiameter;
    }

    @Override
    public String toString() {
        String res="VF2PatternGraph{";
        for (RelationshipEdge edge: pattern.edgeSet()) {
            res+=edge.toString();
        }
        res+='}';
        return res;
    }

}

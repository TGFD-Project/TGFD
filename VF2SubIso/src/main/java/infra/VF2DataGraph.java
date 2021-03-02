package infra;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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
        if(!nodeMap.containsKey(v.getVertexURI()))
        {
            graph.addVertex(v);
            nodeMap.put(v.getVertexURI(),v);
        }
//        else
//        {
//            System.out.println("Node already existed, Vertex URI: " + v.getVertexURI());
//        }
    }

    public Vertex getNode(String vertexURI)
    {
        return nodeMap.getOrDefault(vertexURI, null);
    }

    public void addEdge(DataVertex v1, DataVertex v2, RelationshipEdge edge)
    {
        graph.addEdge(v1,v2,edge);
    }

    public void removeEdge(DataVertex v1, DataVertex v2, RelationshipEdge edge)
    {
        for (RelationshipEdge e:graph.outgoingEdgesOf(v1)) {
            DataVertex target=(DataVertex) e.getTarget();
            if(target.getVertexURI().equals(v2.getVertexURI()) && edge.getLabel().equals(e.getLabel()))
            {
                this.graph.removeEdge(e);
                return;
            }
        }
    }

    public int getSize()
    {
        return nodeMap.size();
    }

    public Graph<Vertex, RelationshipEdge> getSubGraphByDiameter(DataVertex center, int diameter)
    {
        Graph<Vertex, RelationshipEdge> subgraph = new DefaultDirectedGraph<>(RelationshipEdge.class);

        List<Vertex> withinDiameter=new ArrayList<>();

        // Define a HashMap to store visited vertices
        HashMap<String,Integer> visited=new HashMap<>();

        // Create a queue for BFS
        LinkedList<DataVertex> queue = new LinkedList<>();

        // Mark the current node as visited with distance 0 and then enqueue it
        visited.put(center.getVertexURI(),0);
        queue.add(center);
        // Store the center as the node within the diameter
        withinDiameter.add(center);
        //temp variables
        DataVertex v,w;

        while (queue.size() != 0)
        {
            // Dequeue a vertex from queue and get its distance
            v = queue.poll();
            int distance=visited.get(v.getVertexURI());

            // Outgoing edges
            for (RelationshipEdge edge : graph.outgoingEdgesOf(v)) {
                w = (DataVertex) edge.getTarget();

                // Check if the vertex is not visited
                if (!visited.containsKey(w.getVertexURI())) {

                    // Check if the vertex is within the diameter
                    if (distance + 1 <= diameter) {

                        //Enqueue the vertex and add it to the visited set
                        visited.put(w.getVertexURI(), distance + 1);
                        queue.add(w);
                        withinDiameter.add(w);
                    }

                }
            }
            // Incoming edges
            for (RelationshipEdge edge : graph.incomingEdgesOf(v)) {
                w = (DataVertex) edge.getSource();

                // Check if the vertex is not visited
                if (!visited.containsKey(w.getVertexURI())) {

                    // Check if the vertex is within the diameter
                    if (distance + 1 <= diameter) {

                        //Enqueue the vertex and add it to the visited set
                        visited.put(w.getVertexURI(), distance + 1);
                        queue.add(w);
                        withinDiameter.add(w);
                    }

                }
            }
        }

        for (Vertex vertex:withinDiameter) {
            subgraph.addVertex(vertex);
        }

        for (Vertex source:withinDiameter) {
            for (RelationshipEdge e:graph.outgoingEdgesOf(source)) {
                DataVertex target=(DataVertex)e.getTarget();
                if(visited.containsKey(target.getVertexURI()))
                    subgraph.addEdge(e.getSource(),e.getTarget(),e);
            }
        }

        return subgraph;
    }


    public void updateGraphByAttribute(DataVertex v1, Attribute attribute)
    {
        nodeMap.get(v1.getVertexURI()).setOrAddAttribute(attribute);
    }

}

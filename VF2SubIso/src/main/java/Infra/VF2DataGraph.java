package Infra;

import ICs.TGFD;
import VF2BasedWorkload.Joblet;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.io.Serializable;
import java.util.*;

public class VF2DataGraph implements Serializable {

    private Graph<Vertex, RelationshipEdge> graph = new DefaultDirectedGraph<>(RelationshipEdge.class);

    private HashMap<String, Vertex> nodeMap;

    public VF2DataGraph()
    {
        nodeMap= new HashMap<>();
    }

    public VF2DataGraph(Graph <Vertex, RelationshipEdge> graph)
    {
        nodeMap= new HashMap<>();
        this.graph = graph;
        for (Vertex v:graph.vertexSet()) {
            DataVertex dataV=(DataVertex) v;
            if(!nodeMap.containsKey(dataV.getVertexURI())) {
                nodeMap.put(dataV.getVertexURI(), dataV);
            }
        }
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

    public HashMap<String, Vertex> getNodeMap() {
        return nodeMap;
    }

    public Graph<Vertex, RelationshipEdge> getSubGraphWithinDiameter(DataVertex center, int diameter)
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

    public Graph<Vertex, RelationshipEdge> getSubGraphWithinDiameter(DataVertex center, int diameter, TGFD tgfd)
    {
        Graph<Vertex, RelationshipEdge> subgraph = new DefaultDirectedGraph<>(RelationshipEdge.class);
        Set <String> validTypes= extractValidTypesFromTGFD(tgfd);

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
                // Check if the vertex is within the diameter
                // Check if the vertex type is a validType
                if (distance + 1 <= diameter && isValidType(validTypes,w.getTypes()) && !visited.containsKey(w.getVertexURI())) {
                    //Enqueue the vertex and add it to the visited set
                    visited.put(w.getVertexURI(), distance + 1);
                    queue.add(w);
                    withinDiameter.add(w);
                }
            }
            // Incoming edges
            for (RelationshipEdge edge : graph.incomingEdgesOf(v)) {
                w = (DataVertex) edge.getSource();
                // Check if the vertex is not visited
                // Check if the vertex is within the diameter
                // Check if the vertex type is a validType
                if (distance + 1 <= diameter && isValidType(validTypes,w.getTypes()) && !visited.containsKey(w.getVertexURI())) {
                    //Enqueue the vertex and add it to the visited set
                    visited.put(w.getVertexURI(), distance + 1);
                    queue.add(w);
                    withinDiameter.add(w);
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

    public ArrayList<RelationshipEdge> getEdgesWithinDiameter(DataVertex center, int diameter)
    {
        ArrayList<RelationshipEdge> edges = new ArrayList<>();

        // Define a HashMap to store visited vertices
        HashMap<String,Integer> visited=new HashMap<>();

        // Create a queue for BFS
        LinkedList<DataVertex> queue = new LinkedList<>();

        // Mark the current node as visited with distance 0 and then enqueue it
        visited.put(center.getVertexURI(),0);
        queue.add(center);

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
                        edges.add(edge);
                        visited.put(w.getVertexURI(), distance + 1);
                        queue.add(w);
                        //withinDiameter.add(w);
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
                        edges.add(edge);
                        //withinDiameter.add(w);
                    }

                }
            }
        }
        return edges;
    }

    public int getSubGraphSize(DataVertex center, int diameter)
    {
        int size=0;

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
        for (Vertex source : withinDiameter) {
            for (RelationshipEdge e : graph.outgoingEdgesOf(source)) {
                DataVertex target = (DataVertex) e.getTarget();
                if (visited.containsKey(target.getVertexURI()))
                    size++;
            }
        }
        return size;
    }

    public List<Vertex> getVerticesWithinDiameter(DataVertex center, int diameter)
    {
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
        return withinDiameter;
    }

    public Graph<Vertex, RelationshipEdge> getFragmentedGraph(List<Joblet> joblets)
    {
        Graph<Vertex, RelationshipEdge> fragmentedGraph = new DefaultDirectedGraph<>(RelationshipEdge.class);

        HashSet<String> allVisitedVertices=new HashSet <>();

        for (Joblet joblet : joblets) {
            DataVertex centerNode= (DataVertex) this.nodeMap.get(joblet.getCenterNode().getVertexURI());
            if(centerNode==null)
                continue;

            List<Vertex> withinDiameter=new ArrayList<>();

            // Define a HashMap to store visited vertices
            HashMap<String,Integer> visited=new HashMap<>();

            // Create a queue for BFS
            LinkedList<DataVertex> queue = new LinkedList<>();

            // Mark the current node as visited with distance 0 and then enqueue it
            visited.put(centerNode.getVertexURI(),0);
            queue.add(centerNode);
            // Store the center as the node within the diameter
            withinDiameter.add(centerNode);
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
                        if (distance + 1 <= joblet.getDiameter()) {

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
                        if (distance + 1 <= joblet.getDiameter()) {

                            //Enqueue the vertex and add it to the visited set
                            visited.put(w.getVertexURI(), distance + 1);
                            queue.add(w);
                            withinDiameter.add(w);
                        }

                    }
                }
            }
            for (Vertex vertex:withinDiameter) {
                DataVertex dataV=(DataVertex) vertex;
                if(!allVisitedVertices.contains(dataV.getVertexURI()))
                {
                    allVisitedVertices.add(dataV.getVertexURI());
                    fragmentedGraph.addVertex(vertex);
                }
            }
            for (Vertex source:withinDiameter) {
                for (RelationshipEdge e:graph.outgoingEdgesOf(source)) {
                    DataVertex target=(DataVertex)e.getTarget();
                    if(visited.containsKey(target.getVertexURI()))
                    {
                        //We need to check if that edge is already added to the fragmented graph
                        boolean exist=false;
                        for (RelationshipEdge e_f:fragmentedGraph.outgoingEdgesOf(source)) {
                            if(e_f.equals(e))
                            {
                                exist=true;
                                break;
                            }
                        }
                        if(!exist)
                            fragmentedGraph.addEdge(e.getSource(),e.getTarget(),e);
                    }
                }
            }
        }
        return fragmentedGraph;
    }

    public void updateGraphByAttribute(DataVertex v1, Attribute attribute)
    {
        nodeMap.get(v1.getVertexURI()).setOrAddAttribute(attribute);
    }

    /**
     * Extracts all the types being used in a TGFD from from X->Y dependency and the graph pattern
     * @param tgfd input TGFD
     */
    private Set <String> extractValidTypesFromTGFD(TGFD tgfd)
    {
        Set <String> validTypes=new HashSet<>();
        for (Literal x:tgfd.getDependency().getX()) {
            if(x instanceof ConstantLiteral)
                validTypes.add(((ConstantLiteral) x).getVertexType());
            else if(x instanceof VariableLiteral)
            {
                validTypes.add(((VariableLiteral) x).getVertexType_1());
                validTypes.add(((VariableLiteral) x).getVertexType_2());
            }

        }
        for (Literal x:tgfd.getDependency().getY()) {
            if(x instanceof ConstantLiteral)
                validTypes.add(((ConstantLiteral) x).getVertexType());
            else if(x instanceof VariableLiteral)
            {
                validTypes.add(((VariableLiteral) x).getVertexType_1());
                validTypes.add(((VariableLiteral) x).getVertexType_2());
            }

        }
        for (Vertex v:tgfd.getPattern().getPattern().vertexSet()) {
            if(v instanceof PatternVertex)
                validTypes.addAll(v.getTypes());
        }
        return validTypes;
    }

    private boolean isValidType(Set<String> validTypes, Set<String> givenTypes)
    {
        return givenTypes.stream().anyMatch(validTypes::contains);
    }

}

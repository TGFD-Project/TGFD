package VF2Runner;

import infra.RelationshipEdge;
import infra.VF2DataGraph;
import infra.VF2PatternGraph;
import infra.Vertex;
import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2AbstractIsomorphismInspector;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import util.Config;

import java.util.Comparator;
import java.util.Iterator;

public class VF2SubgraphIsomorphism {

    private final Comparator<RelationshipEdge> myEdgeComparator;
    private final Comparator<Vertex> myVertexComparator;
    private VF2AbstractIsomorphismInspector<Vertex, RelationshipEdge> inspector;

    public VF2SubgraphIsomorphism()
    {
        myEdgeComparator = (o1, o2) -> {
            if (o1.getLabel().equals("*") || o2.getLabel().equals("*"))
                return 0;
            else if (o1.getLabel().equals(o2.getLabel()))
                return 0;
            else
                return 1;
        };

        myVertexComparator = (v1, v2) -> {
            if (v1.isMapped(v2))
                return 0;
            else
                return 1;
        };
    }
    public Iterator<GraphMapping<Vertex, RelationshipEdge>> execute(VF2DataGraph dataGraph, VF2PatternGraph pattern, double support, boolean cacheEdges)
    {
        System.out.println("Graph Size :" + dataGraph.getGraph().vertexSet().size());

        long startTime = System.currentTimeMillis();
        inspector = new VF2SubgraphIsomorphismInspector<>(
                dataGraph.getGraph(), pattern.getPattern(),
                myVertexComparator, myEdgeComparator, cacheEdges);

        System.out.println("Search Cost: " + (System.currentTimeMillis() - startTime));
        int size=0;
        if (inspector.isomorphismExists()) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> iterator = inspector.getMappings();
            if(Config.printDetailedMatchingResults)
            {
                while (size < support && iterator.hasNext()) {
                    System.out.println("---------- Match found ---------- ");
                    GraphMapping<Vertex, RelationshipEdge> mappings = iterator.next();

                    for (Vertex v : pattern.getPattern().vertexSet()) {
                        Vertex currentMatchedVertex = mappings.getVertexCorrespondence(v, false);
                        if (currentMatchedVertex != null) {
                            System.out.println(v + " --> " + currentMatchedVertex);
                        }
                    }
                    size++;
                }
                System.out.println("Number of matches: " + size);
            }
            //TODO: Potential error here, if we want to debug and see the matches
            //FIXME: The iterator will go to end if the ConfigParser.printDetailedMatchingResults is true! Will be useless to return
            return iterator;
        }
        else
        {
            System.out.println("No Matches for the query!");
            return null;
        }
    }

    public Iterator<GraphMapping<Vertex, RelationshipEdge>> execute(VF2DataGraph dataGraph, VF2PatternGraph pattern, boolean cacheEdges, boolean isTgfdDiscovery)
    {
        System.out.println("Graph Size :" + dataGraph.getGraph().vertexSet().size());
//        System.gc();

        long startTime = System.currentTimeMillis();
        inspector = new VF2SubgraphIsomorphismInspector<>(
                dataGraph.getGraph(), pattern.getPattern(),
                myVertexComparator, myEdgeComparator, cacheEdges);

        System.out.println("Search Cost: " + (System.currentTimeMillis() - startTime));
        int size=0;
        if (inspector.isomorphismExists()) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> iterator = inspector.getMappings();
            if (!isTgfdDiscovery) {
                while (iterator.hasNext()) {
                    System.out.println("---------- Match found ---------- ");
                    GraphMapping<Vertex, RelationshipEdge> mappings = iterator.next();

                    for (Vertex v : pattern.getPattern().vertexSet()) {
                        Vertex currentMatchedVertex = mappings.getVertexCorrespondence(v, false);
                        if (currentMatchedVertex != null) {
                            System.out.println(v + " --> " + currentMatchedVertex);
                        }
                    }
                    size++;
                }
                System.out.println("Number of matches: " + size);
            }
            //TODO: Potential error here, if we want to debug and see the matches
            //FIXME: The iterator will go to end if the ConfigParser.printDetailedMatchingResults is true! Will be useless to return
            return iterator;
        }
        else
        {
            System.out.println("No Matches for the query!");
            return null;
        }
    }

    public Iterator<GraphMapping<Vertex, RelationshipEdge>> execute(VF2DataGraph dataGraph, VF2PatternGraph pattern, boolean cacheEdges)
    {
        //System.out.println("Graph Size :" + dataGraph.getGraph().vertexSet().size());

        long startTime = System.currentTimeMillis();
        inspector = new VF2SubgraphIsomorphismInspector<>(
                dataGraph.getGraph(), pattern.getPattern(),
                myVertexComparator, myEdgeComparator, cacheEdges);

        System.out.println("Search Cost: " + (System.currentTimeMillis() - startTime));
        int size=0;
        if (inspector.isomorphismExists()) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> iterator = inspector.getMappings();
            if(Config.printDetailedMatchingResults)
            {
                while (iterator.hasNext()) {
                    System.out.println("---------- Match found ---------- ");
                    GraphMapping<Vertex, RelationshipEdge> mappings = iterator.next();

                    for (Vertex v : pattern.getPattern().vertexSet()) {
                        Vertex currentMatchedVertex = mappings.getVertexCorrespondence(v, false);
                        if (currentMatchedVertex != null) {
                            System.out.println(v + " --> " + currentMatchedVertex);
                        }
                    }
                    size++;
                }
                System.out.println("Number of matches: " + size);
            }
            //TODO: Potential error here, if we want to debug and see the matches
            //FIXME: The iterator will go to end if the ConfigParser.printDetailedMatchingResults is true! Will be useless to return
            return iterator;
        }
        else
        {
            System.out.println("No Matches for the query!");
            return null;
        }
    }

    public Iterator<GraphMapping<Vertex, RelationshipEdge>> execute(Graph<Vertex, RelationshipEdge> dataGraph, VF2PatternGraph pattern, boolean cacheEdges)
    {
        //System.out.println("Graph Size :" + dataGraph.getGraph().vertexSet().size());

        long startTime = System.currentTimeMillis();
        inspector = new VF2SubgraphIsomorphismInspector<>(
                dataGraph, pattern.getPattern(),
                myVertexComparator, myEdgeComparator, cacheEdges);

        if(Config.printDetailedMatchingResults)
            System.out.println("Search Cost: "+ (System.currentTimeMillis() - startTime));
        int size=0;
        if (inspector.isomorphismExists()) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> iterator = inspector.getMappings();
            if(Config.printDetailedMatchingResults)
            {
                while (iterator.hasNext()) {
                    System.out.println("---------- Match found ---------- ");
                    GraphMapping<Vertex, RelationshipEdge> mappings = iterator.next();

                    for (Vertex v : pattern.getPattern().vertexSet()) {
                        Vertex currentMatchedVertex = mappings.getVertexCorrespondence(v, false);
                        if (currentMatchedVertex != null) {
                            System.out.println(v + " --> " + currentMatchedVertex);
                        }
                    }
                    size++;
                }
                System.out.println("Number of matches: " + size);
            }
            return iterator;
        }
        else
        {
            if(Config.printDetailedMatchingResults)
                System.out.println("No Matches for the query!");
            return null;
        }
    }
}
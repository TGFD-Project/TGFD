package VF2Runner;

import infra.VF2DataGraph;
import infra.VF2PatternGraph;
import infra.RelationshipEdge;
import infra.Vertex;
import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2AbstractIsomorphismInspector;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;

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
            if (v1.isEqual(v2))
                return 0;
            else
                return 1;
        };
    }

    public Iterator<GraphMapping<Vertex, RelationshipEdge>> execute(VF2DataGraph dataGraph, VF2PatternGraph pattern, boolean print)
    {
        //System.out.println("Graph Size :" + dataGraph.getGraph().vertexSet().size());

        long startTime = System.currentTimeMillis();
        inspector = new VF2SubgraphIsomorphismInspector<>(
                dataGraph.getGraph(), pattern.getGraph(),
                myVertexComparator, myEdgeComparator, false);
        long endTime = System.currentTimeMillis();

        System.out.println("Search Cost Time:" + (endTime - startTime) + "ms");
        int size=0;
        if (inspector.isomorphismExists()) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> iterator = inspector.getMappings();
            if(print)
            {
                while (iterator.hasNext()) {
                    System.out.println("---------- Match found ---------- ");
                    GraphMapping<Vertex, RelationshipEdge> mappings = iterator.next();

                    for (Vertex v : pattern.getGraph().vertexSet()) {
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
            System.out.println("No Matches for the query!");
            return null;
        }
    }
}
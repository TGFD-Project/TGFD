package VF2Runner;

import infra.VF2DataGraph;
import infra.VF2PatternGraph;
import infra.relationshipEdge;
import infra.vertex;
import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2AbstractIsomorphismInspector;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;

import java.util.Comparator;
import java.util.Iterator;

public class VF2SubgraphIsomorphism {

    private final Comparator<relationshipEdge> myEdgeComparator;
    private final Comparator<vertex> myVertexComparator;
    private VF2AbstractIsomorphismInspector<vertex, relationshipEdge> inspector;

    public VF2SubgraphIsomorphism()
    {
        myEdgeComparator = (o1, o2) -> {
            if (o1.getLabel().equals(o2.getLabel()))
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

    public void executeAndPrintResults(VF2DataGraph dataGraph, VF2PatternGraph pattern)
    {
 
        System.out.println("Graph Size :" + dataGraph.getGraph().vertexSet().size());

        long startTime = System.currentTimeMillis();
        inspector = new VF2SubgraphIsomorphismInspector<>(dataGraph.getGraph(), pattern.getGraph(),
                myVertexComparator, myEdgeComparator, false);
        long endTime = System.currentTimeMillis();

        System.out.println("Search Cost Time:" + (endTime - startTime) + "ms");

        if (inspector.isomorphismExists()) {

            Iterator<GraphMapping<vertex, relationshipEdge>> iterator = inspector.getMappings();

            while (iterator.hasNext()) {

                System.out.println("---------- Match found ---------- ");
                GraphMapping<vertex, relationshipEdge> mappings = iterator.next();

                for (vertex v : pattern.getGraph().vertexSet()) {
                    vertex currentMatchedVertex = mappings.getVertexCorrespondence(v, false);
                    if (currentMatchedVertex != null) {
                        System.out.println(v + " --> " + currentMatchedVertex);
                    }
                }
            }
        }
        else
        {
            System.out.println("No Matches for the query!");
        }
    }

    public Iterator<GraphMapping<vertex, relationshipEdge>> execute(VF2DataGraph dataGraph, VF2PatternGraph pattern)
    {

        System.out.println("Graph Size :" + dataGraph.getGraph().vertexSet().size());

        long startTime = System.currentTimeMillis();
        inspector = new VF2SubgraphIsomorphismInspector<>(dataGraph.getGraph(), pattern.getGraph(),
                myVertexComparator, myEdgeComparator, false);
        long endTime = System.currentTimeMillis();

        System.out.println("Search Cost Time:" + (endTime - startTime) + "ms");

        if (inspector.isomorphismExists()) {

            Iterator<GraphMapping<vertex, relationshipEdge>> iterator = inspector.getMappings();

            while (iterator.hasNext()) {

                System.out.println("---------- Match found ---------- ");
                GraphMapping<vertex, relationshipEdge> mappings = iterator.next();

                for (vertex v : pattern.getGraph().vertexSet()) {
                    vertex currentMatchedVertex = mappings.getVertexCorrespondence(v, false);
                    if (currentMatchedVertex != null) {
                        System.out.println(v + " --> " + currentMatchedVertex);
                    }
                }
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

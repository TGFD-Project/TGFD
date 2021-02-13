package infra;

import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;

import java.util.List;

/**
 * Represents a match.
 * @note We do not need the edges of a match
 */
public final class Match {
    //region --[Fields: Private]---------------------------------------
    // Intervals where the match exists.
    private List<Interval> intervals;

    // Graph mapping from pattern graph to match graph.
    private GraphMapping<Vertex, RelationshipEdge> mapping;

    // Pattern graph.
    private VF2PatternGraph pattern;
    //endregion

    //region --[Constructors]------------------------------------------
    /**
     * Creates a new Match.
     */
    public Match() {
        // TODO: add argument for X to be used in getSignature [2021-02-12]
    }
    //endregion

    //region --[Methods: Public]---------------------------------------
    /**
     * Gets the signature of a match for comparison across time.
     * @note Signature consists of the attributes of the vertices on X.
     */
    public static String getSignature(
        VF2PatternGraph pattern,
        GraphMapping<Vertex, RelationshipEdge> mapping)
    {
        return "";
        //for (var patternVertex : pattern.getGraph().vertexSet())
        //{
        //    var matchVertex = mapping.getVertexCorrespondence(patternVertex, false);
        //    if (matchVertex == null)
        //        continue;
        //
        //

        //    for (Literal l : tgfd.getDependency().getX())
        //    {
        //        if (l instanceof ConstantLiteral)
        //        {
        //            if (matchVertex.getTypes().contains(((constantLiteral) l).getVertexType()))
        //            {
        //                //if(matchVertex.attContains())
        //            }
        //        }
        //        else if (l instanceof VariableLiteral)
        //        {
        //        }
        //    }
        //}

        //var builder = new StringBuilder();
        //getVertices()
        //    .stream()
        //    .sorted() // Ensure stable sorting of vertices
        //    .forEach(vertex -> {
        //        vertex
        //            .getAllAttributesList()
        //            .stream()
        //            .sorted() // Ensure stable sorting of attributes
        //            .forEach(attr -> {
        //                // TODO: filter for only attributes of X [2021-02-12]
        //                builder.append(attr.getAttrValue());
        //                builder.append(",");
        //            });
        //    });
        //// CONSIDER: Return a hash [2021-02-12]
        //return builder.toString();
    }
    //endregion

    //region --[Properties: Public]------------------------------------
    /**
     * Gets the intervals of the match.
     */
    public List<Interval> getIntervals() {
        return this.intervals;
    }

    /**
     * Gets the vertices of the match.
     */
    public GraphMapping<Vertex, RelationshipEdge> getMapping() {
        return this.mapping;
    }

    /**
     * Gets the pattern graph.
     */
    public VF2PatternGraph getPattern() { return pattern; }

    /**
     * Gets the vertices of the match that are valid for the corresponding intervals.
     */
    public List<DataVertex> getVertices() {
        // TODO: remove if not needed (if TGFD ond Signature just uses pattern + mapping) [2021-02-13]
        throw new UnsupportedOperationException("not implemented");
    }
    //endregion
}
package infra;

import org.jgrapht.GraphMapping;

import java.util.List;

/**
 * Represents a match.
 * @note We do not need the edges of a match
 */
public final class Match {
    // Intervals where the match exists.
    private List<Interval> intervals;

    // Vertices of the match that are valid for the corresponding intervals.
    //private List<dataVertex> vertices;

    // graph mapping of the matched vertices for the corresponding intervals.
    private GraphMapping<vertex, relationshipEdge> mapping;

    /**
     * Creates a new Match.
     */
    public Match() {
        // TODO: add argument for X to be used in getSignature [2021-02-12]
    }

    /**
     * Gets the intervals of the match.
     */
    public List<Interval> getIntervals() {
        return this.intervals;
    }

    /**
     * Gets the signature of the match for comparison across time.
     * @note Signature is consists of the attributes of the vertices on X.
     */
    public String getSignature() {
        var builder = new StringBuilder();
        vertices
            .stream()
            .sorted() // Ensure stable sorting of vertices
            .forEach(vertex -> {
                vertex
                    .getAllAttributesList()
                    .stream()
                    .sorted() // Ensure stable sorting of attributes
                    .forEach(attr -> {
                        // TODO: filter for only attributes of X [2021-02-12]
                        builder.append(attr.getAttrValue());
                        builder.append(",");
                    });
            });
        // CONSIDER: Return a hash [2021-02-12]
        return builder.toString();
    }

    /**
     * Gets the vertices of the match.
     */
    public GraphMapping<vertex, relationshipEdge> getMapping() {
        return mapping;
    }
}
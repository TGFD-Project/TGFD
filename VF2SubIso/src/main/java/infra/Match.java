package infra;

import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;

import java.util.ArrayList;
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
    public Match(
        VF2PatternGraph pattern,
        GraphMapping<Vertex, RelationshipEdge> mapping)
    {
        this.pattern = pattern;
        this.mapping = mapping;
    }
    //endregion

    //region --[Methods: Public]---------------------------------------
    /**
     * Gets the signature of a match for comparison across time w.r.t. the pattern.
     * @param pattern Pattern of the match.
     * @param mapping Mapping of the match.
     */
    public static String signatureFromPattern(
        VF2PatternGraph pattern,
        GraphMapping<Vertex, RelationshipEdge> mapping)
    {
        var builder = new StringBuilder();

        // NOTE: Ensure stable sorting of vertices [2021-02-13]
        var sortedPatternVertices = pattern.getGraph().vertexSet().stream().sorted();
        sortedPatternVertices.forEach(patternVertex ->
        {
            var matchVertex = mapping.getVertexCorrespondence(patternVertex, false);
            if (matchVertex == null)
                return;

            // NOTE: Ensure stable sorting of attributes [2021-02-13]
            var sortedAttributes = matchVertex.getAllAttributesList().stream().sorted();
            sortedAttributes.forEach(attribute ->
            {
                builder.append(attribute.getAttrValue());
                builder.append(",");
            });
        });
        // TODO: consider returning a hash [2021-02-13]
        return builder.toString();
    }

    /**
     * Gets the signature of a match for comparison across time w.r.t. the x of the dependency.
     * @param pattern Pattern of the match.
     * @param mapping Mapping of the match.
     * @param xLiterals Literals of the X dependency.
     */
    public static String signatureFromX(
        VF2PatternGraph pattern,
        GraphMapping<Vertex, RelationshipEdge> mapping,
        ArrayList<Literal> xLiterals)
    {
        // TODO: can we assume that all x variable literals are also defined in the pattern? [2021-02-13]
        var builder = new StringBuilder();

        // NOTE: Ensure stable sorting of vertices [2021-02-13]
        var sortedPatternVertices = pattern.getGraph().vertexSet().stream().sorted();
        sortedPatternVertices.forEach(patternVertex ->
        {
            var matchVertex = mapping.getVertexCorrespondence(patternVertex, false);
            if (matchVertex == null)
                return;

            // NOTE: Ensure stable sorting of attributes [2021-02-13]
            var sortedAttributes = matchVertex.getAllAttributesList().stream().sorted();
            sortedAttributes.forEach(attribute ->
            {
                for (Literal literal : xLiterals)
                {
                    if (literal instanceof ConstantLiteral)
                    {
                        var constantLiteral = (ConstantLiteral)literal;
                        if (!matchVertex.getTypes().contains(constantLiteral.getVertexType()))
                            continue;
                        if (attribute.getAttrName() != constantLiteral.attrName)
                            continue;
                        if (attribute.getAttrValue() != constantLiteral.attrValue)
                            continue;

                        builder.append(attribute.getAttrValue());
                        builder.append(",");
                    }
                    else if (literal instanceof VariableLiteral)
                    {
                        // TODO: if variable literal then exclude from signature? [2021-02-13]
                    }
                }
            });
        });
        // TODO: consider returning a hash [2021-02-13]
        return builder.toString();

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
    }

    /**
     * Gets the signature of a match for comparison across time w.r.t. the dependency.
     * @param pattern Pattern of the match.
     * @param mapping Mapping of the match.
     * @param dependency TGFD dependency.
     */
    public static String signatureFromDependency(
        VF2PatternGraph pattern,
        GraphMapping<Vertex, RelationshipEdge> mapping,
        Dependency dependency)
    {
        throw new UnsupportedOperationException("not implemented");
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
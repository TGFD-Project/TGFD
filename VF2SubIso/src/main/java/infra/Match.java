package infra;

import org.jgrapht.GraphMapping;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a match.
 * @note We do not need the edges of a match
 */
public final class Match {
    //region --[Fields: Private]---------------------------------------
    /** Intervals where the match exists. */
    private List<Interval> intervals = new ArrayList<Interval>();

    /** Graph mapping from pattern graph to match graph. */
    private GraphMapping<Vertex, RelationshipEdge> mapping;

    /** Pattern graph. */
    private VF2PatternGraph pattern;

    /** Signature of the match computed from X. */
    private String signatureX;
    //endregion

    //region --[Constructors]------------------------------------------
    private Match(
        VF2PatternGraph pattern,
        GraphMapping<Vertex, RelationshipEdge> mapping,
        List<Interval> intervals)
    {
        this.pattern = pattern;
        this.mapping = mapping;
        this.intervals = intervals;
    }

    /**
     * Create a new Match.
     * @param pattern Pattern of the match.
     * @param mapping Mapping of the match.
     * @param signatureX Signature of the match computed from X.
     */
    public Match(VF2PatternGraph pattern, GraphMapping<Vertex, RelationshipEdge> mapping, String signatureX)
    {
        this.pattern = pattern;
        this.mapping = mapping;
        this.signatureX=signatureX;
    }

    /**
     * Creates a new Match.
     * @param intervals Intervals of the match.
     */
    public Match WithIntervals(List<Interval> intervals)
    {
        return new Match(
            this.pattern,
            this.mapping,
            intervals);
    }
    //endregion

    //region --[Methods: Public]---------------------------------------
    /**
     * Adds a timepoint to the match.
     *
     * Will either extend the latest interval to include the new timepoint, or
     * add a new interval (break in intervals represents that no match occurred).
     *
     * @param timepoint Timepoint of match.
     * @param granularity Minimum timespan between matches.
     * @exception IllegalArgumentException if timepoint is before the latest interval's end.
     * @exception IllegalArgumentException if timepoint is less than the granularity away from the latest interval end.
     */
    public void addTimepoint(LocalDate timepoint, Duration granularity)
    {
        if (intervals.isEmpty())
        {
            intervals.add(new Interval(timepoint, timepoint));
            return;
        }

        var latestInterval = intervals.stream()
            .max(Comparator.comparing(Interval::getEnd))
            .orElseThrow();

        var latestEnd = latestInterval.getEnd();
        if (timepoint.isBefore(latestEnd) || timepoint.isEqual(latestEnd))
            throw new IllegalArgumentException("Timepoint is <= the latest interval's end");

        var sinceEnd = Duration.between(latestEnd, timepoint);
        var comparison = sinceEnd.compareTo(granularity);
        if (comparison > 0)
        {
            // Time since end is greater than the granularity so add a new interval.
            // This represents that the match did not exist between the latestInterval.end and newInterval.start.
            intervals.add(new Interval(timepoint, timepoint));
        }
        else if (comparison == 0)
        {
            // Time since end is the granularity so extend the last interval.
            // This represents that the match continued existing for this interval.
            latestInterval.setEnd(timepoint);
        }
        else
        {
            throw new IllegalArgumentException("Timepoint is less than the granularity away from the latest interval end");
        }
    }

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
     * Gets the signature of a match for comparison across time w.r.t. the X of the dependency.
     * @param pattern Pattern of the match.
     * @param mapping Mapping of the match.
     * @param xLiterals Literals of the X dependency.
     */
    public static String signatureFromX(
        VF2PatternGraph pattern,
        GraphMapping<Vertex, RelationshipEdge> mapping,
        ArrayList<Literal> xLiterals)
    {
        // We assume that all x variable literals are also defined in the pattern? [2021-02-13]
        var builder = new StringBuilder();

        // TODO: consider collecting (type, name, attr) and sorting at the end [2021-02-14]

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
                    // We can ignore constant literals because a Match is for a single TGFD which has constant defined in the pattern
                    if (literal instanceof VariableLiteral)
                    {
                        var varLiteral = (VariableLiteral)literal;
                        var matchVertexTypes = matchVertex.getTypes();
                        if ((matchVertexTypes.contains(varLiteral.getVertexType_1()) && attribute.getAttrName().equals(varLiteral.getAttrName_1())) ||
                            (matchVertexTypes.contains(varLiteral.getVertexType_2()) && attribute.getAttrName().equals(varLiteral.getAttrName_2())))
                        {
                            builder.append(attribute.getAttrValue());
                            builder.append(",");
                        }
                    }
                }
            });
        });
        // TODO: consider returning a hash [2021-02-13]
        return builder.toString();
    }
    //endregion

    //region --[Properties: Public]------------------------------------
    /**
     * Gets the intervals of the match.
     */
    public List<Interval> getIntervals() { return this.intervals; }

    /**
     * Gets the vertices of the match.
     */
    public GraphMapping<Vertex, RelationshipEdge> getMapping() { return this.mapping; }

    /**
     * Gets the pattern graph.
     */
    public VF2PatternGraph getPattern() { return pattern; }

    /**
     * Gets the signature of the match computed from X.
     */
    public String getSignatureX() {
        return signatureX;
    }

    //endregion
}
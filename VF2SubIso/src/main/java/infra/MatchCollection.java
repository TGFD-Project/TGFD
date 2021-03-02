package infra;

import org.jgrapht.GraphMapping;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that stores matches across timepoints for a single TGFD.
 */
public class MatchCollection
{
    //region --[Fields: Private]---------------------------------------
    // TODO: handle concurrent use of TemporalGraph [2021-02-24]
    /** Temporal graph containing the vertices to reduce memory consumption by the matches. */
    private TemporalGraph<Vertex> temporalGraph;

    /** Dependency of MatchCollection */
    private Dependency dependency;

    /** The minimum timespan between matches. */
    private Duration granularity;

    // TODO: replace map value type with List<Match> [2021-02-23]
    /** Mapping of match signatures to matches. */
    private AbstractMap<String, Match> matchesBySignature = new HashMap<>();

    // TODO: deduplicate vertices in Match [2021-02-23]
    // This may not be so easy because we need to deduplicate by vertex and time.
    // We need some sort of shared temporal representation of the vertices that we can retrieve given id and timestamp.

    /** Pattern graph of the match. */
    private VF2PatternGraph pattern;

    /** Stores the timestamps of the input data*/
    private ArrayList<LocalDate> timeStamps = new ArrayList<>();

    //endregion

    //region --[Constructors]------------------------------------------
    /**
     * Creates a MatchCollection.
     * @param pattern Pattern of all matches in this collection.
     * @param pattern Dependency of all matches in this collection.
     * @param granularity Minimum timespan between matches.
     */
    public MatchCollection(
        VF2PatternGraph pattern,
        Dependency dependency,
        Duration granularity)
    {
        this.pattern = pattern;
        this.dependency = dependency;
        this.granularity = granularity;
        this.temporalGraph = new TemporalGraph<>(granularity);
    }
    //endregion

    //region --[Methods: Private]--------------------------------------
    /**
     * Add a match for a timepoint.
     * @param timepoint Timepoint of the match.
     * @param mapping The mapping of the match.
     */
    private void addMatch(
        LocalDate timepoint,
        GraphMapping<Vertex, RelationshipEdge> mapping)
    {
        var signature = Match.signatureFromX2(pattern, mapping, dependency.getX());

        var match = matchesBySignature.getOrDefault(signature, null);
        if (match == null)
        {
            match = new Match(temporalGraph, mapping, signature, timepoint);
            matchesBySignature.put(signature, match);
        }

        var signatureY=Match.signatureFromY2(pattern,mapping,dependency.getY());

        match.addTimepoint(timepoint, granularity);
        match.addSignatureY(timepoint,granularity,signatureY);
    }

    /**
     * Adds vertices of the match to the TemporalGraph shared by matches in this collection.
     * @param timepoint Timepoint of the match.
     * @param mapping Mapping of the match.
     */
    private void addVertices(LocalDate timepoint, GraphMapping<Vertex, RelationshipEdge> mapping)
    {
        for (var pattenVertex : pattern.getGraph().vertexSet())
        {
            var matchVertex = mapping.getVertexCorrespondence(pattenVertex, false);

            // TODO: change Vertex type to DataVertex or add vertex id to Vertex [2021-02-24]
            temporalGraph.addVertex(
                matchVertex,
                ((DataVertex)matchVertex).getVertexURI(),
                timepoint);
        }
    }
    //endregion

    //region --[Methods: Public]---------------------------------------
    /**
     * Adds matches for a timepoint.
     * @param timepoint Timepoint of the matches.
     * @param mappingIterator An iterator over all isomorphic mappings from the pattern.
     */
    public int addMatches(
        LocalDate timepoint,
        Iterator<GraphMapping<Vertex, RelationshipEdge>> mappingIterator)
    {
        if (mappingIterator==null)
            return 0;

        if (!timeStamps.contains(timepoint))
            timeStamps.add(timepoint);

        int matchCount = 0;
        while (mappingIterator.hasNext())
        {
            var mapping = mappingIterator.next();
            addMatch(timepoint, mapping);
            addVertices(timepoint, mapping);
            matchCount++;
        }
        return matchCount;
    }


    /**
     * @param timepoint Timepoint of the matches.
     * @param newMatches A HashMap of <SignatureFromPattern,mapping> of all the new matches.
     */
    public void addMatches(LocalDate timepoint,
            HashMap <String, GraphMapping <Vertex, RelationshipEdge>> newMatches)
    {
        if (!timeStamps.contains(timepoint))
            timeStamps.add(timepoint);

        for (GraphMapping <Vertex, RelationshipEdge> mapping:newMatches.values()) {
            addMatch(timepoint, mapping);
            addVertices(timepoint, mapping);
        }
    }
    //endregion

    //region --[Properties: Public]------------------------------------
    /** Gets the minimum timespan between matches. */
    public Duration getGranularity() { return this.granularity; }

    /** Gets all the timestamps from the input data (snapshots of the data). */
    public ArrayList<LocalDate> getTimeStamps() {
        return timeStamps;
    }

    /** Returns matches across all time. */
    public List<Match> getMatches() {
        return new ArrayList<>(matchesBySignature
                .values());
    }

    /** Returns matches applicable for only the given timepoint. */
    public List<Match> getMatches(LocalDate timepoint) {
        var intervals = List.of(new Interval(timepoint, timepoint));
        return matchesBySignature
            .values()
            .stream()
            .filter(match -> match.getIntervals().stream().anyMatch(intv -> intv.contains(timepoint)))
            .map(match -> match.WithIntervals(intervals))
            .collect(Collectors.toList());
    }
    //endregion
}
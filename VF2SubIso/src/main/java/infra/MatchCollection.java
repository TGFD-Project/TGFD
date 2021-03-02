package infra;

import org.jgrapht.GraphMapping;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that stores matches across timestamps for a single TGFD.
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
    private HashSet<LocalDate> timestamps = new HashSet<>();

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
     * Add a match for a timestamp.
     * @param timestamp Timepoint of the match.
     * @param mapping The mapping of the match.
     */
    private void addMatch(
        LocalDate timestamp,
        GraphMapping<Vertex, RelationshipEdge> mapping)
    {
        var signature = Match.signatureFromX2(pattern, mapping, dependency.getX());

        var match = matchesBySignature.getOrDefault(signature, null);
        if (match == null)
        {
            match = new Match(temporalGraph, mapping, signature, timestamp);
            matchesBySignature.put(signature, match);
        }

        var signatureY=Match.signatureFromY2(pattern,mapping,dependency.getY());

        match.addTimepoint(timestamp, granularity);
        match.addSignatureY(timestamp,granularity,signatureY);
    }

    /**
     * Adds vertices of the match to the TemporalGraph shared by matches in this collection.
     * @param timestamp Timepoint of the match.
     * @param mapping Mapping of the match.
     */
    private void addVertices(LocalDate timestamp, GraphMapping<Vertex, RelationshipEdge> mapping)
    {
        for (var pattenVertex : pattern.getGraph().vertexSet())
        {
            var matchVertex = mapping.getVertexCorrespondence(pattenVertex, false);

            // TODO: change Vertex type to DataVertex or add vertex id to Vertex [2021-02-24]
            temporalGraph.addVertex(
                matchVertex,
                ((DataVertex)matchVertex).getVertexURI(),
                timestamp);
        }
    }
    //endregion

    //region --[Methods: Public]---------------------------------------
    /**
     * Adds matches for a timestamp.
     * @param timestamp Timepoint of the matches.
     * @param mappingIterator An iterator over all isomorphic mappings from the pattern.
     */
    public int addMatches(
        LocalDate timestamp,
        Iterator<GraphMapping<Vertex, RelationshipEdge>> mappingIterator)
    {
        if (mappingIterator == null)
            return 0;

        timestamps.add(timestamp);

        int matchCount = 0;
        while (mappingIterator.hasNext())
        {
            var mapping = mappingIterator.next();
            addMatch(timestamp, mapping);
            addVertices(timestamp, mapping);
            matchCount++;
        }
        return matchCount;
    }


    /**
     * Adds matches for a timestamp.
     * @param timepoint Timepoint of the matches.
     * @param newMatches A HashMap of <SignatureFromPattern,mapping> of all the new matches.
     */
    public void addMatches(
        LocalDate timepoint,
        HashMap <String, GraphMapping <Vertex, RelationshipEdge>> newMatches)
    {
        timestamps.add(timepoint);

        for (var mapping : newMatches.values())
        {
            addMatch(timepoint, mapping);
            addVertices(timepoint, mapping);
        }
    }

    /**
     * Add timestamp to all matches that are neither new or removed (for incremental case).
     * @param timestamp Timestamp to add to relevant matches.
     * @param newMatchesSignatures Signatures from new matches.
     * @param removedMatchesSignatures Signatures from deleted matches.
     */
    public void addTimestamp(
        LocalDate timestamp,
        Collection<String> newMatchesSignatures,
        Collection<String> removedMatchesSignatures)
    {
        timestamps.add(timestamp);

        var newSignatures = newMatchesSignatures.stream().distinct().collect(Collectors.toSet());
        var removedSignatures = removedMatchesSignatures.stream().distinct().collect(Collectors.toSet());

        var signaturesToUpdate = matchesBySignature.keySet().stream()
            .filter(k -> !newSignatures.contains(k))
            .filter(k -> !removedSignatures.contains(k))
            .collect(Collectors.toList());

        for (var signature : signaturesToUpdate)
            matchesBySignature.get(signature).addTimepoint(timestamp, granularity);
    }
    //endregion

    //region --[Properties: Public]------------------------------------
    /** Gets the minimum timespan between matches. */
    public Duration getGranularity() { return this.granularity; }

    /** Gets all the timestamps from the input data (snapshots of the data). */
    public LocalDate[] getTimestamps() {
        return timestamps.stream().toArray(LocalDate[]::new);
    }

    /** Returns matches across all time. */
    public List<Match> getMatches() {
        return new ArrayList<>(matchesBySignature
                .values());
    }

    /** Returns matches applicable for only the given timestamp. */
    public List<Match> getMatches(LocalDate timestamp) {
        var intervals = List.of(new Interval(timestamp, timestamp));
        return matchesBySignature
            .values()
            .stream()
            .filter(match -> match.getIntervals().stream().anyMatch(intv -> intv.contains(timestamp)))
            .map(match -> match.WithIntervals(intervals))
            .collect(Collectors.toList());
    }
    //endregion
}
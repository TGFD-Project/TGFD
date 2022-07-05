package Infra;

import QPathBasedWorkload.VertexMapping;
import Util.Config;
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
    private DataDependency dependency;

    /** The minimum timespan between matches. */
    private Duration granularity;

    // TODO: replace map value type with List<Match> [2021-02-23]
    /** Mapping of match signatures to matches. */
    public AbstractMap<String, Match> matchesBySignature = new HashMap<>();

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
            DataDependency dataDependency,
            Duration granularity)
    {
        this.pattern = pattern;
        this.dependency = dataDependency;
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
        var signature = Match.signatureFromX(pattern, mapping, dependency.getX());

        var match = matchesBySignature.getOrDefault(signature, null);
        if (match == null)
        {
            match = new Match(temporalGraph, mapping, signature, timestamp);
            matchesBySignature.put(signature, match);
        }

        var signatureY=Match.signatureFromY(pattern,mapping,dependency.getY());

        if(Config.debug)
            System.out.println(signature + " ->" + signatureY);
        match.addTimepoint(timestamp, granularity);
        match.addSignatureY(timestamp,granularity,signatureY);
        match.addSignatureYBasedOnTimestap(timestamp,signatureY);
        // TODO: This is extra and not needed for runtime tests
        // TODO: This has to be a map of signatures from pattern at different timestamps
        match.setSignatureFromPattern(Match.signatureFromPattern(pattern,mapping));
    }

    /**
     * Add a match for a timestamp.
     * @param timestamp Timepoint of the match.
     * @param mapping The mapping of the match.
     */
    private void addMatch(
            LocalDate timestamp,
            VertexMapping mapping)
    {
        var signature = Match.signatureFromX(pattern, mapping, dependency.getX());

        var match = matchesBySignature.getOrDefault(signature, null);
        if (match == null)
        {
            match = new Match(temporalGraph, mapping, signature, timestamp);
            matchesBySignature.put(signature, match);
        }

        var signatureY=Match.signatureFromY(pattern,mapping,dependency.getY());

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
        for (var pattenVertex : pattern.getPattern().vertexSet())
        {
            var matchVertex = mapping.getVertexCorrespondence(pattenVertex, false);

            // TODO: change Vertex type to DataVertex or add vertex id to Vertex [2021-02-24]
            temporalGraph.addVertex(
                    matchVertex,
                    ((DataVertex)matchVertex).getVertexURI(),
                    timestamp);
        }
    }

    /**
     * Adds vertices of the match to the TemporalGraph shared by matches in this collection.
     * @param timestamp Timepoint of the match.
     * @param mapping VertexMapping of the match.
     */
    private void addVertices(LocalDate timestamp, VertexMapping mapping)
    {
        for (var pattenVertex : pattern.getPattern().vertexSet())
        {
            var matchVertex = mapping.getVertexCorrespondence(pattenVertex);

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
            GraphMapping<Vertex, RelationshipEdge> mapping = mappingIterator.next();
            if (Config.debug) {
                System.out.println("---------- Match found ---------- ");
                for (Vertex v : pattern.getPattern().vertexSet()) {
                    Vertex currentMatchedVertex = mapping.getVertexCorrespondence(v, false);
                    if (currentMatchedVertex != null) {
                        System.out.println(v + " --> " + currentMatchedVertex);
                    }
                }
            }
            addMatch(timestamp, mapping);
            addVertices(timestamp, mapping);
            matchCount++;
        }
        System.out.println("Total Number of matches: " + matchCount);
        return matchCount;
    }


    /**
     * Adds matches for a timestamp.
     * @param timestamp Timepoint of the matches.
     * @param mappings An Arraylist of vertex mapping to the graph pattern.
     */
    public int addMatches(
            LocalDate timestamp,
            Collection<VertexMapping> mappings)
    {
        if (mappings == null)
            return 0;

        timestamps.add(timestamp);

        int matchCount = 0;
        long start= System.currentTimeMillis();
        for (VertexMapping mapping:mappings) {

            addMatch(timestamp, mapping);
            addVertices(timestamp, mapping);
        }
        System.out.println("Total Number of matches: " + matchCount);
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
            if (Config.debug) {
                System.out.println("---------- Match found ---------- ");
                for (Vertex v : pattern.getPattern().vertexSet()) {
                    Vertex currentMatchedVertex = mapping.getVertexCorrespondence(v, false);
                    if (currentMatchedVertex != null) {
                        System.out.println(v + " --> " + currentMatchedVertex);
                    }
                }
            }
            addMatch(timepoint, mapping);
            addVertices(timepoint, mapping);
        }
    }

    /**
     * Add timestamp to all matches that are neither new or removed (for incremental case).
     * @param timestamp Timestamp to add to relevant matches.
     * @param previousTimeStamp Previous time stamp.
     * @param newMatchesSignatures Signatures from new matches.
     * @param removedMatchesSignatures Signatures from deleted matches.
     */
    public void addTimestamp(
            LocalDate timestamp,
            LocalDate previousTimeStamp,
            Collection<String> newMatchesSignatures,
            Collection<String> removedMatchesSignatures)
    {
        timestamps.add(timestamp);

        var newSignatures = newMatchesSignatures.stream().collect(Collectors.toSet());
        var removedSignatures = removedMatchesSignatures.stream().collect(Collectors.toSet());

        var signaturesToUpdate = matchesBySignature.keySet().stream()
                .filter(k -> !newSignatures.contains(k))
                .filter(k -> !removedSignatures.contains(k))
                .collect(Collectors.toList());

        for (var signature : signaturesToUpdate)
        {
            matchesBySignature.get(signature).addTimepoint(timestamp, granularity);
            var signatureY=Match.signatureFromY(pattern,matchesBySignature.get(signature).getMatchMapping(),dependency.getY());
            matchesBySignature.get(signature).addSignatureYBasedOnTimestap(timestamp,signatureY);
            if(Config.debug)
            {
                if(matchesBySignature.get(signature).getSignatureY(previousTimeStamp)!= null &&
                        !matchesBySignature.get(signature).getSignatureY(previousTimeStamp).equals(signatureY))
                {
                    System.out.println("Change in the existing match: " + matchesBySignature.get(signature).getSignatureY(previousTimeStamp));
                    System.out.println("Changed to: " + signatureY);

                }
            }
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

        var newSignatures = newMatchesSignatures.stream().collect(Collectors.toSet());
        var removedSignatures = removedMatchesSignatures.stream().collect(Collectors.toSet());

        var signaturesToUpdate = matchesBySignature.keySet().stream()
                .filter(k -> !newSignatures.contains(k))
                .filter(k -> !removedSignatures.contains(k))
                .collect(Collectors.toList());

        for (var signature : signaturesToUpdate)
        {
            matchesBySignature.get(signature).addTimepoint(timestamp, granularity);
            var signatureY=Match.signatureFromY(pattern,matchesBySignature.get(signature).getMatchMapping(),dependency.getY());
            matchesBySignature.get(signature).addSignatureYBasedOnTimestap(timestamp,signatureY);
        }
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
        List<Match> res=new ArrayList<>();
        for (Match match:matchesBySignature.values()) {
            if(match.getIntervals().stream().anyMatch(intv -> intv.contains(timestamp)))
                res.add(match);
        }
        return res;
//        return matchesBySignature
//            .values()
//            .stream()
//            .filter(match -> match.getIntervals().stream().anyMatch(intv -> intv.contains(timestamp)))
//            .map(match -> match.WithIntervals(intervals))
//            .collect(Collectors.toList());
    }
    //endregion
}
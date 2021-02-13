package infra;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.jgrapht.GraphMapping;
import static java.util.stream.Collectors.toCollection;

/**
 * Class that stores matches across timepoints.
 */
public class MatchCollection
{
    //region --[Fields: Private]---------------------------------------
    /** The minimum timespan between matches. */
    private Duration granularity;

    /** Dependency of MatchCollection */
    private Dependency dependency;

    /** Mapping of match signatures to matches. */
    private AbstractMap<String, Match> matchesBySignature = new HashMap<String, Match>();
    //endregion

    //region --[Constructors]------------------------------------------
    /**
     * Creates a MatchCollection.
     * @param granularity Minimum timespan between matches.
     */
    public MatchCollection(Duration granularity)
    {
        this.granularity = granularity;
    }
    //endregion

    //region --[Methods: Private]--------------------------------------
    /**
     * Add a match for a timepoint.
     * @param timepoint Timepoint of the match.
     * @param pattern Pattern of the match.
     * @param mapping The mapping of the match.
     */
    private void addMatch(
        LocalDate timepoint,
        VF2PatternGraph pattern,
        GraphMapping<Vertex, RelationshipEdge> mapping)
    {
        var signature = Match.signatureFromX(pattern, mapping, dependency.getX());
        var match = matchesBySignature.getOrDefault(signature, null);
        if (match == null)
        {
            match = new Match(pattern, mapping);
            matchesBySignature.put(signature, match);
        }

        match.addTimepoint(timepoint, granularity);
    }
    //endregion

    //region --[Methods: Public]---------------------------------------
    /**
     * Adds matches for a timepoint.
     * @param timepoint Timepoint of the matches.
     * @param pattern Pattern of the matches.
     * @param mappingIterator An iterator over all isomorphic mappings from the pattern.
     */
    public void addMatches(
        LocalDate timepoint,
        VF2PatternGraph pattern,
        Iterator<GraphMapping<Vertex, RelationshipEdge>> mappingIterator)
    {
        // TODO: implement [2021-02-07]
        throw new UnsupportedOperationException("Not implemented");

        //Set<vertex> patternVertices = pattern.getGraph().vertexSet();
        //List<vertex> vertices = new ArrayList<vertex>();
        //while (mappingIterator.hasNext()) {
        //    GraphMapping<vertex, relationshipEdge> mapping = mappingIterator.next();
        //    for (vertex patternVertex : patternVertices) {
        //        vertex v = mapping.getVertexCorrespondence(patternVertex, false);
        //        if (v != null) {
        //            vertices.add(v);
        //        }
        //    }
        //}

        // TODO: get match of previous timepoint by match signature [2021-02-07]
        // TODO: get corresponding match out of previous timepoint [2021-02-07]
        // TODO: handle interval [2021-02-07]
        //   if signature found in previous match
        //     if interval end is the same
        //       extend
        //     else
        //       add new interval
    }
    //endregion

    //region --[Properties: Public]------------------------------------
    /** Gets the minimum timespan between matches. */
    public Duration getGranularity() { return this.granularity; }

    /** Returns matches across all time. */
    public List<Match> getMatches() {
        return matchesBySignature
            .values()
            .stream()
            .collect(Collectors.toList());
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
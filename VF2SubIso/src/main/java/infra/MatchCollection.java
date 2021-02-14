package infra;

import org.jgrapht.GraphMapping;

import java.time.Duration;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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
            match = new Match(pattern, mapping,signature);
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
        while (mappingIterator.hasNext())
        {
            var mapping = mappingIterator.next();
            addMatch(timepoint, pattern, mapping);
        }
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
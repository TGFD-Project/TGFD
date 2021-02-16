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
    /** Dependency of MatchCollection */
    private Dependency dependency;

    /** The minimum timespan between matches. */
    private Duration granularity;

    /** Mapping of match signatures to matches. */
    private AbstractMap<String, Match> matchesBySignature = new HashMap<String, Match>();

    /** Pattern graph of the match. */
    private VF2PatternGraph pattern;

    /** Stores the timestamps of the input data*/
    private ArrayList<LocalDate> timeStamps=new ArrayList<>();
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
    }
    //endregion

    //region --[Methods: Private]--------------------------------------
    /**
     * Add a match for a timepoint.
     * @param timepoint Timepoint of the match.
     * @param mapping The mapping of the match.
     */
    private void addMatch(LocalDate timepoint,
        GraphMapping<Vertex, RelationshipEdge> mapping)
    {
        var signature = Match.signatureFromX2(pattern, mapping, dependency.getX());
        var match = matchesBySignature.getOrDefault(signature, null);
        if (match == null)
        {
            match = new Match(pattern, mapping, signature);
            matchesBySignature.put(signature, match);
        }

        match.addTimepoint(timepoint, granularity);
        match.addSignatureY(timepoint,granularity,Match.signatureFromY2(pattern,mapping,dependency.getY()));
    }
    //endregion

    //region --[Methods: Public]---------------------------------------
    /**
     * Adds matches for a timepoint.
     * @param timepoint Timepoint of the matches.
     * @param mappingIterator An iterator over all isomorphic mappings from the pattern.
     */
    public void addMatches(
        LocalDate timepoint,
        Iterator<GraphMapping<Vertex, RelationshipEdge>> mappingIterator)
    {
        if(mappingIterator==null)
            return;
        timeStamps.add(timepoint);
        int matchCount=0;
        while (mappingIterator.hasNext())
        {
            var mapping = mappingIterator.next();
            addMatch(timepoint, mapping);
            matchCount++;
            if(matchCount%1000==0)
            {
                System.out.println("Match counts: " + matchCount);
            }
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
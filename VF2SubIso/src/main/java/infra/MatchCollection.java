package infra;

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
    /**
     * Mapping of match signatures to matches.
     */
    private HashMap<String, Match> matchesBySignature;
    //endregion

    //region --[Methods: Private]--------------------------------------
    /**
     * Add a match for a timepoint.
     * @param timepoint Timepoint of the match.
     * @param pattern Pattern of the match.
     * @param mapping The mapping of the match.
     */
    private void addMatch(
        int timepoint,
        VF2PatternGraph pattern,
        GraphMapping<Vertex, RelationshipEdge> mapping)
    {

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
        int timepoint,
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
    /**
     * Returns matches across all time.
     */
    public List<Match> getMatches() {
        return matchesBySignature
            .values()
            .stream()
            .collect(Collectors.toList());
    }

    /**
     * Returns matches applicable for only the given timepoint.
     */
    public List<Match> getMatches(int timepoint) {
        // TODO: consider modify the result matches intervals to only be the given timepoint [2021-02-12]
        var intervals = new ArrayList<Interval>()
        {{
            add(new Interval(timepoint, timepoint, 1));
        }};
        return matchesBySignature
            .values()
            .stream()
            .filter(match -> match.getIntervals().stream().anyMatch(i -> i.contains(timepoint)))
            .map(match -> match.WithIntervals(intervals))
            .collect(Collectors.toList());
    }
    //endregion
}
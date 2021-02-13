package infra;

import java.util.*;
import org.jgrapht.GraphMapping;
import static java.util.stream.Collectors.toCollection;

/**
 * Class that stores matches across timepoints.
 */
public class MatchCollection {
    /**
     * Mapping of match signatures to matches.
     */
    private HashMap<String, Match> matchesBySignature;

    /**
     * Adds matches for a timepoint.
     * @param timepoint Timepoint of the matches.
     * @param pattern Pattern of the matches.
     * @param mappingIterator An iterator over all isomorphic mappings from the pattern.
     */
    public void addMatches(
            int timepoint,
            VF2PatternGraph pattern,
            Iterator<GraphMapping<Vertex, relationshipEdge>> mappingIterator) {
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

    /**
     * Returns matches across all time.
     */
    public List<Match> get() {
        return matchesBySignature
            .values()
            .stream()
            .collect(toCollection(ArrayList::new));
    }

    /**
     * Returns matches applicable for only the given timepoint.
     */
    public List<Match> get(int timepoint) {
        // TODO: consider modify the result matches intervals to only be the given timepoint [2021-02-12]
        return matchesBySignature
            .values()
            .stream()
            .filter(match -> match.getIntervals().stream().anyMatch(i -> i.contains(timepoint)))
            .collect(toCollection(ArrayList::new));
    }
}
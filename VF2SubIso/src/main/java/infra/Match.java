package infra;

import java.util.List;
import java.util.Objects;

/**
 * Represents a match.
 * @note We do not need the edges of a match
 */
public class Match {
    // Intervals where the match exists.
    private List<Interval> intervals;
    // Vertices of the match that are valid for the corresponding intervals.
    private List<vertex> vertices;

    /**
     * Creates a new Match.
     */
    public Match() {
        // TODO: implement [2021-02-07]
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Gets the signature of the match for comparison across time.
     * @note Signature is consists of the attributes of the vertices on X.
     */
    public String getSignature() {
        // TODO: implement [2021-02-07]
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Gets the vertices of the match.
     */
    public List<vertex> getVertices() {
        // TODO: implement [2021-02-07]
        throw new UnsupportedOperationException("Not implemented");
    }
}
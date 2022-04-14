package Infra;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class that stores a temporal graph.
 *
 * @param <V> Vertex type.
 * TODO: implement edges if needed
 */
public class TemporalGraph<V>
{
    //region --[Classes: Private]--------------------------------------
    /** Represents a vertex over the given interval. */
    private class TemporalVertex<V>
    {
        public Interval interval;
        public V vertex;

        public TemporalVertex(Interval interval, V vertex)
        {
            this.interval = interval;
            this.vertex = vertex;
        }
    }
    //endregion

    //region --[Fields: Private]---------------------------------------
    /** Minimum timespan between timestamps. */
    private Duration granularity;

    /** Map from vertex id to a list of temporal vertices. */
    private HashMap<String, List<TemporalVertex<V>>> temporalVerticesById = new HashMap<>();
    //endregion

    //region --[Constructors]------------------------------------------
    /**
     * Creates a TemporalGraph.
     * @param granularity Minimum timespan between timestamps.
     */
    public TemporalGraph(Duration granularity)
    {
        this.granularity = granularity;
    }
    //endregion

    //region --[Methods: Public]---------------------------------------
    /**
     * Adds a vertex at the given timestamp.
     * @param vertex Vertex to add.
     * @param vertexId Id of vertex.
     * @param timestamp Timestamp.
     */
    public void addVertex(V vertex, String vertexId, LocalDate timestamp)
    {
        // TODO: extract vertexId from vertex and remove vertexId parameter [2021-02-24]
        var temporalVertices = temporalVerticesById.get(vertexId);
        if (temporalVertices == null)
        {
            temporalVertices = new ArrayList<>();
            temporalVertices.add(new TemporalVertex<>(new Interval(timestamp, timestamp), vertex));
            temporalVerticesById.put(vertexId, temporalVertices);
            return;
        }

        TemporalVertex latestTemporalVertex = null;
        for (var temporalVertex : temporalVertices)
        {
            // TODO: throw exception if argument vertex != temporal vertex [2021-02-24]
            if (temporalVertex.interval.contains(timestamp))
                return ;

            if (latestTemporalVertex == null ||
                latestTemporalVertex.interval.getEnd().isAfter(temporalVertex.interval.getEnd()))
            {
                latestTemporalVertex = temporalVertex;
            }
        }

        var sinceEnd = Duration.between(
            latestTemporalVertex.interval.getEnd().atStartOfDay(),
            timestamp.atStartOfDay());
        var comparison = sinceEnd.compareTo(granularity);
        if (comparison > 0)
        {
            // Time since end is greater than the granularity so add a new interval.
            // This represents that the vertex did not exist between the latestTemporalVertex.end and newInterval.start.
            temporalVertices.add(new TemporalVertex<>(new Interval(timestamp, timestamp), vertex));
        }
        else if (comparison == 0)
        {
            // Time since end is the granularity so extend the last interval.
            // This represents that the vertex continued existing for this interval.
            latestTemporalVertex.interval.setEnd(timestamp);
        }
        else
        {
            throw new IllegalArgumentException(String.format(
                "Timestamp `%s` is less than the granularity `%s` away from the latest interval end `%s` in the TemporalGraph",
                timestamp.toString(), granularity.toString(), latestTemporalVertex.interval.getEnd().toString()));
        }
    }

    /**
     * Gets a vertex at the given timestamp.
     * @param vertexId Id of the vertex.
     * @param timestamp Timestamp.
     */
    public V getVertex(String vertexId, LocalDate timestamp)
    {
        var temporalVertices = temporalVerticesById.get(vertexId);
        if (temporalVertices == null)
            throw new IllegalArgumentException(String.format("vertex %s does not exist", vertexId));

        for (var temporalVertex : temporalVertices)
        {
            if (temporalVertex.interval.contains(timestamp)) {
                return temporalVertex.vertex;
            }
        }

        throw new IllegalArgumentException(String.format("vertex %s does not exist at %s", vertexId, timestamp));
    }
    //endregion
}

package Infra;

import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.IsomorphicGraphMapping;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * A subset of IsomorphicGraphMapping that provides only the backwards vertex mapping.
 *
 * This class is purely for memory efficiency as we do not need to retain the references
 * to either the forwardMapping, or the graphs.
 */
public class BackwardVertexGraphMapping<V, E> implements GraphMapping<V, E>
{
    //region --[Fields: Private]---------------------------------------
    // TODO: change key type to also be string (PatternVertex id) [2021-02-24]
    /** Backward mapping from the second graph to the first one. **/
    private final Map<V, String> backwardMapping;

    /** Timestamp of mapping. */
    private final LocalDate timestamp;

    /** Temporal graph containing the vertices **/
    private final TemporalGraph<V> temporalGraph;
    //endregion

    //region --[Constructors]------------------------------------------
    /** Constructs a new BackwardVertexGraphMapping with a given IsomorphicGraphMapping. */
    public BackwardVertexGraphMapping(
        GraphMapping<V, E> mapping,
        LocalDate timestamp,
        TemporalGraph<V> temporalGraph)
    {
        if (!(mapping instanceof IsomorphicGraphMapping))
            throw new IllegalArgumentException("mapping is not an IsomorphicGraphMapping");

        this.timestamp = timestamp;
        this.temporalGraph = temporalGraph;

        backwardMapping = new HashMap<>();
        for (Map.Entry<V, V> entry : ((IsomorphicGraphMapping<V,E>)mapping).getBackwardMapping().entrySet())
        {
            // TODO: ensure V has to have a id getter [2021-02-24]
            backwardMapping.put(
                entry.getKey(),
                ((DataVertex)entry.getValue()).getVertexURI());
        }
    }
    //endregion

    //region --[GraphMapping]------------------------------------------
    @Override
    public V getVertexCorrespondence(V v, boolean forward) {
        if (forward)
            throw new UnsupportedOperationException("BackwardVertexGraphMapping does not support forward getVertexCorrespondence");

        return temporalGraph.getVertex(
            backwardMapping.get(v),
            timestamp);
    }

    @Override
    public E getEdgeCorrespondence(E e, boolean forward) {
        throw new UnsupportedOperationException("BackwardVertexGraphMapping does not support getEdgeCorrespondence");
    }
    //endregion
}

package infra;

import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.IsomorphicGraphMapping;

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
    /** Backward mapping from the second graph to the first one. **/
    private final Map<V, V> backwardMapping;
    //endregion

    //region --[Constructors]------------------------------------------
    /** Constructs a new BackwardVertexGraphMapping with a given IsomorphicGraphMapping. */
    public BackwardVertexGraphMapping(GraphMapping<V, E> mapping)
    {
        if (!(mapping instanceof IsomorphicGraphMapping))
            throw new IllegalArgumentException("mapping is not an IsomorphicGraphMapping");

        backwardMapping = ((IsomorphicGraphMapping)mapping).getBackwardMapping();
    }
    //endregion

    //region --[GraphMapping]------------------------------------------
    @Override
    public V getVertexCorrespondence(V v, boolean forward) {
        if (forward == true)
            throw new UnsupportedOperationException("BackwardVertexGraphMapping does not support forward getVertexCorrespondence");

        return backwardMapping.get(v);
    }

    @Override
    public E getEdgeCorrespondence(E e, boolean forward) {
        throw new UnsupportedOperationException("BackwardVertexGraphMapping does not support getEdgeCorrespondence");
    }
    //endregion
}

package Partitioner;

import graphLoader.GraphLoader;
import Infra.*;
import org.jgrapht.Graph;

import java.util.List;

public class GraphPartitionLoader extends GraphLoader {

    public GraphPartitionLoader(GraphLoader graphLoader, List<FocusNode> focusNodes) {
        super();
        Graph <Vertex, RelationshipEdge> subgraph= graphLoader.getGraph().getFragmentedGraph(focusNodes);
        VF2DataGraph fragmentedGraph=new VF2DataGraph(subgraph);
        setGraph(fragmentedGraph);
    }
}

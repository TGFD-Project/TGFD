package Partitioner;

import VF2BasedWorkload.Joblet;
import Loader.GraphLoader;
import Infra.*;
import org.jgrapht.Graph;

import java.util.List;

public class GraphPartitionLoader extends GraphLoader {

    public GraphPartitionLoader(GraphLoader graphLoader, List<Joblet> joblets) {
        super();
        Graph <Vertex, RelationshipEdge> subgraph= graphLoader.getGraph().getFragmentedGraph(joblets);
        VF2DataGraph fragmentedGraph=new VF2DataGraph(subgraph);
        setGraph(fragmentedGraph);
    }
}

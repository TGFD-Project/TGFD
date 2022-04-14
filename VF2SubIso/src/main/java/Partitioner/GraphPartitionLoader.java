package main.java.Partitioner;

import main.java.VF2BasedWorkload.Joblet;
import main.java.Loader.GraphLoader;
import main.java.Infra.*;
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

package Partitioner;

import Infra.DataVertex;
import Infra.VF2DataGraph;
import Infra.Vertex;

import java.util.HashMap;

public class RangeBasedPartitioner {

    private VF2DataGraph graph;

    public RangeBasedPartitioner(VF2DataGraph graph)
    {
        this.graph=graph;
    }

    public HashMap<DataVertex,Integer> fragment(int numberOfPartitions)
    {
        if(numberOfPartitions<1)
            return null;
        HashMap<DataVertex,Integer> mapping=new HashMap<>();
        int numberOfVertices=graph.getSize();
        int partitionSize=numberOfVertices/numberOfPartitions;
        partitionSize+=2;
        int i=0, partitionID=0;
        for (Vertex v:graph.getGraph().vertexSet()) {
            if(++i>partitionSize)
            {
                i=0;
                partitionID++;
            }
            mapping.put((DataVertex) v,partitionID);
        }
        return mapping;
    }

}
package Partitioner;

import graphLoader.GraphLoader;
import infra.VF2DataGraph;

import java.util.HashMap;

public class SimpleGraphPartitioner{

    private VF2DataGraph graph;


    public SimpleGraphPartitioner(VF2DataGraph graph)
    {
        this.graph=graph;
    }

    public HashMap<String,Integer> partition(int numberOfPartitions)
    {
        if(numberOfPartitions<1)
            return null;
        HashMap<String,Integer> mapping=new HashMap<>();
        int numberOfVertices=graph.getSize();
        int partitionSize=numberOfVertices/numberOfPartitions;
        partitionSize+=2;
        int i=0, partitionID=1;
        for (String vertexURI:graph.getNodeMap().keySet()) {
            if(++i>partitionSize)
            {
                i=0;
                partitionID++;
            }
            mapping.put(vertexURI,partitionID);
        }
        return mapping;
    }

}
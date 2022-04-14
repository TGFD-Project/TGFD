package Util;

import Infra.DataVertex;
import ICs.TGFD;
import Infra.VF2DataGraph;
import Infra.Vertex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CostEstimator {

    private VF2DataGraph graph;

    public CostEstimator(VF2DataGraph graph)
    {
        this.graph=graph;
    }

    public int computeSubgraphIsomorphismCost(TGFD tgfd)
    {
        String centerNodeType=tgfd.getPattern().getCenterVertexType();
        int patternSize=tgfd.getPattern().getSize();
        int cost=0;
        for (Vertex v:graph.getNodeMap().values()) {
            if(v.getTypes().contains(centerNodeType))
                cost+=patternSize;
        }
        return cost;
    }

    public HashMap<TGFD,Integer> computeSubgraphIsomorphismCost(List<TGFD> tgfds)
    {
        HashMap<TGFD, Integer> costs=new HashMap<>();
        for (TGFD tgfd:tgfds) {
            costs.put(tgfd, computeSubgraphIsomorphismCost(tgfd));
        }
        return costs;
    }

    public int computeCommunicationCost(TGFD tgfd, int assignedPartition, HashMap<String,Integer> partitionMapping)
    {
        String centerNodeType=tgfd.getPattern().getCenterVertexType();
        HashSet<String> nodesToRetrieve=new HashSet<>();
        for (String vertexURI:graph.getNodeMap().keySet()) {
            DataVertex data_v=(DataVertex) graph.getNode(vertexURI);
            if(data_v.getTypes().contains(centerNodeType)) {
                List<Vertex> withinDiameter=graph.getVerticesWithinDiameter(data_v,tgfd.getPattern().getDiameter());
                for (Vertex w:withinDiameter) {
                    String uri=((DataVertex) w).getVertexURI();
                    if(partitionMapping.containsKey(uri) && partitionMapping.get(uri)!=assignedPartition)
                        nodesToRetrieve.add(uri);
                }
            }
        }
        return nodesToRetrieve.size();
    }

}

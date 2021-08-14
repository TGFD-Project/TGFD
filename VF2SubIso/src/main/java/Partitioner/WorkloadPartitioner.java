package Partitioner;

import Infra.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WorkloadPartitioner {

    private VF2DataGraph graph;
    private List <TGFD> tgfds;
    private String centerNodeType;
    private int numberOfPartitions;
    private HashMap<Integer,ArrayList<FocusNode>> partitions;

    public WorkloadPartitioner(VF2DataGraph graph, List <TGFD> tgfds, String centerNodeType, int numberOfPartitiones)
    {
        this.graph=graph;
        this.tgfds=tgfds;
        this.centerNodeType=centerNodeType;
        this.numberOfPartitions=numberOfPartitiones;

        partition();
    }

    public String getPartition(int partitionNumber)
    {
        if(!partitions.containsKey(partitionNumber))
            return "";
        StringBuilder sb=new StringBuilder();
        for (FocusNode focusNode:partitions.get(partitionNumber)) {
            sb.append(focusNode.getNodeURI()).append("#").append(focusNode.getTGFDName()).append("#").append(focusNode.getDiameter()).append("\n");
        }
        return sb.toString();
    }

    private void partition()
    {
        HashMap<String,ArrayList<DataVertex>> allRelevantVertices=new HashMap <>();
        for (TGFD tgfd:tgfds) {
            allRelevantVertices.put(tgfd.getName(),new ArrayList <>());
            centerNodeType=tgfd.getPattern().getCenterVertexType();
            for (Vertex v:graph.getGraph().vertexSet()) {
                if(v.getTypes().contains(centerNodeType))
                    allRelevantVertices.get(tgfd.getName()).add((DataVertex) v);
            }
        }
        HashMap<String,Integer> TGFDsBySize=new HashMap <>();
        for (TGFD tgfd:tgfds) {
            TGFDsBySize.put(tgfd.getName(),tgfd.getPattern().getSize());
        }
        ArrayList<String> tgfdsSortedBySize=new ArrayList <>();
        while (true)
        {
            int patternSize=-1;
            String name="";
            for (String tgfdName:TGFDsBySize.keySet()) {
                if(TGFDsBySize.get(tgfdName)<patternSize)
                {
                    patternSize=TGFDsBySize.get(tgfdName);
                    name=tgfdName;
                }
            }
            if(patternSize!=-1)
            {
                tgfdsSortedBySize.add(name);
                TGFDsBySize.remove(name);
            }
            else
                break;
        }
        int currentPartition=1;
        partitions.put(currentPartition,new ArrayList <>());
        int totalNumberOfVertices= allRelevantVertices.values().stream().mapToInt(ArrayList::size).sum();
        int partitionSize= totalNumberOfVertices/numberOfPartitions;
        int j=0;
        for (String tgfdName:tgfdsSortedBySize) {
            TGFD currentTGFD = tgfds.stream().filter(tgfd -> tgfd.getName().equals(tgfdName)).findFirst().orElse(null);

            for (var i = 0; i < allRelevantVertices.get(tgfdName).size(); i++) {
                if (j < partitionSize) {
                    partitions.get(currentPartition).add(
                            new FocusNode(allRelevantVertices.get(tgfdName).get(i).getVertexURI(),currentTGFD.getName(),currentTGFD.getPattern().getDiameter()));
                } else {
                    j++;
                    currentPartition++;
                    partitions.put(currentPartition, new ArrayList <>());
                    partitions.get(currentPartition).add(
                            new FocusNode(allRelevantVertices.get(tgfdName).get(i).getVertexURI(),currentTGFD.getName(),currentTGFD.getPattern().getDiameter()));
                }
            }
        }
    }

}

package Workload;

import Partitioner.RangeBasedPartitioner;
import graphLoader.GraphLoader;
import infra.DataVertex;
import infra.RelationshipEdge;
import infra.TGFD;
import infra.Vertex;
import org.jgrapht.Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

public class WorkloadEstimator {

    private GraphLoader loader;
    private HashMap<DataVertex,Integer> fragments;
    private HashMap<String,ArrayList<Joblet>> jobletsByTGFD;
    private HashMap<Integer, ArrayList<Joblet>> jobletsByFragmentID;
    private int numberOfProcessors;

    public WorkloadEstimator(GraphLoader loader,int numberOfProcessors, HashMap<DataVertex,Integer> fragments)
    {
        this.loader = loader;
        this.fragments=fragments;
        this.numberOfProcessors=numberOfProcessors;
    }

    public WorkloadEstimator(GraphLoader loader, int numberOfProcessors)
    {
        this.loader = loader;
        this.numberOfProcessors=numberOfProcessors;
        RangeBasedPartitioner partitioner=new RangeBasedPartitioner(loader.getGraph());
        this.fragments=partitioner.fragment(numberOfProcessors);
    }

    public void defineJoblets(List<TGFD> tgfds)
    {
        jobletsByTGFD=new HashMap<>();
        jobletsByFragmentID= new HashMap<>();
        IntStream
                .rangeClosed(1, numberOfProcessors)
                .forEach(i -> jobletsByFragmentID.put(i, new ArrayList<>()));

        for (TGFD tgfd:tgfds) {
            jobletsByTGFD.put(tgfd.getName(),new ArrayList <>());
            String centerNodeType=tgfd.getPattern().getCenterVertexType();
            for (Vertex v: loader.getGraph().getGraph().vertexSet()) {
                if(v.getTypes().contains(centerNodeType))
                {
                    DataVertex dataVertex=(DataVertex) v;
                    Joblet joblet=new Joblet(dataVertex,tgfd.getName(),tgfd.getPattern().getDiameter(),fragments.get(dataVertex));
                    Graph<Vertex, RelationshipEdge> subgraph = loader.getGraph().getSubGraphWithinDiameter(dataVertex, tgfd.getPattern().getDiameter());
                    joblet.setSubgraph(subgraph);
                    jobletsByTGFD.get(tgfd.getName()).add(joblet);
                    jobletsByFragmentID.get(fragments.get(dataVertex)).add(joblet);
                }
            }
        }
    }

    public int communicationCost()
    {
        int count=0;
        for (int fragment:jobletsByFragmentID.keySet()) {
            count += jobletsByFragmentID
                    .get(fragment)
                    .stream()
                    .flatMap(joblet -> joblet
                            .getSubgraph()
                            .edgeSet()
                            .stream())
                    .filter(edge -> fragments.get((DataVertex) edge.getTarget()) != fragment || fragments.get((DataVertex) edge.getSource()) != fragment)
                    .count();
        }
        return count;
    }

}

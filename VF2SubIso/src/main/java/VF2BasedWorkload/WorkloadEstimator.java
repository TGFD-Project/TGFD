package VF2BasedWorkload;

import AmazonStorage.S3Storage;
import ICs.TGFD;
import Infra.*;
import Partitioner.RangeBasedPartitioner;
import ChangeExploration.*;
import Loader.GraphLoader;
import Util.Config;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public class WorkloadEstimator {

    private GraphLoader loader;
    private HashMap<DataVertex,Integer> fragments;
    private HashMap<DataVertex, HashSet<Integer>> copiedVertices;
    //private HashMap<String,ArrayList<Joblet>> jobletsByTGFD;
    private HashMap<Integer, ArrayList<Joblet>> jobletsByFragmentID;
    private HashMap<Integer, Joblet> jobletsByID;
    private int numberOfProcessors;

    public WorkloadEstimator(GraphLoader loader,int numberOfProcessors, HashMap<DataVertex,Integer> fragments)
    {
        this.loader = loader;
        this.fragments=fragments;
        this.numberOfProcessors=numberOfProcessors;
        copiedVertices=new HashMap<>();
    }

    public WorkloadEstimator(GraphLoader loader, int numberOfProcessors)
    {
        this.loader = loader;
        this.numberOfProcessors=numberOfProcessors;
        RangeBasedPartitioner partitioner=new RangeBasedPartitioner(loader.getGraph());
        this.fragments=partitioner.fragment(numberOfProcessors);
        copiedVertices=new HashMap<>();

    }

    public void defineJoblets(List<TGFD> tgfds)
    {
        //jobletsByTGFD=new HashMap<>();
        jobletsByID=new HashMap<>();
        jobletsByFragmentID= new HashMap<>();
        int jobletID=0;
        IntStream.range(0, numberOfProcessors)
                .forEach(i -> jobletsByFragmentID.put(i, new ArrayList<>()));

        for (TGFD tgfd:tgfds) {
            System.out.println("TGFD: " + tgfd.getName() + " with the center type: " + tgfd.getPattern().getCenterVertexType());
            //jobletsByTGFD.put(tgfd.getName(),new ArrayList <>());
            String centerNodeType=tgfd.getPattern().getCenterVertexType();
            for (Vertex v: loader.getGraph().getGraph().vertexSet()) {
                if(v.getTypes().contains(centerNodeType))
                {
                    jobletID++;
                    DataVertex dataVertex=(DataVertex) v;
                    Joblet joblet=new Joblet(jobletID,dataVertex,tgfd,tgfd.getPattern().getDiameter(),fragments.get(dataVertex));
                    ArrayList<RelationshipEdge> edges = loader.getGraph().getEdgesWithinDiameter(dataVertex, tgfd.getPattern().getDiameter());
                    joblet.setEdges(edges);
                    jobletsByID.put(jobletID,joblet);
                    //jobletsByTGFD.get(tgfd.getName()).add(joblet);
                    jobletsByFragmentID.get(fragments.get(dataVertex)).add(joblet);
                    if(jobletID%100==0)
                        System.out.println("Joblets so far: " + jobletID + "  **  " + LocalDateTime.now());
                }
            }
        }
    }

    public void partitionWorkload()
    {
        WorkloadPartitioner partitioner=new WorkloadPartitioner(this);
        this.jobletsByFragmentID =  partitioner.partition();
    }

    public int communicationCost()
    {
        System.out.println("Computing the data that needs to be shipped");
        int count=0;
        for (int fragment:jobletsByFragmentID.keySet()) {
            count += jobletsByFragmentID
                    .get(fragment)
                    .stream()
                    .flatMap(joblet -> joblet
                            .getEdges()
                            .stream())
                    .filter(edge -> fragments.get((DataVertex) edge.getTarget()) != fragment || fragments.get((DataVertex) edge.getSource()) != fragment)
                    .count();
        }
        return count;
    }

    public HashMap<Integer,HashMap<Integer,ArrayList<SimpleEdge>>> dataToBeShipped()
    {
        HashMap<Integer,HashMap<Integer,ArrayList<SimpleEdge>>> dataToBeShipped=new HashMap<>();
        for(int i:jobletsByFragmentID.keySet())
        {
            dataToBeShipped.put(i,new HashMap<>());
            for(int j:jobletsByFragmentID.keySet())
                dataToBeShipped.get(i).put(j,new ArrayList<>());
        }
        for (int fragmentID:jobletsByFragmentID.keySet()) {
            for (Joblet joblet :jobletsByFragmentID.get(fragmentID)) {
                for (RelationshipEdge edge:joblet.getEdges()) {
                    DataVertex srcVertex=(DataVertex) edge.getSource();
                    DataVertex dstVertex=(DataVertex) edge.getTarget();
                    if(fragments.get(srcVertex)!=fragmentID)
                    {
                        if(fragments.get(srcVertex).equals(fragments.get(dstVertex)))
                        {
                            dataToBeShipped.get(fragments.get(srcVertex))
                                    .get(fragmentID)
                                    .add(new SimpleEdge(edge));
                        }
                        else if(fragments.get(dstVertex)!=fragmentID)
                        {
                            dataToBeShipped.get(fragments.get(dstVertex))
                                    .get(fragmentID)
                                    .add(new SimpleEdge(edge));
                        }
                    }
                    else if(fragments.get(dstVertex)!=fragmentID)
                    {
                        dataToBeShipped.get(fragments.get(dstVertex))
                                .get(fragmentID)
                                .add(new SimpleEdge(edge));
                    }
                }
            }
        }
        return dataToBeShipped;
    }

    public HashMap<Integer,HashMap<Integer,ArrayList<SimpleEdge>>> dataToBeShipped(List<Change> changes)
    {
        HashMap<Integer,HashMap<Integer,ArrayList<SimpleEdge>>> dataToBeShipped=new HashMap<>();
        for(int i:jobletsByFragmentID.keySet())
        {
            dataToBeShipped.put(i,new HashMap<>());
            for(int j:jobletsByFragmentID.keySet())
                dataToBeShipped.get(i).put(j,new ArrayList<>());
        }
        for (Change change:changes) {
            if(change instanceof EdgeChange)
            {
                EdgeChange edgeChange=(EdgeChange) change;
                if(edgeChange.getTypeOfChange()== ChangeType.insertEdge)
                {
                    DataVertex src= (DataVertex) loader.getGraph().getNode(edgeChange.getSrc());
                    DataVertex dst= (DataVertex) loader.getGraph().getNode(edgeChange.getSrc());
                    if(!fragments.get(src).equals(fragments.get(dst)))
                    {
                        if(!src.getJobletID().isEmpty())
                        {
                            dataToBeShipped.get(fragments.get(dst))
                                    .get(fragments.get(src))
                                    .add(new SimpleEdge(src.getVertexURI(),dst.getVertexURI(),edgeChange.getLabel()));
                            if(!dst.getJobletID().isEmpty())
                            {
                                if(!copiedVertices.containsKey(dst))
                                    copiedVertices.put(dst,new HashSet<>());
                                copiedVertices.get(dst).add(fragments.get(src));
                                change.addJobletID(dst.getJobletID());
                            }
                        }
                        if(!dst.getJobletID().isEmpty())
                        {
                            dataToBeShipped.get(fragments.get(src))
                                    .get(fragments.get(dst))
                                    .add(new SimpleEdge(src.getVertexURI(),dst.getVertexURI(),edgeChange.getLabel()));
                            if(!src.getJobletID().isEmpty())
                            {
                                if(!copiedVertices.containsKey(src))
                                    copiedVertices.put(src,new HashSet<>());
                                copiedVertices.get(src).add(fragments.get(dst));
                                change.addJobletID(src.getJobletID());
                            }
                        }
                    }
                }
            }
        }
        return dataToBeShipped;
    }

    public HashMap<Integer,List<Change>> changesToBeSent(List<Change> changes)
    {
        HashMap<Integer,List<Change>> changesByFragmentID=new HashMap<>();

        for(int i:jobletsByFragmentID.keySet())
            changesByFragmentID.put(i,new ArrayList<>());

        for (Change change:changes) {
            if (change instanceof EdgeChange) {
                EdgeChange edgeChange = (EdgeChange) change;
                DataVertex src = (DataVertex) loader.getGraph().getNode(edgeChange.getSrc());
                DataVertex dst = (DataVertex) loader.getGraph().getNode(edgeChange.getSrc());
                if (!src.getJobletID().isEmpty())
                {
                    change.addJobletID(src.getJobletID());
                    changesByFragmentID.get(fragments.get(src)).add(change);
                }
                if (!dst.getJobletID().isEmpty())
                {
                    change.addJobletID(dst.getJobletID());
                    changesByFragmentID.get(fragments.get(dst)).add(change);
                }
                if(copiedVertices.containsKey(src))
                {
                    copiedVertices
                            .get(src)
                            .stream()
                            .mapToInt(f -> f)
                            .filter(f -> fragments.get(src) != f)
                            .forEach(f -> changesByFragmentID.get(f).add(change));
                }
                if(copiedVertices.containsKey(dst))
                {
                    copiedVertices
                            .get(dst)
                            .stream()
                            .mapToInt(f -> f)
                            .filter(f -> fragments.get(dst) != f)
                            .forEach(f -> changesByFragmentID.get(f).add(change));
                }
            }
            else if(change instanceof AttributeChange)
            {
                AttributeChange attributeChange = (AttributeChange) change;
                DataVertex vertex = (DataVertex) loader.getGraph().getNode(attributeChange.getUri());
                if (!vertex.getJobletID().isEmpty())
                {
                    change.addJobletID(vertex.getJobletID());
                    changesByFragmentID.get(fragments.get(vertex)).add(change);
                }
                if(copiedVertices.containsKey(vertex))
                {
                    copiedVertices
                            .get(vertex)
                            .stream()
                            .mapToInt(f -> f)
                            .filter(f -> fragments.get(vertex) != f)
                            .forEach(f -> {
                                changesByFragmentID.get(f).add(change);
                            } );
                }
            }
            else if(change instanceof VertexChange)
            {
                VertexChange vertexChange = (VertexChange) change;
                if(vertexChange.getTypeOfChange()==ChangeType.deleteVertex) {
                    if (!vertexChange.getVertex().getJobletID().isEmpty())
                    {
                        change.addJobletID(vertexChange.getVertex().getJobletID());
                        changesByFragmentID.get(fragments.get(vertexChange.getVertex())).add(change);
                    }
                    if(copiedVertices.containsKey(vertexChange.getVertex()))
                    {
                        copiedVertices
                                .get(vertexChange.getVertex())
                                .stream()
                                .mapToInt(f -> f)
                                .filter(f -> fragments.get(vertexChange.getVertex()) != f)
                                .forEach(f -> changesByFragmentID.get(f).add(change));
                    }
                }

            }
        }
        return changesByFragmentID;
    }

    public HashMap<Integer, String> sendChangesToWorkers(HashMap<Integer,List<Change>> changes, int snapshotID)
    {
        HashMap<Integer, String> listOfFiles=new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        String date=now.getHour() + "_" + now.getMinute() + "_" + now.getSecond();

        for (int id:changes.keySet()) {
            S3Storage.upload(Config.S3BucketName, date + "_Change[" + snapshotID + "]_" + id + ".ser", changes.get(id));
            listOfFiles.put(id, date + "_Change[" + snapshotID + "]_" + id + ".ser");
        }
        return listOfFiles;
    }

    public HashMap<Integer, ArrayList<String>> sendEdgesToWorkersForShipment(HashMap<Integer, HashMap<Integer,ArrayList<SimpleEdge>>> dataToBeShipped)
    {
        HashMap<Integer, ArrayList<String>> listOfFiles=new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        String date=now.getHour() + "_" + now.getMinute() + "_" + now.getSecond();

        for (int id:dataToBeShipped.keySet()) {
            StringBuilder sb = new StringBuilder();
            listOfFiles.put(id,new ArrayList<>());
            for (int key : dataToBeShipped.get(id).keySet()) {
                if (key != id) {
                    sb.append(key).append("\n");
                    for (SimpleEdge edge : dataToBeShipped.get(id).get(key))
                        sb.append(edge.getSrc()).append("\t").append(edge.getDst()).append("\n");
                    S3Storage.upload(Config.S3BucketName,date + "_F" + id + "_to_" +key + ".txt",sb.toString());
                    listOfFiles.get(id).add(date + "_F" + id + "_to_" +key + ".txt");
                    //saveEdges("./Fragment" + id + "_to_" +key + ".txt", sb);
                }
            }
        }
        return listOfFiles;
    }

    public double computeJobletsSize(int fragmentID)
    {
        return jobletsByFragmentID
                .get(fragmentID)
                .stream()
                .mapToDouble(Joblet::getSize)
                .sum();
    }

    public double computeTotalSize()
    {
        return jobletsByFragmentID
                .keySet()
                .stream()
                .mapToInt(fragmentID -> fragmentID)
                .mapToObj(fragmentID -> jobletsByFragmentID
                        .get(fragmentID)
                        .stream())
                .flatMap(Function.identity())
                .mapToDouble(Joblet::getSize)
                .sum();
    }

    public HashMap<Integer, ArrayList<Joblet>> getJobletsByFragmentID() {
        return jobletsByFragmentID;
    }

    public GraphLoader getLoader() {
        return loader;
    }

    public HashMap<DataVertex, Integer> getFragments() {
        return fragments;
    }

    public int getNumberOfProcessors() {
        return numberOfProcessors;
    }

    public void setJobletsByFragmentID(HashMap<Integer, ArrayList<Joblet>> jobletsByFragmentID) {
        this.jobletsByFragmentID = jobletsByFragmentID;
    }

    private void saveEdges(String path, StringBuilder stringBuilder)
    {
        try {
            FileWriter file = new FileWriter(path);
            file.write(stringBuilder.toString());
            file.close();
            System.out.println("Successfully wrote to the file: " + path);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}

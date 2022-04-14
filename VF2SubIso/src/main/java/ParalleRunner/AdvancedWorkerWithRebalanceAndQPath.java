package ParalleRunner;

import AmazonStorage.S3Storage;
import Infra.RelationshipEdge;
import Infra.SimpleEdge;
import Infra.Vertex;
import MPI.Consumer;
import MPI.Producer;
import Partitioner.Util;
import Util.Config;
import QPathBasedWorkload.JobRunner;
import ChangeExploration.Change;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class AdvancedWorkerWithRebalanceAndQPath {

    //region --[Fields: Private]---------------------------------------

    private String nodeName = "";
    private JobRunner runner;
    private String workingBucketName="";
    private HashMap<Integer, ArrayList<SimpleEdge>> dataToBeShipped;

    //endregion

    //region --[Constructor]-----------------------------------------

    public AdvancedWorkerWithRebalanceAndQPath()  {
        this.nodeName= Config.nodeName;
        workingBucketName = Config
                .getFirstDataFilePath()
                .get(0)
                .substring(0, Config.getFirstDataFilePath().get(0).lastIndexOf("/"));
    }

    //endregion

    //region --[Public Methods]-----------------------------------------

    public void start()
    {
        sendStatusToCoordinator();

        runner=new JobRunner();
        runner.load();

        runFirstSuperstep();

        for (int superstep =2; superstep<=Config.supersteps;superstep++)
        {
            runNextSuperstep(superstep);
        }

        System.out.println("All Done!");
    }

    //endregion

    //region --[Private Methods]-----------------------------------------

    private void runFirstSuperstep()
    {
        dataToBeShipped=new HashMap<>();
        boolean jobsRecieved=false, datashipper=false;
        Consumer consumer=new Consumer();
        consumer.connect(nodeName);

        while (!jobsRecieved || !datashipper)
        {
            String msg=consumer.receive();
            if (msg !=null) {
                if(msg.startsWith("#jobs"))
                {
                    runner.setJobsInRawString(msg);
                    System.out.println("The jobs have been received.");
                    jobsRecieved=true;
                }
                else if(msg.startsWith("#datashipper"))
                {
                    readEdgesToBeShipped(msg);
                    datashipper=true;
                }
            }
            else
                System.out.println("*JOB RECEIVER*: Error happened - message is null");
        }
        consumer.close();

        for (int workerID:dataToBeShipped.keySet()) {

            Graph<Vertex, RelationshipEdge> graphToBeSent = extractGraphToBeSent(workerID);

            LocalDateTime now = LocalDateTime.now();
            String date=now.getHour() + "_" + now.getMinute() + "_" + now.getSecond();
            String key=date + "_G_" + nodeName + "_to_" +Config.workers.get(workerID) + ".ser";
            S3Storage.upload(Config.S3BucketName,key,graphToBeSent);

            Producer messageProducer=new Producer();
            messageProducer.connect();
            messageProducer.send(Config.workers.get(workerID)+"_data",key);
            System.out.println("*DATA SENDER*: Graph object has been sent to '" + Config.workers.get(workerID) + "' successfully");
            messageProducer.close();
        }

        int receivedData=0;

        consumer=new Consumer();
        consumer.connect(nodeName+"_data");

        while (receivedData<Config.workers.size()-1)
        {
            System.out.println("*WORKER*: Start reading data from other workers...");
            String msg = consumer.receive();
            System.out.println("*WORKER*: Received a new message.");
            if (msg!=null) {
                Object obj=S3Storage.downloadObject(Config.S3BucketName,msg);
                if(obj!=null)
                {
                    Graph<Vertex, RelationshipEdge> receivedGraph =(Graph<Vertex, RelationshipEdge>) obj;
                    Util.mergeGraphs(runner.getLoader().getGraph(),receivedGraph);
                }
                else
                    System.out.println("*WORKER*: Object was null!");
            } else
                System.out.println("*WORKER*: Error happened - msg is null");
            receivedData++;
        }
        consumer.close();

        runner.generateJobs();
        runner.runTheFirstSnapshot();

        Producer messageProducer=new Producer();
        messageProducer.connect();
        messageProducer.send("results1",nodeName+"@Done.");
        System.out.println("*WORKER*: Superstep 1 is done successfully");
        messageProducer.close();
    }

    private void runNextSuperstep(int superstepNumber)
    {
        dataToBeShipped=new HashMap<>();
        boolean changesReceived=false, datashipper=false;
        List<Change> changes=new ArrayList<>();

        Consumer consumer=new Consumer();
        consumer.connect(nodeName);

        while (!changesReceived || !datashipper)
        {

            String msg=consumer.receive();
            if (msg !=null) {
                if(msg.startsWith("#change")) {
                    changes= (List<Change>) S3Storage.downloadObject(Config.S3BucketName,msg.split("\n")[1]);
                    System.out.println("List of changes have been received.");
                    changesReceived=true;
                }
                else if(msg.startsWith("#datashipper")){
                    readEdgesToBeShipped(msg);
                    datashipper=true;
                }
            }
            else
                System.out.println("Error happended in the first step of runNextSuperstep: "+superstepNumber+" - message is null");
        }

        consumer.close();

        for (int workerID:dataToBeShipped.keySet()) {

            Graph<Vertex, RelationshipEdge> graphToBeSent = extractGraphToBeSent(workerID);

            LocalDateTime now = LocalDateTime.now();
            String date=now.getHour() + "_" + now.getMinute() + "_" + now.getSecond();
            String key=date + "_G_" + nodeName + "_to_" +Config.workers.get(workerID) + ".ser";
            S3Storage.upload(Config.S3BucketName,key,graphToBeSent);

            Producer messageProducer=new Producer();
            messageProducer.connect();
            messageProducer.send(Config.workers.get(workerID)+"_data",key);
            System.out.println("*DATA SENDER*: Graph object has been sent to '" + Config.workers.get(workerID) + "' successfully");
            messageProducer.close();
        }

        int receivedData=0;

        consumer=new Consumer();
        consumer.connect(nodeName+"_data");

        while (receivedData<Config.workers.size()-1)
        {
            System.out.println("*WORKER*: Start reading data from other workers...");
            String msg = consumer.receive();
            System.out.println("*WORKER*: Received a new message.");
            if (msg!=null) {
                Object obj=S3Storage.downloadObject(Config.S3BucketName,msg);
                if(obj!=null)
                {
                    Graph<Vertex, RelationshipEdge> receivedGraph =(Graph<Vertex, RelationshipEdge>) obj;
                    Util.mergeGraphs(runner.getLoader().getGraph(),receivedGraph);
                }
                else
                    System.out.println("*WORKER*: Object was null!");
            } else
                System.out.println("*WORKER*: Error happened - msg is null");
            receivedData++;
        }
        consumer.close();

        runner.runTheNextTimestamp(changes, superstepNumber);

        Producer messageProducer=new Producer();
        messageProducer.connect();
        messageProducer.send("results"+superstepNumber,nodeName+"@Done.");
        System.out.println("*WORKER*: Superstep "+superstepNumber+" is done successfully");
        messageProducer.close();
    }

    private void readEdgesToBeShipped(String msg)
    {
        String []temp=msg.split("\n");
        for (int i=1;i<temp.length;i++)
        {
            StringBuilder sb = S3Storage.downloadWholeTextFile(Config.S3BucketName,temp[i]);
            String []arr = sb.toString().split("\n");
            int workerID=Integer.parseInt(arr[0]);
            if(!dataToBeShipped.containsKey(workerID))
                dataToBeShipped.put(workerID,new ArrayList<>());
            for (int j=1;j<arr.length;j++)
            {
                String []arr2=arr[j].split("\t");
                dataToBeShipped.get(workerID).add(new SimpleEdge(arr2[0],arr2[1],arr2[2]));
            }
        }
    }

    private Graph<Vertex, RelationshipEdge> extractGraphToBeSent(int workerID)
    {
        Graph<Vertex, RelationshipEdge> graphToBeSent = new DefaultDirectedGraph<>(RelationshipEdge.class);
        HashSet<String> visited=new HashSet<>();
        for (SimpleEdge edge:dataToBeShipped.get(workerID)) {
            Vertex src=null,dst=null;
            if(!visited.contains(edge.getSrc()))
            {
                src = runner.getLoader().getGraph().getNode(edge.getSrc());
                if(src!=null)
                {
                    graphToBeSent.addVertex(src);
                    visited.add(edge.getSrc());
                }
            }
            if(!visited.contains(edge.getDst()))
            {
                dst = runner.getLoader().getGraph().getNode(edge.getDst());
                if(dst!=null)
                {
                    graphToBeSent.addVertex(dst);
                    visited.add(edge.getDst());
                }
            }
            if(src!=null && dst!=null)
            {
                graphToBeSent.addEdge(src,dst,new RelationshipEdge(edge.getLabel()));
            }
        }
        return graphToBeSent;
    }

    private void sendStatusToCoordinator()
    {
        System.out.println("Worker '"+nodeName+"' is up and send status to the Coordinator");
        Producer producer=new Producer();
        producer.connect();
        producer.send("status","up " + nodeName);
        System.out.println("Status sent to the Coordinator successfully.");
        producer.close();
    }

    //endregion
}

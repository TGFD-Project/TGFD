package ParalleRunner;

import AmazonStorage.S3Storage;
import MPI.Consumer;
import MPI.Producer;
import MPI.Status;
import TGFDLoader.TGFDGenerator;
import infra.TGFD;
import infra.VF2DataGraph;
import org.apache.commons.lang3.RandomStringUtils;
import util.ConfigParser;
import util.testRunner;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

public class AdvancedWorker {

    private String nodeName = "";
    private boolean jobReceived=false;
    private boolean workerDone=false;
    private String job="";
    private HashMap<String, String> allJobs;
    private HashMap<String, List<TGFD>> otherWorkersJobs;
    private testRunner runner;
    private String workingBucketName="";

    public AdvancedWorker()  {
        this.nodeName=ConfigParser.nodeName;
        workingBucketName = ConfigParser
                .getFirstDataFilePath()
                .get(0)
                .substring(0,ConfigParser.getFirstDataFilePath().get(0).lastIndexOf("/"));
        allJobs=new HashMap<>();
        otherWorkersJobs=new HashMap<>();
    }

    public void start()
    {
        sendStatusToCoordiantor();

        Thread jobReceiverThread = new Thread(new JobReceiver());
        jobReceiverThread.setDaemon(false);
        jobReceiverThread.start();

        Thread jobRunnerThread = new Thread(new JobRunner());
        jobRunnerThread.setDaemon(false);
        jobRunnerThread.start();
    }

    public Status getStatus()
    {
        if(workerDone)
            return Status.Worker_Is_Done;
        if(!jobReceived)
            return Status.Worker_waits_For_Job;
        else
            return Status.Worker_Received_Job;
    }

    private void sendStatusToCoordiantor()
    {
        System.out.println("Worker '"+nodeName+"' is up and send status to the Coordinator");
        Producer producer=new Producer();
        producer.connect();
        producer.send("status","up " + nodeName);
        System.out.println("Status sent to the Coordinator successfully.");
        producer.close();
    }

    private class JobReceiver implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            Consumer consumer=new Consumer();
            consumer.connect(nodeName);
            String msg=consumer.receive();
            if (msg !=null) {
                String []temp=msg.split("#");
                Arrays.stream(temp)
                        .map(str -> str.split(","))
                        .filter(arr -> arr.length > 1)
                        .forEach(arr -> allJobs.put(arr[0], arr[1]));
                job=allJobs.get(nodeName);
                allJobs.remove(nodeName);
                ConfigParser.patternPath=job;
                jobReceived=true;
                System.out.println("The job has been received: " + job);
            }
            else
            {
                System.out.println("Error happened");
            }
            consumer.close();
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("JMS Exception occurred.  Shutting down client.");
        }
    }

    private class DataShipper implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            System.out.println("Jobs are recieved to be assigned to the workers");
            try {
                while(getStatus()!=Status.Worker_Received_Job) {
                    Thread.sleep(3000);
                    System.out.println("Worker '"+nodeName+"' has not received the job yet.");
                }

                runner=new testRunner();
                runner.load();

                Producer producer = new Producer();
                producer.connect();

                for (String workerName:allJobs.keySet()) {
                    TGFDGenerator generator = new TGFDGenerator(allJobs.get(workerName));
                    otherWorkersJobs.put(workerName,generator.getTGFDs());
                    VF2DataGraph graphToBeShipped=Partitioner.Util.getSubgraphToSendToOtherNodes(runner.getLoader().getGraph(), otherWorkersJobs.get(workerName));
                    String key = workerName + "_" + RandomStringUtils.randomAlphabetic(5) + ".ser";
                    boolean result = S3Storage.upload(workingBucketName,key,graphToBeShipped);
                    if(!result)
                        System.out.println("Error to upload the data for: " + workerName);
                    else
                    {
                        producer.send(workerName + "_shippedData",workingBucketName + "/" + key);
                        System.out.println("File path has been sent to worker: '"+workerName + "'");
                    }
                }
                producer.close();

                System.out.println("All files have been sent to other workers.");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onException(JMSException e) {
            System.out.println("JMS Exception occurred (JobAssigner).  Shutting down coordinator.");
        }
    }

    private class DataReceiver implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            Consumer consumer=new Consumer();
            consumer.connect(nodeName + "_shippedData");
            int numberOfFilesReceived=0;
            while (numberOfFilesReceived<allJobs.size())
            {
                String msg=consumer.receive();
                if (msg !=null) {
                    String bucketName=msg.substring(0,msg.lastIndexOf("/"));
                    String key=msg.substring(msg.lastIndexOf("/")+1);

                    Object obj=S3Storage.downloadObject(bucketName,key);
                    if(obj!=null)
                    {
                        VF2DataGraph ShippedData=(VF2DataGraph) obj;
                        Partitioner.Util.mergeGraphs(runner.getLoader().getGraph(),ShippedData);
                    }
                    System.out.println("The job has been received: " + job);
                }
                else
                {
                    System.out.println("Error happened");
                }
                numberOfFilesReceived++;
            }
            consumer.close();
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("JMS Exception occurred.  Shutting down client.");
        }
    }

    private class JobRunner implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            System.out.println("Jobs are recieved to be assigned to the workers");
            try {
                while(getStatus()!=Status.Worker_Received_Job) {
                    Thread.sleep(3000);
                    System.out.println("Worker '"+nodeName+"' has not received the job yet.");
                }

                ConfigParser.patternPath=job;

                runner=new testRunner();
                runner.load();
                String result= runner.run();

                Producer producer=new Producer();
                producer.connect();
                producer.send("results",nodeName + "@" + result);

                System.out.println("Results successfully sent to the coordinator");
                producer.close();

                workerDone=true;

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onException(JMSException e) {
            System.out.println("JMS Exception occurred (JobAssigner).  Shutting down coordinator.");
        }
    }

}

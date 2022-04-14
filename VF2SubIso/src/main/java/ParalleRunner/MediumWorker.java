package ParalleRunner;

import AmazonStorage.S3Storage;
import MPI.Consumer;
import MPI.Producer;
import Loader.TGFDGenerator;
import ICs.TGFD;
import Infra.VF2DataGraph;
import org.apache.commons.lang3.RandomStringUtils;
import Util.Config;
import Util.testRunner;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class MediumWorker {

    //region --[Fields: Private]---------------------------------------

    private String nodeName = "";
    private AtomicBoolean jobReceived =new AtomicBoolean(false);
    private AtomicBoolean dataShipping=new AtomicBoolean(false);
    private AtomicBoolean dataReceiving=new AtomicBoolean(false);
    private AtomicBoolean readyToRun=new AtomicBoolean(false);
    private AtomicBoolean workerDone=new AtomicBoolean(false);
    private String job="";
    private HashMap<String, String> allJobs;
    private HashMap<String, List<TGFD>> otherWorkersJobs;
    private testRunner runner;
    private String workingBucketName="";

    //endregion

    //region --[Constructor]-----------------------------------------

    public MediumWorker()  {
        this.nodeName= Config.nodeName;
        workingBucketName = Config
                .getFirstDataFilePath()
                .get(0)
                .substring(0, Config.getFirstDataFilePath().get(0).lastIndexOf("/"));
        allJobs=new HashMap<>();
        otherWorkersJobs=new HashMap<>();
    }

    //endregion

    //region --[Public Methods]-----------------------------------------

    public void start()
    {
        sendStatusToCoordinator();

        Thread jobReceiverThread = new Thread(new JobReceiver());
        jobReceiverThread.setDaemon(false);
        jobReceiverThread.start();

        Thread jobRunnerThread = new Thread(new JobRunner());
        jobRunnerThread.setDaemon(false);
        jobRunnerThread.start();

        Thread dataShipperThread = new Thread(new DataShipper());
        dataShipperThread.setDaemon(false);
        dataShipperThread.start();

        Thread dataReceiverThread = new Thread(new DataReceiver());
        dataReceiverThread.setDaemon(false);
        dataReceiverThread.start();
    }

    public Status getStatus()
    {
        if(workerDone.get())
            return Status.Worker_Is_Done;
        else if(jobReceived.get())
            return Status.Worker_Received_Job;
        else if(dataShipping.get())
            return Status.Worker_Shipping_Data;
        else if(dataReceiving.get())
            return Status.Worker_Receiving_Data;
        else if(readyToRun.get())
            return Status.Worker_Ready_To_Run;
        else
            return Status.Worker_waits_For_Job;
    }

    //endregion

    //region --[Private Methods]-----------------------------------------

    private void sendStatusToCoordinator()
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
                Config.patternPath=job;
                jobReceived.set(true);
                System.out.println("*JOB RECEIVER*: The job has been received: " + job);
            }
            else
            {
                System.out.println("*JOB RECEIVER*: Error happened - message is null");
            }
            consumer.close();
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("*JOB RECEIVER*: JMS Exception occurred. Shutting down worker.");
        }
    }

    private class DataShipper implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            try {
                while(getStatus()!=Status.Worker_Received_Job) {
                    sleep(Config.threadsIdleTime);
                    System.out.println("*DATA SHIPPER*: Worker '"+nodeName+"' has not received the job yet.");
                }

                System.out.println("*DATA SHIPPER*: Start shipping data.");

                jobReceived.set(false);
                dataShipping.set(true);

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
                        System.out.println("*DATA SHIPPER*: Error to upload the data for: " + workerName);
                    else
                    {
                        producer.send(workerName + "_shippedData",workingBucketName + "/" + key);
                        System.out.println("*DATA SHIPPER*: Uploaded successfully! File path has been sent to worker: '"+workerName + "'");
                    }
                }
                producer.close();

                System.out.println("*DATA SHIPPER*: All files have been sent to other workers.");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dataShipping.set(false);
            dataReceiving.set(true);
        }
        @Override
        public void onException(JMSException e) {
            System.out.println("*DATA SHIPPER*: JMS Exception occurred . Shutting down worker.");
        }
    }

    private class DataReceiver implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            try
            {
                while(getStatus()!=Status.Worker_Receiving_Data) {
                    sleep(Config.threadsIdleTime);
                    System.out.println("*DATA RECEIVER*: Worker '"+nodeName+"' has not received the job yet.");
                }

                System.out.println("*DATA RECEIVER*: Start Receiving data.");

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
                        System.out.println("*DATA RECEIVER*: Data '" + numberOfFilesReceived +"' has been received");
                    }
                    else
                    {
                        System.out.println("*DATA RECEIVER*: Error happened - message is null");
                    }
                    numberOfFilesReceived++;
                }
                System.out.println("*DATA RECEIVER*: All data has been received");
                consumer.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            dataReceiving.set(false);
            readyToRun.set(true);
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("*DATA RECEIVER*: JMS Exception occurred.  Shutting down worker.");
        }
    }

    private class JobRunner implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            try {
                while(getStatus()!=Status.Worker_Ready_To_Run) {
                    sleep(Config.threadsIdleTime);
                    System.out.println("*RUNNER*: Worker '"+nodeName+"' has not received the data yet to start.");
                }
                readyToRun.set(false);

                System.out.println("*RUNNER*: Start Running the TED algorithm.");

                String result= runner.run();

                Producer producer=new Producer();
                producer.connect();
                producer.send("results",nodeName + "@" + result);

                System.out.println("*RUNNER*: Results successfully sent to the worker");
                producer.close();

                workerDone.set(true);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onException(JMSException e) {
            System.out.println("*RUNNER*: JMS Exception occurred. Shutting down worker.");
        }
    }

    //endregion
}

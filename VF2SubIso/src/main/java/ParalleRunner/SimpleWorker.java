package ParalleRunner;

import MPI.Consumer;
import MPI.Producer;
import MPI.Status;
import util.ConfigParser;
import util.testRunner;

import javax.jms.*;

public class SimpleWorker {

    private String nodeName = "";
    private boolean jobReceived=false;
    private boolean workerDone=false;
    private String job="";

    public SimpleWorker()  {
        this.nodeName=ConfigParser.nodeName;
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
                job=msg;
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

                testRunner runner=new testRunner();
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
            System.out.println("JMS Exception occured (JobAssigner).  Shutting down coordinator.");
        }
    }

}

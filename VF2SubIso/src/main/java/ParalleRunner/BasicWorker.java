package ParalleRunner;

import MPI.Consumer;
import MPI.Producer;
import Util.Config;
import Util.testRunner;

import javax.jms.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BasicWorker {

    private String nodeName = "";
    private AtomicBoolean jobReceived=new AtomicBoolean(false);
    private AtomicBoolean workerDone=new AtomicBoolean(false);
    private String job="";

    public BasicWorker()  {
        this.nodeName= Config.nodeName;
    }

    public void start()
    {
        sendStatusToCoordinator();

        Thread jobReceiverThread = new Thread(new JobReceiver());
        jobReceiverThread.setDaemon(false);
        jobReceiverThread.start();

        Thread jobRunnerThread = new Thread(new JobRunner());
        jobRunnerThread.setDaemon(false);
        jobRunnerThread.start();
    }

    public Status getStatus()
    {
        if(workerDone.get())
            return Status.Worker_Is_Done;
        if(!jobReceived.get())
            return Status.Worker_waits_For_Job;
        else
            return Status.Worker_Received_Job;
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

    private class JobReceiver implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            Consumer consumer=new Consumer();
            consumer.connect(nodeName);
            String msg=consumer.receive();
            if (msg !=null) {
                job=msg;
                jobReceived.set(true);
                System.out.println("*JOB RECEIVER*: The job has been received: " + job);
            }
            else
            {
                System.out.println("*JOB RECEIVER*: Error happened. Message is null");
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
            try {
                while(getStatus()!=Status.Worker_Received_Job) {
                    Thread.sleep(Config.threadsIdleTime);
                    System.out.println("*JOB RUNNER*: Worker '"+nodeName+"' has not received the job yet.");
                }

                System.out.println("*JOB RUNNER*: Jobs are received. Starting the TED algorithm");

                Config.patternPath=job;

                testRunner runner=new testRunner();
                runner.load();
                String result= runner.run();

                Producer producer=new Producer();
                producer.connect();
                producer.send("results",nodeName + "@" + result);

                System.out.println("*JOB RUNNER*: Done. Results successfully sent to the coordinator");
                producer.close();

                workerDone.set(true);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onException(JMSException e) {
            System.out.println("*JOB RUNNER*: JMS Exception occurred. Shutting down worker.");
        }
    }

}

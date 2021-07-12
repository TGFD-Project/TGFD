package ParalleRunner;

import MPI.Consumer;
import MPI.Producer;
import Util.Config;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediumCoordinator {

    //region --[Fields: Private]---------------------------------------

    private String nodeName = "coordinator";

    private AtomicBoolean workersStatusChecker=new AtomicBoolean(true);
    private AtomicBoolean workersResultsChecker =new AtomicBoolean(false);
    private AtomicBoolean allDone=new AtomicBoolean(false);

    private HashMap<String,Boolean> workersStatus=new HashMap <>();
    private HashMap<String,String> results=new HashMap <>();

    //endregion

    //region --[Constructor]-----------------------------------------

    public MediumCoordinator()
    {
        for (String worker: Config.workers) {
            workersStatus.put(worker,false);
        }
    }

    //endregion

    //region --[Public Methods]-----------------------------------------

    public void start()
    {
        Thread setupThread = new Thread(new Setup());
        setupThread.setDaemon(false);
        setupThread.start();
    }

    public void stop()
    {
        this.workersStatusChecker.set(false);
        this.workersResultsChecker.set(false);
    }

    public void assignJob(HashMap<String,String> jobs)
    {
        Thread jobAssignerThread = new Thread(new JobAssigner(jobs));
        jobAssignerThread.setDaemon(false);
        jobAssignerThread.start();
    }

    public void waitForResults()
    {
        Thread ResultsGetterThread = new Thread(new ResultsGetter());
        ResultsGetterThread.setDaemon(false);
        ResultsGetterThread.start();
    }

    public HashMap<String,String> getResults()
    {
        if(getStatus()== Status.Coordinator_Is_Done)
            return results;
        else
            return null;
    }

    public Status getStatus()
    {
        if(workersStatusChecker.get())
            return Status.Coordinator_Waits_For_Workers_Status;
        else if(workersResultsChecker.get())
            return Status.Coordinator_Waits_For_Workers_Results;
        else if(allDone.get())
            return Status.Coordinator_Is_Done;
        else
            return Status.Coordinator_Assigns_jobs_To_Workers;
    }

    //endregion

    //region --[Private Methods]-----------------------------------------

    private class Setup implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            try {
                Consumer consumer=new Consumer();
                consumer.connect("status");

                while (workersStatusChecker.get()) {

                    System.out.println("*SETUP*: Listening for new messages to get workers' status...");
                    String msg = consumer.receive();
                    System.out.println("*SETUP*: Received a new message.");
                    if (msg!=null) {
                        if(msg.startsWith("up"))
                        {
                            String []temp=msg.split(" ");
                            if(temp.length==2)
                            {
                                String worker_name=temp[1];
                                if(workersStatus.containsKey(worker_name))
                                {
                                    System.out.println("*SETUP*: Status update: '" + worker_name + "' is up");
                                    workersStatus.put(worker_name,true);
                                }
                                else
                                {
                                    System.out.println("*SETUP*: Unable to find the worker name: '" + worker_name + "' in workers list. " +
                                            "Please update the list in the Config file.");
                                }
                            }
                            else
                                System.out.println("*SETUP*: Message corrupted: " + msg);
                        }
                        else
                            System.out.println("*SETUP*: Message corrupted: " + msg);
                    } else
                        System.out.println("*SETUP*: Error happened.");

                    boolean done=true;
                    for (Boolean worker_status:workersStatus.values()) {
                        if(!worker_status)
                        {
                            done=false;
                            break;
                        }
                    }
                    if(done)
                    {
                        System.out.println("*SETUP*: All workers are up and ready to start.");
                        workersStatusChecker.set(false);
                    }
                }
                consumer.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("JMS Exception occurred.  Shutting down coordinator.");
        }
    }

    private class JobAssigner implements Runnable, ExceptionListener
    {
        private HashMap<String,String> jobs;

        public JobAssigner(HashMap<String,String> jobs)
        {
            this.jobs=jobs;
        }

        @Override
        public void run() {
            System.out.println("*JOB ASSIGNER*: Jobs are received to be assigned to the workers");
            try {
                while(getStatus()==Status.Coordinator_Waits_For_Workers_Status) {
                    System.out.println("*JOB ASSIGNER*: Coordinator waits for these workers to be online: ");
                    for (String worker : workersStatus.keySet()) {
                        if (!workersStatus.get(worker))
                            System.out.print(worker + " - ");
                    }
                    Thread.sleep(Config.threadsIdleTime);
                }
                Producer messageProducer=new Producer();
                messageProducer.connect();
                StringBuilder message= new StringBuilder();
                for (String job:jobs.values()) {
                    message.append(job).append("#");
                }
                message = new StringBuilder(message.substring(0, message.length() - 1));
                for (String worker:jobs.keySet()) {

                    messageProducer.send(worker,message.toString());
                    System.out.println("*JOB ASSIGNER*: Job assigned to '" + worker + "' successfully");
                }
                messageProducer.close();
                System.out.println("*JOB ASSIGNER*: All jobs are assigned.");
                workersResultsChecker.set(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("*JOB ASSIGNER*: JMS Exception occurred (JobAssigner).  Shutting down coordinator.");
        }
    }

    private class ResultsGetter implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            System.out.println("*RESULTS GETTER*: Coordinator listens to get the results back from the workers");
            try {
                while(getStatus()==Status.Coordinator_Waits_For_Workers_Status) {
                    System.out.print("\n*RESULTS GETTER*: Coordinator waits for workers to be online: ");
                    for (String worker : workersStatus.keySet()) {
                        if (!workersStatus.get(worker))
                            System.out.print(worker + " - ");
                    }
                    System.out.println("\n");
                    Thread.sleep(Config.threadsIdleTime);
                }
                while(getStatus()==Status.Coordinator_Assigns_jobs_To_Workers) {
                    System.out.println("*RESULTS GETTER*: Coordinator waits to finish assigning the jobs");
                    Thread.sleep(Config.threadsIdleTime);
                }
                Consumer consumer=new Consumer();
                consumer.connect("results");

                while (workersResultsChecker.get()) {
                    System.out.println("*RESULTS GETTER*: Listening for new messages to get the results...");
                    String msg = consumer.receive();
                    System.out.println("*RESULTS GETTER*: Received a new message.");
                    if (msg!=null) {
                        String []temp=msg.split("@");
                        if(temp.length==2)
                        {
                            String worker_name=temp[0].toLowerCase();
                            if(workersStatus.containsKey(worker_name))
                            {
                                System.out.println("*RESULTS GETTER*: Results received from: '" + worker_name+"'");
                                results.put(worker_name,temp[1]);
                            }
                            else
                            {
                                System.out.println("*RESULTS GETTER*: Unable to find the worker name: '" + worker_name + "' in workers list. " +
                                        "Please update the list in the Config file.");
                            }
                        }
                        else
                        {
                            System.out.println("*RESULTS GETTER*: Message corrupted: " + msg);
                        }
                    }
                    else
                        System.out.println("*RESULTS GETTER*: Error happened. message is null");

                    boolean done=true;
                    for (String worker_name:workersStatus.keySet()) {
                        if(!results.containsKey(worker_name))
                        {
                            done=false;
                            break;
                        }
                    }
                    if(done)
                    {
                        System.out.println("*RESULTS GETTER*: All workers have sent the results.");
                        workersResultsChecker.set(false);
                        allDone.set(true);
                    }
                }
                consumer.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("*RESULTS GETTER*: JMS Exception occurred. Shutting down coordinator.");
        }
    }

    //endregion

}

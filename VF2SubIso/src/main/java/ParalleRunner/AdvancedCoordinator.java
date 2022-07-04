package ParalleRunner;

import Infra.SimpleEdge;
import Loader.GraphLoader;
import Loader.SimpleDBPediaLoader;
import Loader.SimpleIMDBLoader;
import ICs.TGFD;
import MPI.Consumer;
import MPI.Producer;
import Loader.TGFDGenerator;
import Util.Config;
import VF2BasedWorkload.Joblet;
import VF2BasedWorkload.WorkloadEstimator;
import ChangeExploration.Change;
import ChangeExploration.ChangeLoader;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AdvancedCoordinator {

    //region --[Fields: Private]---------------------------------------

    private String nodeName = "coordinator";

    private GraphLoader loader=null;
    private List<TGFD> tgfds;

    private WorkloadEstimator estimator=null;

    private AtomicBoolean workersStatusChecker=new AtomicBoolean(true);
    private AtomicBoolean workersResultsChecker =new AtomicBoolean(false);
    private AtomicBoolean allDone=new AtomicBoolean(false);

    private HashMap<Integer, HashMap<Integer, ArrayList<String>>> edgesToBeShippedToOtherWorkers;
    private HashMap<Integer, HashMap<Integer, String>> changesToBeSentToOtherWorkers;

    private HashMap<String,Boolean> workersStatus=new HashMap <>();
    private HashMap<String,List<String>> results=new HashMap <>();

    private AtomicInteger superstep=new AtomicInteger(0);

    //endregion

    //region --[Constructor]-----------------------------------------

    public AdvancedCoordinator()
    {
        for (String worker: Config.workers) {
            workersStatus.put(worker,false);
        }
        edgesToBeShippedToOtherWorkers=new HashMap<>();
        changesToBeSentToOtherWorkers=new HashMap<>();
    }

    //endregion

    //region --[Public Methods]-----------------------------------------

    public void start()
    {
        loadTheWorkload();

        Thread setupThread = new Thread(new Setup());
        setupThread.setDaemon(false);
        setupThread.start();

        Thread dataAndChangeFilesGeneratorThread = new Thread(new ShippedDataGenerator());
        dataAndChangeFilesGeneratorThread.setDaemon(false);
        dataAndChangeFilesGeneratorThread.start();
    }

    public void stop()
    {
        this.workersStatusChecker.set(false);
        this.workersResultsChecker.set(false);
    }

    public void assignJoblets()
    {
        Thread jobAssignerThread = new Thread(new JobletAssigner());
        jobAssignerThread.setDaemon(false);
        jobAssignerThread.start();

        Thread dataShipperThread = new Thread(new DataShipper());
        dataShipperThread.setDaemon(false);
        dataShipperThread.start();
    }

    public void waitForResults()
    {
        Thread ResultsGetterThread = new Thread(new ResultsGetter());
        ResultsGetterThread.setDaemon(false);
        ResultsGetterThread.start();
    }

    public HashMap<String,List<String>> getResults()
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

    private void loadTheWorkload()
    {
        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        tgfds=generator.getTGFDs();

        if(Config.datasetName== Config.dataset.dbpedia)
            loader = new SimpleDBPediaLoader(tgfds, Config.getFirstTypesFilePath(), Config.getFirstDataFilePath());
        else if(Config.datasetName== Config.dataset.synthetic) {
            //loader = new SyntheticLoader(tgfds, Config.getFirstDataFilePath());
        }
        else if(Config.datasetName== Config.dataset.imdb) // default is imdb
            loader = new SimpleIMDBLoader(tgfds, Config.getFirstDataFilePath());


        System.out.println("Number of edges: " + loader.getGraph().getGraph().edgeSet().size());

        estimator=new WorkloadEstimator(loader,Config.workers.size());
        estimator.defineJoblets(generator.getTGFDs());
        estimator.partitionWorkload();
        //System.out.println("Number of edges to be shipped: " + estimator.communicationCost());
        HashMap<Integer, HashMap<Integer, ArrayList<SimpleEdge>>> dataToBeShipped = estimator.dataToBeShipped();
        HashMap<Integer, ArrayList<String>> filesOnS3Storage = estimator.sendEdgesToWorkersForShipment(dataToBeShipped);
        edgesToBeShippedToOtherWorkers.put(1,filesOnS3Storage);
    }

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

    private class JobletAssigner implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            System.out.println("*JOBLET ASSIGNER*: Joblets are received to be assigned to the workers");
            try {
                while(getStatus()==Status.Coordinator_Waits_For_Workers_Status) {
                    System.out.println("*JOBLET ASSIGNER*: Coordinator waits for these workers to be online: ");
                    for (String worker : workersStatus.keySet()) {
                        if (!workersStatus.get(worker))
                            System.out.print(worker + " - ");
                    }
                    Thread.sleep(Config.threadsIdleTime);
                }
                Producer messageProducer=new Producer();
                messageProducer.connect();
                StringBuilder message;
                for (int workerID:estimator.getJobletsByFragmentID().keySet()) {
                    message= new StringBuilder();
                    message.append("#joblets").append("\n");
                    for (Joblet joblet:estimator.getJobletsByFragmentID().get(workerID)) {
                        message.append(joblet.getId()).append("#")
                                .append(joblet.getCenterNode()).append("#")
                                .append(joblet.getTGFD().getName())
                                .append("\n");
                    }
                    messageProducer.send(Config.workers.get(workerID),message.toString());
                    System.out.println("*JOBLET ASSIGNER*: joblets assigned to '" + Config.workers.get(workerID) + "' successfully");
                }
                messageProducer.close();
                System.out.println("*JOBLET ASSIGNER*: All joblets are assigned.");
                superstep.set(1);
                workersResultsChecker.set(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("*JOBLET ASSIGNER*: JMS Exception occurred (JobletAssigner).  Shutting down coordinator.");
        }
    }

    private class DataShipper implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            System.out.println("*JOBLET ASSIGNER*: Joblets are received to be assigned to the workers");
            try {
                while (true)
                {
                    int currentSuperstep=superstep.get();
                    while(!edgesToBeShippedToOtherWorkers.containsKey(currentSuperstep)) {
                        System.out.println("*DataShipper*: Wait for the new superstep: ");
                        Thread.sleep(Config.threadsIdleTime);
                        currentSuperstep=superstep.get();
                    }
                    Producer messageProducer=new Producer();
                    messageProducer.connect();
                    StringBuilder message;
                    for (int workerID:edgesToBeShippedToOtherWorkers.get(currentSuperstep).keySet()) {
                        message= new StringBuilder();
                        message.append("#datashipper").append("\n");
                        for (String path:edgesToBeShippedToOtherWorkers.get(currentSuperstep).get(workerID)) {
                            message.append(path).append("\n");
                        }
                        messageProducer.send(Config.workers.get(workerID),message.toString());
                        System.out.println("*DataShipper*: Shipping files have been shared with '" + Config.workers.get(workerID) + "' successfully");
                    }

                    if(changesToBeSentToOtherWorkers.containsKey(currentSuperstep))
                    {
                        for (int workerID:changesToBeSentToOtherWorkers.get(currentSuperstep).keySet()) {
                            message= new StringBuilder();
                            message.append("#change").append("\n").append(changesToBeSentToOtherWorkers.get(currentSuperstep).get(workerID));
                            messageProducer.send(Config.workers.get(workerID),message.toString());
                            System.out.println("*DataShipper*: Change objects have been shared with '" + Config.workers.get(workerID) + "' successfully");
                        }
                    }
                    messageProducer.close();
                    System.out.println("*DataShipper*: All files are shared for the superstep: " + currentSuperstep);
                    edgesToBeShippedToOtherWorkers.remove(currentSuperstep);
                    changesToBeSentToOtherWorkers.remove(currentSuperstep);
                    if(currentSuperstep==Config.getAllDataPaths().keySet().size()+1)
                        return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("*DataShipper: JMS Exception occurred (DataShipper).  Shutting down coordinator.");
        }
    }

    private class ShippedDataGenerator implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            System.out.println("*DATA NEEDS TO BE SHIPPED*: Generating files to upload to S3 to send to workers later");
            try {
                Object[] ids= Config.getDiffFilesPath().keySet().toArray();
                Arrays.sort(ids);
                for (int i=0;i<ids.length;i++) {
                    System.out.println("*DATA NEEDS TO BE SHIPPED*: Generating files for snapshot (" + ids[i] + ")");

                    ChangeLoader changeLoader = new ChangeLoader(Config.getDiffFilesPath().get(ids[i]));
                    List<Change> changes = changeLoader.getAllChanges();

                    HashMap<Integer,HashMap<Integer,ArrayList<SimpleEdge>>> dataToBeShipped=estimator.dataToBeShipped(changes);
                    HashMap<Integer, ArrayList<String>> filesOnS3Storage = estimator.sendEdgesToWorkersForShipment(dataToBeShipped);

                    HashMap<Integer,List<Change>> changesToBeSent=estimator.changesToBeSent(changes);
                    HashMap<Integer, String> changesOnS3Storage = estimator.sendChangesToWorkers(changesToBeSent,i+2);

                    changesToBeSentToOtherWorkers.put(i+2,changesOnS3Storage);
                    edgesToBeShippedToOtherWorkers.put(i+2,filesOnS3Storage);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("*DataShipper: JMS Exception occurred (DataShipper).  Shutting down coordinator.");
        }
    }

    private class ResultsGetter implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            System.out.println("*RESULTS GETTER*: Coordinator listens to get the results back from the workers");
            for (String worker_name : workersStatus.keySet()) {
                if (!results.containsKey(worker_name))
                    results.put(worker_name, new ArrayList<>());
            }
            try {
                while (true) {
                    Consumer consumer = new Consumer();
                    consumer.connect("results"+superstep.get());

                    System.out.println("*RESULTS GETTER*: Listening for new messages to get the results...");
                    String msg = consumer.receive();
                    System.out.println("*RESULTS GETTER*: Received a new message.");
                    if (msg != null) {
                        String[] temp = msg.split("@");
                        if (temp.length == 2) {
                            String worker_name = temp[0].toLowerCase();
                            if (workersStatus.containsKey(worker_name)) {
                                System.out.println("*RESULTS GETTER*: Results received from: '" + worker_name + "'");
                                results.get(worker_name).add(temp[1]);
                            } else {
                                System.out.println("*RESULTS GETTER*: Unable to find the worker name: '" + worker_name + "' in workers list. " +
                                        "Please update the list in the Config file.");
                            }
                        } else {
                            System.out.println("*RESULTS GETTER*: Message corrupted: " + msg);
                        }
                    } else
                        System.out.println("*RESULTS GETTER*: Error happened. message is null");

                    boolean done = true;
                    for (String worker_name : workersStatus.keySet()) {
                        if (results.get(worker_name).size() != superstep.get()) {
                            done = false;
                            break;
                        }
                    }
                    if (done) {
                        System.out.println("*RESULTS GETTER*: All workers have sent the results for superstep: " + superstep.get());
                        if(superstep.get() > Config.getDiffFilesPath().keySet().size())
                        {
                            System.out.println("*RESULTS GETTER*: All done! No superstep remained.");
                            superstep.set(superstep.get() + 1);
                            allDone.set(true);
                            break;
                        }
                        else
                        {
                            superstep.set(superstep.get() + 1);
                            System.out.println("*RESULTS GETTER*: Starting the new superstep! -> " + superstep.get() );
                        }
                    }
                    consumer.close();
                }
            } catch (Exception e) {
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

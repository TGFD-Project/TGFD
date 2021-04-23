package MPI;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import util.ConfigParser;

import javax.jms.*;
import java.util.HashMap;

public class Coordinator {

    // default broker URL is : tcp://localhost:61616"
    private String url = ActiveMQConnection.DEFAULT_BROKER_URL;

    private String nodeName = "coordinator";

    private boolean workersStatusChecker =true;
    private boolean workersResultsChecker =false;
    private boolean allDone=false;

    private HashMap<String,Boolean> workersStatus=new HashMap <>();

    private HashMap<String,String> results=new HashMap <>();

    public Coordinator()
    {
        for (String worker:ConfigParser.workers) {
            workersStatus.put(worker,false);
        }
        this.url= ConfigParser.ActiveMQBrokerURL;
    }

    public void start()
    {
        Thread consumerThread = new Thread(new Setup());
        consumerThread.setDaemon(false);
        consumerThread.start();
    }

    public void stop()
    {
        this.workersStatusChecker =false;
        workersResultsChecker=false;
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
        if(getStatus()==Status.Coordinator_Is_Done)
            return results;
        else
            return null;
    }


    public Status getStatus()
    {
        if(workersStatusChecker)
            return Status.Coordinator_Waits_For_Workers_Status;
        else if(workersResultsChecker)
            return Status.Coordinator_Waits_For_Workers_Results;
        else if(allDone)
            return Status.Coordinator_Is_Done;
        else
            return Status.Coordinator_Assigns_jobs_To_Workers;
    }

    private class Setup implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            Connection connection=null;
            MessageConsumer consumer=null;
            try {
                final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
                connectionFactory.setUserName(ConfigParser.ActiveMQUsername);
                connectionFactory.setPassword(ConfigParser.ActiveMQPassword);
                connection = connectionFactory.createConnection();
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue("status");
                consumer = session.createConsumer(destination);

                while (workersStatusChecker) {

                    System.out.println("Listening for new messages to get workers' status...");
                    Message message = consumer.receive();
                    System.out.println("Recieved a new message.");
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        if(textMessage.getText().startsWith("up"))
                        {
                            String []temp=textMessage.getText().split(" ");
                            if(temp.length==2)
                            {
                                String worker_name=temp[1];
                                if(workersStatus.containsKey(worker_name))
                                {
                                    System.out.println("Status update: '" + worker_name + "' is up");
                                    workersStatus.put(worker_name,true);
                                }
                                else
                                {
                                    System.out.println("Unable to find the worker name: '" + worker_name + "' in workers list. " +
                                            "Please update the list in the Config file.");
                                }
                            }
                            else
                                System.out.println("Message corrupted: " + textMessage.getText());
                        }
                        else
                            System.out.println("Message corrupted: " + textMessage.getText());
                    } else
                        System.out.println("No message so far.");

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
                        System.out.println("All workers are up and ready to start.");
                        workersStatusChecker =false;
                    }
                }
                connection.close();
                consumer.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("JMS Exception occured.  Shutting down coordinator.");
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
            System.out.println("Jobs are recieved to be assigned to the workers");
            try {
                while(getStatus()==Status.Coordinator_Waits_For_Workers_Status) {
                    System.out.println("Coordinator waits for these workers to be online: ");
                    for (String worker : workersStatus.keySet()) {
                        if (!workersStatus.get(worker))
                            System.out.print(worker + " - ");
                    }
                    Thread.sleep(3000);
                }
                final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
                connectionFactory.setUserName(ConfigParser.ActiveMQUsername);
                connectionFactory.setPassword(ConfigParser.ActiveMQPassword);
                Connection connection = connectionFactory.createConnection();
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                for (String worker:jobs.keySet()) {

                    Destination destination = session.createQueue(worker);

                    MessageProducer producer = session.createProducer(destination);
                    TextMessage message = session.createTextMessage(jobs.get(worker));
                    producer.send(message);
                    System.out.println("Job assigned to '" + worker + "' successfully");
                    producer.close();
                }
                connection.close();
                System.out.println("All jobs are assigned.");
                workersResultsChecker=true;
            } catch (InterruptedException | JMSException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("JMS Exception occured (JobAssigner).  Shutting down coordinator.");
        }
    }

    private class ResultsGetter implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            System.out.println("Coordinator listens to get the results back from the workers");
            Connection connection=null;
            MessageConsumer consumer=null;
            try {
                while(getStatus()==Status.Coordinator_Waits_For_Workers_Status) {
                    System.out.print("\nCoordinator waits for workers to be online: ");
                    for (String worker : workersStatus.keySet()) {
                        if (!workersStatus.get(worker))
                            System.out.print(worker + " - ");
                    }
                    System.out.println("\n");
                    Thread.sleep(3000);
                }
                while(getStatus()==Status.Coordinator_Assigns_jobs_To_Workers) {
                    System.out.println("Coordinator waits to finish assigning the jobs");
                    Thread.sleep(3000);
                }
                final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
                connectionFactory.setUserName(ConfigParser.ActiveMQUsername);
                connectionFactory.setPassword(ConfigParser.ActiveMQPassword);
                connection = connectionFactory.createConnection();
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue("results");
                consumer = session.createConsumer(destination);

                while (workersResultsChecker) {

                    System.out.println("Listening for new messages to get the results...");
                    Message message = consumer.receive();
                    System.out.println("Recieved a new message.");
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        if(textMessage.getText().startsWith("result"))
                        {
                            String []temp=textMessage.getText().split("@");
                            if(temp.length==2)
                            {
                                String worker_name=temp[1];
                                if(workersStatus.containsKey(worker_name))
                                {
                                    System.out.println("Results received from: '" + worker_name+"'");
                                    results.put(worker_name,temp[1]);
                                }
                                else
                                {
                                    System.out.println("Unable to find the worker name: '" + worker_name + "' in workers list. " +
                                            "Please update the list in the Config file.");
                                }
                            }
                            else
                                System.out.println("Message corrupted: " + textMessage.getText());
                        }
                        else
                            System.out.println("Message corrupted: " + textMessage.getText());
                    } else
                        System.out.println("No message so far.");

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
                        System.out.println("All workers have sent the results.");
                        workersResultsChecker =false;
                        allDone=true;
                    }
                }
                connection.close();
                consumer.close();
            } catch (InterruptedException | JMSException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("JMS Exception occured (JobAssigner).  Shutting down coordinator.");
        }
    }

}

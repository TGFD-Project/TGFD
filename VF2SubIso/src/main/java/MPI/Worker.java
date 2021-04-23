package MPI;

import org.apache.activemq.ActiveMQConnectionFactory;
import util.ConfigParser;
import util.testRunner;

import javax.jms.*;

public class Worker {

    private String url;
    private String nodeName = "";
    private boolean jobReceived=false;
    private boolean workerDone=false;
    private String job="";

    public Worker()  {
        this.url=ConfigParser.ActiveMQBrokerURL;
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
        try {
            final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
            connectionFactory.setUserName(ConfigParser.ActiveMQUsername);
            connectionFactory.setPassword(ConfigParser.ActiveMQPassword);
            Connection connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Destination destination = session.createQueue("status");

            MessageProducer producer = session.createProducer(destination);
            TextMessage message = session.createTextMessage("up " + nodeName);
            producer.send(message);
            System.out.println("Status sent to the Coordiantor successfully.");
            producer.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private class JobReceiver implements Runnable, ExceptionListener
    {
        @Override
        public void run() {
            try {
                // Getting JMS connection from the server
                final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
                connectionFactory.setUserName(ConfigParser.ActiveMQUsername);
                connectionFactory.setPassword(ConfigParser.ActiveMQPassword);
                Connection connection = connectionFactory.createConnection();
                connection.start();
                Session session = connection.createSession(false,
                        Session.AUTO_ACKNOWLEDGE);

                Destination destination = session.createQueue(nodeName);
                MessageConsumer consumer = session.createConsumer(destination);

                System.out.println("Listening for the new job to be assigned.");
                Message message = consumer.receive();

                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    job=textMessage.getText();
                    jobReceived=true;
                    System.out.println("The job has been received: " + job);
                } else
                    System.out.println("No message so far.");
                //Thread.sleep(1000);
                connection.close();
            }//| InterruptedException
            catch (JMSException  e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(JMSException e) {
            System.out.println("JMS Exception occured.  Shutting down client.");
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

                final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
                connectionFactory.setUserName(ConfigParser.ActiveMQUsername);
                connectionFactory.setPassword(ConfigParser.ActiveMQPassword);
                Connection connection = connectionFactory.createConnection();
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue("results");

                MessageProducer producer = session.createProducer(destination);
                TextMessage message = session.createTextMessage(nodeName + "@" + result);
                producer.send(message);
                System.out.println("Results successfully sent to the coordinator");
                producer.close();
                connection.close();
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

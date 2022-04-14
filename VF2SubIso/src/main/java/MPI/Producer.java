package MPI;

import org.apache.activemq.ActiveMQConnectionFactory;
import Util.Config;

import javax.jms.*;

public class Producer {

    private final String url;
    private Connection connection=null;
    private Session session=null;

    public Producer()
    {
        url= Config.ActiveMQBrokerURL;
    }

    public void connect()
    {
        try
        {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
            connectionFactory.setUserName(Config.ActiveMQUsername);
            connectionFactory.setPassword(Config.ActiveMQPassword);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public void send(String dstQueue, String msg)
    {
        try
        {
            if(connection==null || session ==null) {
                System.out.println("Connection is not established to the broker...");
                return;
            }

            Destination destination = session.createQueue(dstQueue);
            MessageProducer producer = session.createProducer(destination);
            TextMessage message = session.createTextMessage(msg);
            producer.send(message);
            System.out.println("Message sent to '" + dstQueue + "' successfully");
            producer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void close()
    {
        try
        {
            session.close();
            connection.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}

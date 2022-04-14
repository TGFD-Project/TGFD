package MPI;

import org.apache.activemq.ActiveMQConnectionFactory;
import Util.Config;

import javax.jms.*;

public class Consumer {

    private final String url;
    private Connection connection=null;
    private Session session=null;
    private MessageConsumer consumer=null;

    public Consumer()
    {
        url= Config.ActiveMQBrokerURL;
    }

    public void connect(String queueName)
    {
        try
        {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
            connectionFactory.setUserName(Config.ActiveMQUsername);
            connectionFactory.setPassword(Config.ActiveMQPassword);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(queueName);
            consumer = session.createConsumer(destination);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public String receive()
    {
        try
        {
            if(connection==null || session ==null) {
                System.out.println("Connection is not established to the broker...");
                return null;
            }
            Message message = consumer.receive();
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                return textMessage.getText();
            }
            else
            {
                System.out.println("Message received, but not an instance of text message.");
                return null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }


    public void close()
    {
        try
        {
            consumer.close();
            session.close();
            connection.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}

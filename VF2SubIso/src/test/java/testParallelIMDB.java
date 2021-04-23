import MPI.Coordinator;
import MPI.Worker;
import util.ConfigParser;

import java.io.FileNotFoundException;

public class testParallelIMDB {

    public static void main(String []args) throws FileNotFoundException {
        ConfigParser.parse(args[0]);
        if(ConfigParser.nodeName.equalsIgnoreCase("coordinator"))
        {

            Coordinator coordinator=new Coordinator();
            coordinator.start();
            coordinator.assignJob(ConfigParser.jobs);
            coordinator.waitForResults();
        }
        else
        {
            System.out.println("Worker '"+ConfigParser.nodeName+"' is starting...");
            Worker worker=new Worker();
            worker.start();
        }
    }
}

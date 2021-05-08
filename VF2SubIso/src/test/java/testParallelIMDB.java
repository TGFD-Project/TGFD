import ParalleRunner.SimpleCoordinator;
import ParalleRunner.SimpleWorker;
import util.ConfigParser;

import java.io.FileNotFoundException;

public class testParallelIMDB {

    public static void main(String []args) throws FileNotFoundException {
        ConfigParser.parse(args[0]);
        if(ConfigParser.nodeName.equalsIgnoreCase("coordinator"))
        {

            SimpleCoordinator simpleCoordinator =new SimpleCoordinator();
            simpleCoordinator.start();
            simpleCoordinator.assignJob(ConfigParser.jobs);
            simpleCoordinator.waitForResults();
        }
        else
        {
            System.out.println("Worker '"+ConfigParser.nodeName+"' is starting...");
            SimpleWorker worker=new SimpleWorker();
            worker.start();
        }
    }
}

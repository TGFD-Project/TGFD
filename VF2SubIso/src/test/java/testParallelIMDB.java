import ParalleRunner.SimpleCoordinator;
import ParalleRunner.SimpleWorker;
import util.Config;

import java.io.FileNotFoundException;

public class testParallelIMDB {

    public static void main(String []args) throws FileNotFoundException {
        Config.parse(args[0]);
        if(Config.nodeName.equalsIgnoreCase("coordinator"))
        {

            SimpleCoordinator simpleCoordinator =new SimpleCoordinator();
            simpleCoordinator.start();
            simpleCoordinator.assignJob(Config.jobs);
            simpleCoordinator.waitForResults();
        }
        else
        {
            System.out.println("Worker '"+ Config.nodeName+"' is starting...");
            SimpleWorker worker=new SimpleWorker();
            worker.start();
        }
    }
}

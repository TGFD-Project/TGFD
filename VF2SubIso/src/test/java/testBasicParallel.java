

import ParalleRunner.BasicCoordinator;
import ParalleRunner.BasicWorker;
import Util.Config;

import java.io.FileNotFoundException;

public class testBasicParallel {

    public static void main(String []args) throws FileNotFoundException {
        Config.parse(args[0]);
        if(Config.nodeName.equalsIgnoreCase("coordinator"))
        {

            BasicCoordinator simpleCoordinator =new BasicCoordinator();
            simpleCoordinator.start();
            simpleCoordinator.assignJob(Config.jobs);
            simpleCoordinator.waitForResults();
        }
        else
        {
            System.out.println("Worker '"+ Config.nodeName+"' is starting...");
            BasicWorker worker=new BasicWorker();
            worker.start();
        }
    }
}

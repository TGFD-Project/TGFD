package test.java;

import main.java.ParalleRunner.AdvancedCoordinator;
import main.java.ParalleRunner.AdvancedWorker;
import main.java.Util.Config;

import java.io.FileNotFoundException;

public class testAdvanceParallel {

    public static void main(String []args) throws FileNotFoundException {
        Config.parse(args[0]);
        if(Config.nodeName.equalsIgnoreCase("coordinator"))
        {

            AdvancedCoordinator advancedCoordinator =new AdvancedCoordinator();
            advancedCoordinator.start();
            advancedCoordinator.assignJoblets();
            advancedCoordinator.waitForResults();
        }
        else
        {
            System.out.println("Worker '"+ Config.nodeName+"' is starting...");
            AdvancedWorker worker=new AdvancedWorker();
            worker.start();
        }
    }
}

package test.java;

import main.java.ParalleRunner.MediumCoordinator;
import main.java.ParalleRunner.MediumWorker;
import main.java.Util.Config;

import java.io.FileNotFoundException;

public class testMediumParallel {

    public static void main(String []args) throws FileNotFoundException {
        Config.parse(args[0]);
        if(Config.nodeName.equalsIgnoreCase("coordinator"))
        {

            MediumCoordinator mediumCoordinator =new MediumCoordinator();
            mediumCoordinator.start();
            mediumCoordinator.assignJob(Config.jobs);
            mediumCoordinator.waitForResults();
        }
        else
        {
            System.out.println("Worker '"+ Config.nodeName+"' is starting...");
            MediumWorker worker=new MediumWorker();
            worker.start();
        }
    }
}

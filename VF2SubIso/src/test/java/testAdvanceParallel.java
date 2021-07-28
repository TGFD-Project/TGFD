import ParalleRunner.AdvancedCoordinatorNoReBalance;
import ParalleRunner.AdvancedWorkerNoRebalance;
import ParalleRunner.MediumCoordinator;
import ParalleRunner.MediumWorker;
import Util.Config;

import java.io.FileNotFoundException;

public class testAdvanceParallel {

    public static void main(String []args) throws FileNotFoundException {
        Config.parse(args[0]);
        if(Config.nodeName.equalsIgnoreCase("coordinator"))
        {

            AdvancedCoordinatorNoReBalance advancedCoordinatorNoReBalance =new AdvancedCoordinatorNoReBalance();
            advancedCoordinatorNoReBalance.start();
            advancedCoordinatorNoReBalance.assignJoblets();
            advancedCoordinatorNoReBalance.waitForResults();
        }
        else
        {
            System.out.println("Worker '"+ Config.nodeName+"' is starting...");
            AdvancedWorkerNoRebalance worker=new AdvancedWorkerNoRebalance();
            worker.start();
        }
    }
}

import Util.Config;
import Util.testRunner;

import java.io.FileNotFoundException;

public class testIncrementalRunner
{
    public static void main(String []args) throws FileNotFoundException {
        Config.parse(args[0]);
        testRunner runner = new testRunner();
        runner.load();
        //runner.testDataset();
        runner.run();
    }
}


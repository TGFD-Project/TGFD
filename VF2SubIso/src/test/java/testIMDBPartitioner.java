package test.java;

import main.java.Partitioner.IMDBPartitioner;
import main.java.Loader.IMDBLoader;
import main.java.Infra.TGFD;
import main.java.Util.Config;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class testIMDBPartitioner {

    public static void main(String []args)
    {
        try {
            if(args.length==2)
            {
                Config.parse(args[0]);
                IMDBLoader loader=new IMDBLoader(new ArrayList<TGFD>(), Config.getFirstDataFilePath());
                IMDBPartitioner partitioner=new IMDBPartitioner(loader,Integer.parseInt(args[1]));
                partitioner.partition(Config.getFirstDataFilePath().get(0),"./");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}

import Partitioner.IMDBPartitioner;
import graphLoader.IMDBLoader;
import infra.TGFD;
import util.ConfigParser;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class testIMDBPartitioner {

    public static void main(String []args)
    {
        try {
            if(args.length==2)
            {
                ConfigParser.parse(args[0]);
                IMDBLoader loader=new IMDBLoader(new ArrayList<TGFD>(),ConfigParser.getFirstDataFilePath());
                IMDBPartitioner partitioner=new IMDBPartitioner(loader,Integer.parseInt(args[1]));
                partitioner.partition(ConfigParser.getFirstDataFilePath().get(0),"./");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}

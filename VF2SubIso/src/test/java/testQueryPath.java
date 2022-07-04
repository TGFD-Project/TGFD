

import ICs.TGFD;
import QPathBasedWorkload.Query;
import Loader.TGFDGenerator;
import java.util.List;

public class testQueryPath {

    public static void main(String []args)
    {
        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator("C:\\Users\\Morteza\\Downloads\\pattern1602.txt");
        List<TGFD> allTGFDs=generator.getTGFDs();

        for (TGFD tgfd:allTGFDs) {
            Query query=new Query(tgfd);
            System.out.println("");
        }
    }

}

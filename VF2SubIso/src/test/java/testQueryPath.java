package java;

import ICs.TGFD;
import QPathBasedWorkload.Query;
import Loader.TGFDGenerator;

import java.util.List;

public class testQueryPath {

    public static void main(String []args)
    {
        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator("C:\\Users\\Morteza\\IdeaProjects\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\dbpedia\\pattern0401.txt");
        List<TGFD> allTGFDs=generator.getTGFDs();

        for (TGFD tgfd:allTGFDs) {
            Query query=new Query(tgfd);
            System.out.println("");
        }
    }

}

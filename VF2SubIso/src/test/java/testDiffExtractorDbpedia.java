

import Loader.TGFDGenerator;
import ChangeExploration.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import Loader.DBPediaLoader;
import ICs.TGFD;
import Util.Config;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class testDiffExtractorDbpedia {

    public static void main(String []args) throws FileNotFoundException {

        System.out.println("Test extract diffs over DBPedia graph");

        Config.parse(args[0]);

        System.out.println(Config.getAllDataPaths().keySet() + " *** " + Config.getAllDataPaths().values());
        System.out.println(Config.getAllTypesPaths().keySet() + " *** " + Config.getAllTypesPaths().values());

        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        List<TGFD> allTGFDs=generator.getTGFDs();

        String name="";
        for (TGFD tgfd:allTGFDs)
            name+=tgfd.getName() + "_";

        if(!name.equals(""))
            name=name.substring(0, name.length() - 1);
        else
            name="noSpecificTGFDs";

        System.out.println("Generating the diff files for the TGFD: " + name);
        Object[] ids= Config.getAllDataPaths().keySet().toArray();
        Arrays.sort(ids);
        DBPediaLoader first, second=null;
        List<Change> allChanges;
        int t1,t2=0;
        for (int i=0;i<ids.length;i+=2) {

            System.out.println("===========Snapshot (" + ids[i] + ")===========");
            long startTime = System.currentTimeMillis();

            t1=(int)ids[i];
            first = new DBPediaLoader(allTGFDs, Config.getAllTypesPaths().get((int) ids[i]),
                    Config.getAllDataPaths().get((int) ids[i]));

            printWithTime("Load graph (" + ids[i] + ")", System.currentTimeMillis() - startTime);

            //
            if(second!=null)
            {
                ChangeFinder cFinder=new ChangeFinder(second,first,allTGFDs);
                allChanges= cFinder.findAllChanged();

                analyzeChanges(allChanges,allTGFDs,second.getGraphSize(),cFinder.getNumberOfEffectiveChanges(),t2,t1,name, Config.getDiffCaps());
            }

            if(i+1>=ids.length)
                break;

            System.out.println("===========Snapshot (" + ids[i+1] + ")===========");
            startTime = System.currentTimeMillis();

            t2=(int)ids[i+1];
            second = new DBPediaLoader(allTGFDs, Config.getAllTypesPaths().get((int) ids[i+1]),
                    Config.getAllDataPaths().get((int) ids[i+1]));

            printWithTime("Load graph (" + ids[i+1] + ")", System.currentTimeMillis() - startTime);

            //
            ChangeFinder cFinder=new ChangeFinder(first,second,allTGFDs);
            allChanges= cFinder.findAllChanged();

            analyzeChanges(allChanges,allTGFDs,first.getGraphSize(),cFinder.getNumberOfEffectiveChanges(),t1,t2,name, Config.getDiffCaps());
        }
    }

    private static void analyzeChanges(List<Change> allChanges, List<TGFD> allTGFDs, int graphSize,
                                       int changeSize, int timestamp1, int timestamp2, String TGFDsName, ArrayList <Double> diffCaps)
    {
        ChangeTrimmer trimmer=new ChangeTrimmer(allChanges,allTGFDs);
        for (double i:diffCaps)
        {
            int allowedNumberOfChanges= (int) (i*graphSize);
            if (allowedNumberOfChanges<changeSize)
            {
                List<Change> trimmedChanges=trimmer.trimChanges(allowedNumberOfChanges);
                saveChanges(trimmedChanges,timestamp1,timestamp2,TGFDsName + "_" + i);
            }
            else
            {
                saveChanges(allChanges,timestamp1,timestamp2,TGFDsName + "_full");
                return;
            }
        }
    }

    private static void printWithTime(String message, long runTimeInMS)
    {
        System.out.println(message + " time: " + runTimeInMS + "(ms) ** " +
                TimeUnit.MILLISECONDS.toSeconds(runTimeInMS) + "(sec) ** " +
                TimeUnit.MILLISECONDS.toMinutes(runTimeInMS) +  "(min)");
    }

    private static void saveChanges(List<Change> allChanges, int t1, int t2, String tgfdName)
    {
        System.out.println("Printing the changes: " + t1 +" -> " + t2);
        int insertChangeEdge=0;
        int insertChangeVertex=0;
        int insertChangeAttribute=0;
        int deleteChangeEdge=0;
        int deleteChangeVertex=0;
        int deleteChangeAttribute=0;
        int changeAttributeValue=0;

        for (Change c:allChanges) {
            if(c instanceof EdgeChange)
            {
                if(c.getTypeOfChange()== ChangeType.deleteEdge)
                    deleteChangeEdge++;
                else if(c.getTypeOfChange()== ChangeType.insertEdge)
                    insertChangeEdge++;
            }
            else if(c instanceof VertexChange)
            {
                if(c.getTypeOfChange()== ChangeType.deleteVertex)
                    deleteChangeVertex++;
                else if(c.getTypeOfChange()== ChangeType.insertVertex)
                    insertChangeVertex++;
            }
            else if(c instanceof AttributeChange)
            {
                if(c.getTypeOfChange()== ChangeType.deleteAttr)
                    deleteChangeAttribute++;
                else if(c.getTypeOfChange()== ChangeType.insertAttr)
                    insertChangeAttribute++;
                else
                    changeAttributeValue++;
            }
        }

        final StringWriter sw =new StringWriter();
        final ObjectMapper mapper = new ObjectMapper();
        try
        {
            mapper.writeValue(sw, allChanges);
            FileWriter file = new FileWriter("./changes_t"+t1+"_t"+t2+"_"+tgfdName+".json");
            file.write(sw.toString());
            file.close();
            System.out.println("Successfully wrote to the file.");
            sw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Total number of changes: " + allChanges.size());
        System.out.println("Edges: +" + insertChangeEdge + " ** -" + deleteChangeEdge);
        System.out.println("Vertices: +" + insertChangeVertex + " ** -" + deleteChangeVertex);
        System.out.println("Attributes: +" + insertChangeAttribute + " ** -" + deleteChangeAttribute +" ** updates: "+ changeAttributeValue);
    }
}

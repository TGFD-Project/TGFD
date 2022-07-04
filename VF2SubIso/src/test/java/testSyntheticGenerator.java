

import SyntheticGraph.SyntheticGenerator;
import ChangeExploration.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import Loader.SyntheticLoader;
import ICs.TGFD;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class testSyntheticGenerator {

    public static void main(String []args)
    {
        if(args.length!=3)
        {
            System.out.println("""
                    Input argument is missing:
                    1. Path to the first snapshot of the synthetic graph
                    2. Number of snapshots
                    3. Name of the graph""");
            return;
        }
        SyntheticLoader syntheticLoader=new SyntheticLoader(new ArrayList <TGFD>(), Collections.singletonList(args[0]));

        SyntheticGenerator generator=new SyntheticGenerator(syntheticLoader);
        HashMap <Integer, List <Change>> changeLogs = generator.generateSnapshot(Integer.parseInt(args[1]),0.04);

        int t1=1;
        for (int t2:changeLogs.keySet()) {
            saveChanges(changeLogs.get(t2),t1,t2,args[2]);
            t1=t2;
        }
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

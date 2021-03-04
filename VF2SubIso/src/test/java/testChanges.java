import TGFDLoader.TGFDGenerator;
import changeExploration.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphLoader.DBPediaLoader;
import infra.TGFD;
import util.myConsole;
import util.properties;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class testChanges {

    public static void main(String []args) throws FileNotFoundException {

        long wallClockStart=System.currentTimeMillis();

        HashMap<Integer, ArrayList<String>> typePathsById = new HashMap<>();
        HashMap<Integer, ArrayList<String>> dataPathsById = new HashMap<>();
        String patternPath = "";
        HashMap<Integer,LocalDate> timestamps=new HashMap<>();


        System.out.println("Test changes over DBPedia graph");

        Scanner scanner = new Scanner(new File(args[0]));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String []conf=line.split(" ");
            if(conf.length!=2)
                continue;
            if (conf[0].toLowerCase().startsWith("-t"))
            {
                var snapshotId = Integer.parseInt(conf[0].substring(2));
                if (!typePathsById.containsKey(snapshotId))
                    typePathsById.put(snapshotId, new ArrayList<String>());
                typePathsById.get(snapshotId).add(conf[1]);
            }
            else if (conf[0].toLowerCase().startsWith("-d"))
            {
                var snapshotId = Integer.parseInt(conf[0].substring(2));
                if (!dataPathsById.containsKey(snapshotId))
                    dataPathsById.put(snapshotId, new ArrayList<String>());
                dataPathsById.get(snapshotId).add(conf[1]);
            }
            else if (conf[0].toLowerCase().startsWith("-p"))
            {
                patternPath = conf[1];
            }
            else if (conf[0].toLowerCase().startsWith("-s"))
            {
                var snapshotId = Integer.parseInt(conf[0].substring(2));
                timestamps.put(snapshotId, LocalDate.parse(conf[1]));
            }
            else if(conf[0].toLowerCase().startsWith("-optgraphload"))
            {
                properties.myProperties.optimizedLoadingBasedOnTGFD=Boolean.parseBoolean(conf[1]);
            }
        }

        System.out.println(dataPathsById.keySet() + " *** " + dataPathsById.values());
        System.out.println(typePathsById.keySet() + " *** " + typePathsById.values());

        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator(patternPath);
        List<TGFD> allTGFDs=generator.getTGFDs();

        for (TGFD tgfd:allTGFDs) {

            System.out.println("Generating the change files for the TGFD: " + tgfd.getName());
            Object[] ids=dataPathsById.keySet().toArray();
            Arrays.sort(ids);
            DBPediaLoader first=null, second=null;
            List<Change> allChanges;
            int t1=0,t2=0;
            for (int i=0;i<ids.length;i+=2) {

                System.out.println("===========Snapshot (" + ids[i] + ")===========");
                long startTime = System.currentTimeMillis();

                t1=(int)ids[i];
                first = new DBPediaLoader(Collections.singletonList(tgfd),typePathsById.get((int) ids[i]),
                        dataPathsById.get((int) ids[i]));

                myConsole.print("Load graph (" + ids[i] + ")", System.currentTimeMillis() - startTime);

                //
                if(second!=null)
                {
                    ChangeFinder cFinder=new ChangeFinder(second,first);
                    allChanges= cFinder.findAllChanged();
                    saveChanges(allChanges,t2,t1,tgfd.getName());
                }

                if(i+1>=ids.length)
                    break;

                System.out.println("===========Snapshot (" + ids[i+1] + ")===========");
                startTime = System.currentTimeMillis();

                t2=(int)ids[i+1];
                second = new DBPediaLoader(Collections.singletonList(tgfd),typePathsById.get((int) ids[i+1]),
                        dataPathsById.get((int) ids[i+1]));

                myConsole.print("Load graph (" + ids[i+1] + ")", System.currentTimeMillis() - startTime);

                //
                ChangeFinder cFinder=new ChangeFinder(first,second);
                allChanges= cFinder.findAllChanged();
                saveChanges(allChanges,t1,t2,tgfd.getName());

            }
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
        System.out.println("insertChangeEdge: " + insertChangeEdge);
        System.out.println("insertChangeVertex: " + insertChangeVertex);
        System.out.println("insertChangeAttribute: " + insertChangeAttribute);
        System.out.println("deleteChangeEdge: " + deleteChangeEdge);
        System.out.println("deleteChangeVertex: " + deleteChangeVertex);
        System.out.println("deleteChangeAttribute: " + deleteChangeAttribute);
        System.out.println("changeAttributeValue: " + changeAttributeValue);
    }
}

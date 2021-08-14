import TgfdDiscovery.TgfdDiscovery;
import changeExploration.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphLoader.DBPediaLoader;
import Infra.TGFD;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import util.Config;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class testDiffExtractorDbpedia {

    public static void main(String []args) throws FileNotFoundException {

        Long graphSize = args.length > 0 ? Long.valueOf(args[0]) : null;
        String graphSizeStr = graphSize == null ? "" : "-"+ graphSize;
        String str = "-t1 2015types"+graphSizeStr+".ttl\n" +
                "-d1 2015literals"+graphSizeStr+".ttl\n" +
                "-d1 2015objects"+graphSizeStr+".ttl\n" +
                "-t2 2016types"+graphSizeStr+".ttl\n" +
                "-d2 2016literals"+graphSizeStr+".ttl\n" +
                "-d2 2016objects"+graphSizeStr+".ttl\n" +
                "-t3 2017types"+graphSizeStr+".ttl\n" +
                "-d3 2017literals"+graphSizeStr+".ttl\n" +
                "-d3 2017objects"+graphSizeStr+".ttl\n" +
                "-s1 2015-10-01\n" +
                "-s2 2016-04-01\n" +
                "-s3 2016-10-01\n" +
                "-logcap 1,1,1";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("conf.txt"));
            writer.write(str);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Config.optimizedLoadingBasedOnTGFD = true;
        System.out.println("Test extract diffs over DBPedia graph");

        Config.parse("conf.txt");

        System.out.println(Config.getAllDataPaths().keySet() + " *** " + Config.getAllDataPaths().values());
        System.out.println(Config.getAllTypesPaths().keySet() + " *** " + Config.getAllTypesPaths().values());

        HashMap<Integer, ArrayList<Model>> typeModelHashMap = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            typeModelHashMap.put(i, new ArrayList<>());
            for (String pathString : Config.getAllTypesPaths().get(i)) {
                System.out.println("Reading " + pathString);
                Model model = ModelFactory.createDefaultModel();
                Path input = Paths.get(pathString);
                model.read(input.toUri().toString());
                typeModelHashMap.get(i).add(model);
            }
        }

        HashMap<Integer, ArrayList<Model>> dataModelHashMap = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            dataModelHashMap.put(i, new ArrayList<>());
            for (String pathString : Config.getAllDataPaths().get(i)) {
                System.out.println("Reading " + pathString);
                Model model = ModelFactory.createDefaultModel();
                Path input = Paths.get(pathString);
                model.read(input.toUri().toString());
                dataModelHashMap.get(i).add(model);
            }
        }

        // Create dummy TGFDs based on frequent nodes and edges from histogram
        TgfdDiscovery tgfdDiscovery = new TgfdDiscovery(Config.getTimestamps().size());
        tgfdDiscovery.graphSize = graphSize;
        tgfdDiscovery.histogram();

        ArrayList<TGFD> dummyTGFDs = tgfdDiscovery.getDummyTGFDs();
        System.out.println("Number of dummy TGFDs: " + dummyTGFDs.size());
        for (TGFD dummyTGFD : dummyTGFDs) {
            String name = dummyTGFD.getName().replace(' ', '_') + (tgfdDiscovery.graphSize == null ? "" : ("_" + tgfdDiscovery.graphSize));

            System.out.println("Generating the diff files for the TGFD: " + name);
            Object[] ids = dataModelHashMap.keySet().toArray();
            Arrays.sort(ids);
            DBPediaLoader first, second = null;
            List<Change> allChanges;
            int t1, t2 = 0;
            for (int i = 0; i < ids.length; i += 2) {

                System.out.println("===========Snapshot (" + ids[i] + ")===========");
                long startTime = System.currentTimeMillis();

                t1 = (int) ids[i];
                first = new DBPediaLoader(Collections.singletonList(dummyTGFD), typeModelHashMap.get((int) ids[i]),
                        dataModelHashMap.get((int) ids[i]));

                printWithTime("Load graph (" + ids[i] + ")", System.currentTimeMillis() - startTime);

                //
                if (second != null) {
                    ChangeFinder cFinder = new ChangeFinder(second, first, Collections.singletonList(dummyTGFD));
                    allChanges = cFinder.findAllChanged();

                    analyzeChanges(allChanges, Collections.singletonList(dummyTGFD), second.getGraphSize(), cFinder.getNumberOfEffectiveChanges(), t2, t1, name, Config.getDiffCaps());
                }

                if (i + 1 >= ids.length)
                    break;

                System.out.println("===========Snapshot (" + ids[i + 1] + ")===========");
                startTime = System.currentTimeMillis();

                t2 = (int) ids[i + 1];
                second = new DBPediaLoader(Collections.singletonList(dummyTGFD), typeModelHashMap.get((int) ids[i + 1]),
                        dataModelHashMap.get((int) ids[i + 1]));

                printWithTime("Load graph (" + ids[i + 1] + ")", System.currentTimeMillis() - startTime);

                //
                ChangeFinder cFinder = new ChangeFinder(first, second, Collections.singletonList(dummyTGFD));
                allChanges = cFinder.findAllChanged();

                analyzeChanges(allChanges, Collections.singletonList(dummyTGFD), first.getGraphSize(), cFinder.getNumberOfEffectiveChanges(), t1, t2, name, Config.getDiffCaps());

                System.gc();
            }
        }
    }

    private static void analyzeChanges(List<Change> allChanges, List<TGFD> allTGFDs, int graphSize,
                                       int changeSize, int timestamp1, int timestamp2, String TGFDsName, ArrayList <Double> diffCaps)
    {
//        ChangeTrimmer trimmer=new ChangeTrimmer(allChanges,allTGFDs);
//        for (double i:diffCaps)
//        {
//            int allowedNumberOfChanges= (int) (i*graphSize);
//            if (allowedNumberOfChanges<changeSize)
//            {
//                List<Change> trimmedChanges=trimmer.trimChanges(allowedNumberOfChanges);
//                saveChanges(trimmedChanges,timestamp1,timestamp2,TGFDsName + "_" + i);
//            }
//            else
//            {
                saveChanges(allChanges,timestamp1,timestamp2,TGFDsName);
//                return;
//            }
//        }
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
        HashMap<ChangeType, Integer> map = new HashMap<>();
        map.put(ChangeType.deleteAttr, 1);
        map.put(ChangeType.insertAttr, 2);
        map.put(ChangeType.changeAttr, 2);
        map.put(ChangeType.deleteEdge, 3);
        map.put(ChangeType.insertEdge, 4);
        map.put(ChangeType.deleteVertex, 5);
        map.put(ChangeType.insertVertex, 5);
        allChanges.sort(new Comparator<Change>() {
            @Override
            public int compare(Change o1, Change o2) {
                return map.get(o1.getTypeOfChange()).compareTo(map.get(o2.getTypeOfChange()));
            }
        });
        try {
            FileWriter file = new FileWriter("./changes_t" + t1 + "_t" + t2 + "_" + tgfdName + ".json");
            file.write("[");
            for (int index = 0; index < allChanges.size(); index++) {
                Change change = allChanges.get(index);
                final StringWriter sw =new StringWriter();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(sw, change);
                file.write(sw.toString());
                if (index < allChanges.size() - 1) {
                    file.write(",");
                }
                sw.close();
            }
            file.write("]");
            System.out.println("Successfully wrote to the file.");
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total number of changes: " + allChanges.size());
        System.out.println("Edges: +" + insertChangeEdge + " ** -" + deleteChangeEdge);
        System.out.println("Vertices: +" + insertChangeVertex + " ** -" + deleteChangeVertex);
        System.out.println("Attributes: +" + insertChangeAttribute + " ** -" + deleteChangeAttribute +" ** updates: "+ changeAttributeValue);
    }
}

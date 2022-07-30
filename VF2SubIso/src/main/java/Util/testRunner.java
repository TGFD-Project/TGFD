package Util;

import BatchViolation.NaiveBatchTED;
import ICs.TGFD;
import IncrementalRunner.IncUpdates;
import IncrementalRunner.IncrementalChange;
import Loader.*;
import VF2Runner.VF2SubgraphIsomorphism;
import Violations.Violation;
import ChangeExploration.Change;
import ChangeExploration.ChangeLoader;
import Infra.*;
import Violations.ViolationCollection;
import org.jgrapht.GraphMapping;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class testRunner {

    private GraphLoader loader=null;
    private List<TGFD> tgfds;
    private long wallClockTime=0;
    private ViolationCollection collection;
    private HashSet<String> prescriptionIDs=new HashSet<>();

    public testRunner()
    {
        System.out.println("Test Incremental algorithm for the "+ Config.datasetName +" dataset (using testRunner)");
    }

    public void load()
    {
        long startTime=System.currentTimeMillis();

        // Test whether we loaded all the files correctly
        System.out.println(Arrays.toString(Config.getFirstDataFilePath().toArray()));
        System.out.println(Config.getDiffFilesPath().keySet() + " *** " + Config.getDiffFilesPath().values());

        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        tgfds=generator.getTGFDs();
        collection = new Violations.ViolationCollection();

        //Load the first timestamp
        System.out.println("===========Snapshot 1 (" + Config.getTimestamps().get(1) + ")===========");

        if(Config.datasetName== Config.dataset.dbpedia)
        {
            loader = new DBPediaLoader(tgfds, Config.getFirstTypesFilePath(), Config.getFirstDataFilePath());
        }
        else if(Config.datasetName== Config.dataset.synthetic)
        {
            loader = new SyntheticLoader(tgfds, Config.getFirstDataFilePath());
        }
        else if(Config.datasetName== Config.dataset.pdd)
        {
            loader = new PDDLoader(tgfds, Config.getFirstDataFilePath());
        }
        else if(Config.datasetName== Config.dataset.imdb)// default is imdb
        {
            loader = new IMDBLoader(tgfds, Config.getFirstDataFilePath());
        }
        printWithTime("Load graph 1 (" + Config.getTimestamps().get(1) + ")", System.currentTimeMillis()-startTime);

        wallClockTime+=System.currentTimeMillis()-startTime;

    }

    public void testDataset()
    {
        for (Vertex v:loader.getGraph().getGraph().vertexSet()) {
            if(v.getTypes().contains("admission"))
            {
                HashSet<Vertex> neighbors=new HashSet<>();
                for (RelationshipEdge edge:loader.getGraph().getGraph().outgoingEdgesOf(v)) {
                    if(edge.getLabel().equals("diagnoses_icd9"))
                    {
                        neighbors.add(edge.getTarget());
                    }
                }
                if(neighbors.size()==1)
                {
                    System.out.println("\n------------------------");
                    System.out.println(v);
                    prescriptionIDs.add(v.getAttributeValueByName("uri"));
                    System.out.println(neighbors.stream().iterator().next());
                }
            }
        }
    }

    public String run()
    {
        if(loader==null)
        {
            System.out.println("Graph is not loaded yet");
            return null;
        }
        StringBuilder msg=new StringBuilder();

        long startTime, functionWallClockTime=System.currentTimeMillis();
        LocalDate currentSnapshotDate= Config.getTimestamps().get(1);

        //Create the match collection for all the TGFDs in the list
        HashMap <String, MatchCollection> matchCollectionHashMap=new HashMap <>();
        for (TGFD tgfd:tgfds) {
            matchCollectionHashMap.put(tgfd.getName(),new MatchCollection(tgfd.getPattern(),tgfd.getDependency(),tgfd.getDelta().getGranularity()));
        }


        // Now, we need to find the matches for the first snapshot.
        for (TGFD tgfd:tgfds) {
            VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
            System.out.println("\n###########"+tgfd.getName()+"###########");
            Iterator <GraphMapping <Vertex, RelationshipEdge>> results= VF2.execute(loader.getGraph(), tgfd.getPattern(),false);

            //Retrieving and storing the matches of each timestamp.
            System.out.println("Retrieving the matches");
            startTime=System.currentTimeMillis();
            matchCollectionHashMap.get(tgfd.getName()).addMatches(currentSnapshotDate,results);
            printWithTime("Match retrieval", System.currentTimeMillis()-startTime);
        }

        //Load the change files
        Object[] ids= Config.getDiffFilesPath().keySet().toArray();
        Arrays.sort(ids);
        for (int i=0;i<ids.length;i++)
        {
            System.out.println("===========Snapshot "+ids[i]+" (" + Config.getTimestamps().get(ids[i]) + ")===========");

            startTime=System.currentTimeMillis();
            currentSnapshotDate= Config.getTimestamps().get((int)ids[i]);
            LocalDate prevTimeStamp=Config.getTimestamps().get(((int)ids[i])-1);
            ChangeLoader changeLoader=new ChangeLoader(Config.getDiffFilesPath().get(ids[i]));
            List<Change> changes=changeLoader.getAllChanges();

            printWithTime("Load changes "+ids[i]+" (" + Config.getTimestamps().get(ids[i]) + ")", System.currentTimeMillis()-startTime);
            System.out.println("Total number of changes: " + changes.size());

            // Now, we need to find the matches for each snapshot.
            // Finding the matches...

            startTime=System.currentTimeMillis();
            System.out.println("Updating the graph");
            IncUpdates incUpdatesOnGraph=new IncUpdates(loader.getGraph(),tgfds);
            incUpdatesOnGraph.AddNewVertices(changes);

            HashMap<String, ArrayList <String>> newMatchesSignaturesByTGFD=new HashMap <>();
            HashMap<String,ArrayList<String>> removedMatchesSignaturesByTGFD=new HashMap <>();
            HashMap<String,TGFD> tgfdsByName=new HashMap <>();
            for (TGFD tgfd:tgfds) {
                newMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
                removedMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
                tgfdsByName.put(tgfd.getName(),tgfd);
            }
            for (Change change:changes) {

                //System.out.print("\n" + change.getId() + " --> ");
                HashMap<String, IncrementalChange> incrementalChangeHashMap=incUpdatesOnGraph.updateGraph(change,tgfdsByName);
                if(incrementalChangeHashMap==null)
                    continue;
                for (String tgfdName:incrementalChangeHashMap.keySet()) {
                    newMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getNewMatches().keySet());
                    removedMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getRemovedMatchesSignatures());
                    matchCollectionHashMap.get(tgfdName).addMatches(currentSnapshotDate,incrementalChangeHashMap.get(tgfdName).getNewMatches());
                }
            }
            for (TGFD tgfd:tgfds) {
                matchCollectionHashMap.get(tgfd.getName()).addTimestamp(currentSnapshotDate, prevTimeStamp,
                        newMatchesSignaturesByTGFD.get(tgfd.getName()),removedMatchesSignaturesByTGFD.get(tgfd.getName()));
                System.out.println("New matches ("+tgfd.getName()+"): " +
                        newMatchesSignaturesByTGFD.get(tgfd.getName()).size() + " ** " + removedMatchesSignaturesByTGFD.get(tgfd.getName()).size());
            }
            printWithTime("Update and retrieve matches ", System.currentTimeMillis()-startTime);
            //myConsole.print("#new matches: " + newMatchesSignatures.size()  + " - #removed matches: " + removedMatchesSignatures.size());
        }

        for (TGFD tgfd:tgfds) {

            System.out.println("==========="+tgfd.getName()+"===========");

//            System.out.println("Running the optimized TED");
//
//            startTime=System.currentTimeMillis();
//            OptBatchTED optimize=new OptBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
//            Set<Violation> allViolationsOptBatchTED=optimize.findViolations();
//            System.out.println("Number of violations (Optimized method): " + allViolationsOptBatchTED.size());
//            printWithTime("Optimized Batch TED", System.currentTimeMillis()-startTime);
//            msg.append(getViolationsMessage(allViolationsOptBatchTED,tgfd));


            // Now, we need to find all the violations
            //First, we run the Naive Batch TED
            System.out.println("Running the naive TED");

            startTime=System.currentTimeMillis();
            NaiveBatchTED naive=new NaiveBatchTED(matchCollectionHashMap.get(tgfd.getName()),tgfd);
            Set<Violation> allViolationsOptBatchTED=naive.findViolations();
            System.out.println("Number of violations (Naive method): " + allViolationsOptBatchTED.size());
            collection.addViolations(tgfd, allViolationsOptBatchTED); // Add violation into violation collection !!!!!!!!!!!!
            printWithTime("Naive Batch TED", System.currentTimeMillis()-startTime);
            if(Config.saveViolations)
                saveViolations("./naive",allViolationsOptBatchTED,tgfd,collection,prescriptionIDs);
            msg.append(getViolationsMessage(allViolationsOptBatchTED,tgfd));

        }
        wallClockTime+=System.currentTimeMillis()-functionWallClockTime;
        printWithTime("Total wall clock time: ", wallClockTime);
        return msg.toString();
    }

    public GraphLoader getLoader() {
        return loader;
    }

    private String getViolationsMessage(Set<Violation> violations, TGFD tgfd)
    {
        StringBuilder msg=new StringBuilder();
        msg.append("***************TGFD***************\n");
        msg.append(tgfd.toString());
        msg.append("\n===============Violations===============\n");
        for (Violation vio:violations) {
            msg.append(vio.toString() +
                    "\n---------------------------------------------------\n");
        }
        return msg.toString();
    }

    private void printWithTime(String message, long runTimeInMS)
    {
        System.out.println(message + " time: " + runTimeInMS + "(ms) ** " +
                TimeUnit.MILLISECONDS.toSeconds(runTimeInMS) + "(sec) ** " +
                TimeUnit.MILLISECONDS.toMinutes(runTimeInMS) +  "(min)");
    }

    private static void saveViolations(String path, Set<Violation> violations, TGFD tgfd, ViolationCollection collection,HashSet<String> prescriptionIDs)
    {
        try {
            HashMap<String, Integer> drugCount=new HashMap<>();
            HashMap<String,HashMap<String, Integer>> drugViolatingDosage = new HashMap<>();
            HashMap<String, ArrayList<String>> drugDisease= new HashMap<>();
            HashMap<String, Integer> patients = new HashMap<>();
            FileWriter file = new FileWriter(path +"_" + tgfd.getName() + ".txt");
            file.write("***************TGFD***************\n");
            file.write(tgfd.toString());
            file.write("\n===============Violations===============\n");
            int withinOneTimestamp=0;
            int i =1;
            for (Violation vio:violations) {
                if(Config.timestampsReverseMap.get(vio.getInterval().getStart()) == Config.timestampsReverseMap.get(vio.getInterval().getEnd()))
                    withinOneTimestamp++;
                file.write(i+".");
                file.write(vio.toString() +
                        "\nPatters1: " + vio.getMatch1().getSignatureFromPattern() +
                        "\nPatters2: " + vio.getMatch2().getSignatureFromPattern() +
                        "\n---------------------------------------------------\n");

                //extraAnalysis(drugCount,drugViolatingDosage,drugDisease,patients,vio);

                i++;
//                String admissionID=vio.getMatch1().getSignatureFromPattern(vio.getInterval().getStart()).split(",")[0];
//                if(prescriptionIDs.contains(admissionID))
//                {
//                    file.write("Found it!" + admissionID);
//                }
//                admissionID=vio.getMatch2().getSignatureFromPattern(vio.getInterval().getEnd()).split(",")[0];
//                if(prescriptionIDs.contains(admissionID))
//                {
//                    file.write("Found it!" + admissionID);
//                }
            }
            System.out.println("Number within timestamp: " + withinOneTimestamp + " out of " + violations.size());
            printExtraAnalysis(drugCount,drugViolatingDosage,drugDisease,patients);
//            file.write("\n===============Sorted Error Matches (Frequency of Occurrence)===============\n");
//            /*Problems for multiple TGFDs*/
//            ArrayList<Match> sort_list = collection.sortMatchList();
//            for(Match match:sort_list){
//                file.write(match.getSignatureX()+
//                        "\n---------------------------------------------------\n");
//            }


            file.close();
            System.out.println("Successfully wrote to the file: " + path);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void extraAnalysis(HashMap<String, Integer> drugCount,
                                      HashMap<String,HashMap<String, Integer>> drugViolatingDosage,
                                      HashMap<String, ArrayList<String>> drugDisease,
                                      HashMap<String, Integer> patients,
                                      Violation vio)
    {
//            Patters1: 172484,29.9,m,adult,pres415063,200mg,http://bio2rdf.org/drugbank:db00996,v12.04,
//            Patters2: 172484,29.9,m,adult,pres415063,200mg,http://bio2rdf.org/drugbank:db00996,v12.04,
        try
        {
            String [] temp = vio.getMatch1().getSignatureFromPattern().split(",");
            if(!patients.containsKey(temp[0]))
                patients.put(temp[0],0);
            patients.put(temp[0],patients.get(temp[0])+2);
            String drugName= temp[6];
            String diseaseName=temp[7];
            if(!drugDisease.containsKey(drugName))
                drugDisease.put(drugName,new ArrayList<>());
            drugDisease.get(drugName).add(diseaseName);

            if(!drugCount.containsKey(drugName))
                drugCount.put(drugName,0);
            drugCount.put(drugName,drugCount.get(drugName)+1);

            if(!drugViolatingDosage.containsKey(drugName))
                drugViolatingDosage.put(drugName,new HashMap<>());
            if(!drugViolatingDosage.get(drugName).containsKey(vio.getY1()))
                drugViolatingDosage.get(drugName).put(vio.getY1(),0);
            if(!drugViolatingDosage.get(drugName).containsKey(vio.getY2()))
                drugViolatingDosage.get(drugName).put(vio.getY2(),0);
            drugViolatingDosage.get(drugName).put(vio.getY1(),drugViolatingDosage.get(drugName).get(vio.getY1())+1);
            drugViolatingDosage.get(drugName).put(vio.getY2(),drugViolatingDosage.get(drugName).get(vio.getY2())+1);
        }
        catch (Exception e)
        {
            //System.out.println(vio.getMatch1().getSignatureFromPattern(vio.getInterval().getStart()));
        }

    }

    private static void printExtraAnalysis(HashMap<String, Integer> drugCount,
                                           HashMap<String,HashMap<String, Integer>> drugViolatingDosage,
                                           HashMap<String, ArrayList<String>> drugDisease,
                                           HashMap<String, Integer> patients)
    {
        HashSet<String> visited = new HashSet<>();
        boolean found = false;
        while (true)
        {
            int max = Integer.MIN_VALUE;
            String val = "";
            for (String drugName:drugCount.keySet()) {
                if(!visited.contains(drugName) && drugCount.get(drugName)>max)
                {
                    val=drugName;
                    found = true;
                    max = drugCount.get(drugName);
                }
            }
            if(!found)
                break;
            found = false;
            visited.add(val);
            System.out.println("Drug: " + val + " -- " + max);
        }

        for (String patient:patients.keySet()) {
            System.out.println(patient + "  :  " + patients.get(patient));
        }
    }

}



import ErrorInjection.ErrorGenerator;
import ICs.TGFD;
import IncrementalRunner.IncUpdates;
import IncrementalRunner.IncrementalChange;
import Loader.TGFDGenerator;
import VF2Runner.VF2SubgraphIsomorphism;
import Violations.Violation;
import ChangeExploration.Change;
import ChangeExploration.ChangeLoader;
import Loader.GraphLoader;
import Loader.IMDBLoader;
import Infra.*;
import org.jgrapht.GraphMapping;
import Util.Config;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class testErrorInjectionIMDB {

    public static void main(String []args) throws FileNotFoundException {

        long wallClockStart=System.currentTimeMillis();

        Config.parse(args[0]);

        System.out.println("Test Error Injection over IMDB");

        System.out.println(Arrays.toString(Config.getFirstDataFilePath().toArray()));
        System.out.println(Config.getDiffFilesPath().keySet() + " *** " + Config.getDiffFilesPath().values());

        //Load the TGFDs.
        TGFDGenerator generator = new TGFDGenerator(Config.patternPath);
        List <TGFD> allTGFDs=generator.getTGFDs();

        //Create the match collection for all the TGFDs in the list
        HashMap <String, MatchCollection> matchCollectionHashMap=new HashMap <>();
        for (TGFD tgfd:allTGFDs) {
            matchCollectionHashMap.put(tgfd.getName(),new MatchCollection(tgfd.getPattern(),tgfd.getDependency(),tgfd.getDelta().getGranularity()));
        }

        //Load the first timestamp
        System.out.println("-----------Snapshot (1)-----------");
        long startTime=System.currentTimeMillis();
        LocalDate currentSnapshotDate= Config.getTimestamps().get(1);
        GraphLoader imdb = new IMDBLoader(allTGFDs, Config.getFirstDataFilePath());

        printWithTime("Load graph (1)", System.currentTimeMillis()-startTime);

        // Now, we need to find the matches for each snapshot.
        // Finding the matches...

        for (TGFD tgfd:allTGFDs) {
            VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
            System.out.println("\n###########"+tgfd.getName()+"###########");
            Iterator <GraphMapping <Vertex, RelationshipEdge>> results= VF2.execute(imdb.getGraph(), tgfd.getPattern(),false);

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
            System.out.println("-----------Snapshot (" + ids[i] + ")-----------");

            startTime=System.currentTimeMillis();
            currentSnapshotDate= Config.getTimestamps().get((int)ids[i]);
            ChangeLoader changeLoader=new ChangeLoader(Config.getDiffFilesPath().get(ids[i]));
            List<Change> changes=changeLoader.getAllChanges();

            printWithTime("Load changes ("+ids[i] + ")", System.currentTimeMillis()-startTime);
            System.out.println("Total number of changes: " + changes.size());

            // Now, we need to find the matches for each snapshot.
            // Finding the matches...

            startTime=System.currentTimeMillis();
            System.out.println("Updating the imdb graph");
            IncUpdates incUpdatesOnDBpedia=new IncUpdates(imdb.getGraph(),allTGFDs);
            incUpdatesOnDBpedia.AddNewVertices(changes);

            HashMap<String, ArrayList <String>> newMatchesSignaturesByTGFD=new HashMap <>();
            HashMap<String,ArrayList<String>> removedMatchesSignaturesByTGFD=new HashMap <>();
            HashMap<String,TGFD> tgfdsByName=new HashMap <>();
            for (TGFD tgfd:allTGFDs) {
                newMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
                removedMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList <>());
                tgfdsByName.put(tgfd.getName(),tgfd);
            }
            for (Change change:changes) {

                //System.out.print("\n" + change.getId() + " --> ");
                HashMap<String, IncrementalChange> incrementalChangeHashMap=incUpdatesOnDBpedia.updateGraph(change,tgfdsByName);
                if(incrementalChangeHashMap==null)
                    continue;
                for (String tgfdName:incrementalChangeHashMap.keySet()) {
                    newMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getNewMatches().keySet());
                    removedMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getRemovedMatchesSignatures());
                    matchCollectionHashMap.get(tgfdName).addMatches(currentSnapshotDate,incrementalChangeHashMap.get(tgfdName).getNewMatches());
                }
            }
            for (TGFD tgfd:allTGFDs) {
                matchCollectionHashMap.get(tgfd.getName()).addTimestamp(currentSnapshotDate,
                        newMatchesSignaturesByTGFD.get(tgfd.getName()),removedMatchesSignaturesByTGFD.get(tgfd.getName()));
            }
            printWithTime("Update and retrieve matches ", System.currentTimeMillis()-startTime);
            //myConsole.print("#new matches: " + newMatchesSignatures.size()  + " - #removed matches: " + removedMatchesSignatures.size());
        }

        for (TGFD tgfd:allTGFDs) {
            System.out.println("==========="+tgfd.getName()+"===========");

            for (double i=0.01;i<0.1;i+=0.02)
            {
                System.out.println("Injecting errors and evaluating the results with "+i + "% error rate");
                ErrorGenerator errorGenerator=new ErrorGenerator(matchCollectionHashMap.get(tgfd.getName()),tgfd);
                errorGenerator.evaluate(i);

            }
        }
        printWithTime("Total wall clock time: ", System.currentTimeMillis()-wallClockStart);
    }

    private static void printWithTime(String message, long runTimeInMS)
    {
        System.out.println(message + " time: " + runTimeInMS + "(ms) ** " +
                TimeUnit.MILLISECONDS.toSeconds(runTimeInMS) + "(sec) ** " +
                TimeUnit.MILLISECONDS.toMinutes(runTimeInMS) +  "(min)");
    }

    private static void saveViolations(String path, Set<Violation> violations, TGFD tgfd)
    {
        try {
            FileWriter file = new FileWriter(path +"_" + tgfd.getName() + ".txt");
            file.write("***************TGFD***************\n");
            file.write(tgfd.toString());
            file.write("\n===============Violations===============\n");
            for (Violation vio:violations) {
                file.write(vio.toString() +
                        "\n---------------------------------------------------\n");
            }
            file.close();
            System.out.println("Successfully wrote to the file: " + path);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

}

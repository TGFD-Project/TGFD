import VF2Runner.VF2SubgraphIsomorphism;
import graphLoader.dbPediaLoader;
import infra.VF2PatternGraph;
import patternLoader.patternGenerator;

import java.util.ArrayList;

public class testDBPediaLoader {


    public static void main(String []args) {
        //Expected arguments:
        // arges[0]: Type file,             sample ->  "F:\\MorteZa\\Datasets\\Statistical\\2016\\types.ttl"
        // arges[1]: Object mapping file,   sample ->  "F:\\MorteZa\\Datasets\\Statistical\\2016\\mappingbased_objects_en.ttl"
        // arges[2]: Literal mapping file,  sample ->  "F:\\MorteZa\\Datasets\\Statistical\\2016\\literals.ttl"
        // arges[3]: Graph pattern file,    sample ->  "D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\pattern1.txt"


        ArrayList<String> typesPath=new ArrayList<>();
        ArrayList<String> dataPath=new ArrayList<>();
        String patternPath="";


        System.out.println("Test DBPedia subgraph isomorphism");


        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-t")) {
                typesPath.add(args[++i]);
            } else if (args[i].equals("-d")) {
                dataPath.add(args[++i]);
            } else if (args[i].equals("-p")) {
                patternPath = args[++i];
            }
        }

        dbPediaLoader dbpedia = new dbPediaLoader(typesPath,dataPath);

        patternGenerator generator = new patternGenerator(patternPath);

        VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
        for (VF2PatternGraph pattern : generator.getPattern()) {
            System.out.println("\n########## Graph pattern ##########");
            System.out.println(pattern.toString());
            VF2.execute(dbpedia.getGraph(), pattern,false);
        }

    }

}

import VF2Runner.VF2SubgraphIsomorphism;
import graphLoader.dbPediaLoader;
import infra.VF2PatternGraph;
import patternLoader.PatternGenerator;

import java.util.ArrayList;
import java.util.HashMap;

public class TestDBPedia
{
    // TODO: input a JSON file for arguments [2021-02-14]
    // {
    //   "patternFile": "D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\pattern1.txt",
    //   "snapshots: [
    //     {
    //       "date": "2017",
    //       "literalFile": "2017-literals-0.txt",
    //       "objectFiles": [
    //         "2017-mappings-0.ttl",
    //         "2017-mappings-1.ttl"
    //       ],
    //       typefiles: [
    //         "2017-types-0.ttl",
    //         "2017-types-1.ttl"
    //       ],
    //     }
    //   ]
    // }
    /**
     * Arguments: -p <patternFile> [-t<snapshotId> <typeFile>] [-d<snapshotId> <dataFile>]
     *
     * Example:
     *   TestDBPedia \
     *     -p "D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\pattern1.txt" \
     *     -t1 "F:\\MorteZa\\Datasets\\Statistical\\2016\\types.ttl" \
     *     -t1 "F:\\MorteZa\\Datasets\\Statistical\\2016\\types2.ttl" \
     *     -d1 "F:\\MorteZa\\Datasets\\Statistical\\2016\\mappingbased_objects_en.ttl" \
     *     -d1 "F:\\MorteZa\\Datasets\\Statistical\\2016\\mappingbased_objects_en2.ttl" \
     *     -t2 "F:\\MorteZa\\Datasets\\Statistical\\2017\\types.ttl" \
     *     -t2 "F:\\MorteZa\\Datasets\\Statistical\\2017\\types2.ttl" \
     *     -d2 "F:\\MorteZa\\Datasets\\Statistical\\2017\\mappingbased_objects_en.ttl" \
     *     -d2 "F:\\MorteZa\\Datasets\\Statistical\\2017\\mappingbased_objects_en2.ttl"
     */
    public static void main(String []args)
    {
        //Expected arguments:
        // arges[0]: Type file,             sample ->  "F:\\MorteZa\\Datasets\\Statistical\\2016\\types.ttl"
        // arges[1]: Object mapping file,   sample ->  "F:\\MorteZa\\Datasets\\Statistical\\2016\\mappingbased_objects_en.ttl"
        // arges[2]: Literal mapping file,  sample ->  "F:\\MorteZa\\Datasets\\Statistical\\2016\\literals.ttl"
        // arges[3]: Graph pattern file,    sample ->  "D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\pattern1.txt"

        // TODO: input literals [2021-02-14]
        HashMap<Integer, ArrayList<String>> typePathsById = new HashMap<>();
        HashMap<Integer, ArrayList<String>> dataPathsById = new HashMap<>();
        String patternPath = "";

        System.out.println("Test DBPedia subgraph isomorphism");

        for (int i = 0; i < args.length; i++)
        {
            if (args[i].startsWith("-t"))
            {
                var snapshotId = Integer.parseInt(args[i].substring(2));
                if (!typePathsById.containsKey(snapshotId))
                    typePathsById.put(snapshotId, new ArrayList<String>());
                typePathsById.get(snapshotId).add(args[++i]);
            }
            else if (args[i].equals("-d"))
            {
                var snapshotId = Integer.parseInt(args[i].substring(2));
                if (!dataPathsById.containsKey(snapshotId))
                    dataPathsById.put(snapshotId, new ArrayList<String>());
                dataPathsById.get(snapshotId).add(args[++i]);
            }
            else if (args[i].equals("-p"))
            {
                patternPath = args[++i];
            }
        }

        // TODO: check that typesPaths.keySet == dataPaths.keySet [2021-02-14]
        for (var snapshotId : dataPathsById.keySet())
        {
            dbPediaLoader dbpedia = new dbPediaLoader(
                typePathsById.get(snapshotId),
                dataPathsById.get(snapshotId));

            PatternGenerator generator = new PatternGenerator(patternPath);

            VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();

            for (VF2PatternGraph pattern : generator.getPattern())
            {
                System.out.println("\n########## Graph pattern ##########");
                System.out.println(pattern.toString());
                VF2.execute(dbpedia.getGraph(), pattern,false);
            }
        }
    }
}
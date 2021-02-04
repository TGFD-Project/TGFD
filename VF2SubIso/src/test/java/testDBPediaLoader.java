import graphLoader.dbPediaLoader;

public class testDBPediaLoader {


    public static void main(String []args) {

//        VF2SubgraphIsomorphism VF2=new VF2SubgraphIsomorphism();
//        VF2.executeAndPrintResults(generateDataGraph(),generatePatternGraph());

        dbPediaLoader dbpedia=new dbPediaLoader("F:\\MorteZa\\Datasets\\Statistical\\2016\\types.ttl",
                "F:\\MorteZa\\Datasets\\Statistical\\2016\\mappingbased_objects_en.ttl");

    }

}

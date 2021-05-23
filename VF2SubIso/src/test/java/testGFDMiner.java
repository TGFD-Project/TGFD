import graphLoader.DBPediaLoader;
import graphLoader.IMDBLoader;
import infra.*;
import org.apache.jena.vocabulary.DB;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class testGFDMiner {

    public static void main(String []args)
    {
        DBPediaLoader dbpedia2015 = new DBPediaLoader(new ArrayList<>(), new ArrayList<>(Collections.singletonList("2015types.ttl")), new ArrayList<>(Arrays.asList("2015literals.ttl", "2015objects.ttl")));
        DBPediaLoader dbpedia2016 = new DBPediaLoader(new ArrayList<>(), new ArrayList<>(Collections.singletonList("2016types.ttl")), new ArrayList<>(Arrays.asList("2016literals.ttl", "2016objects.ttl")));
        DBPediaLoader dbpedia2017 = new DBPediaLoader(new ArrayList<>(), new ArrayList<>(Collections.singletonList("2017types.ttl")), new ArrayList<>(Arrays.asList("2017literals.ttl", "2017objects.ttl")));
        ArrayList<DBPediaLoader> loaders = new ArrayList<>(Arrays.asList(dbpedia2015,dbpedia2016,dbpedia2017));

        int year = 2015;
        for (DBPediaLoader loader : loaders) {
            StringBuilder sb = new StringBuilder();
            for (Vertex v : loader.getGraph().getGraph().vertexSet()) {
                DataVertex data_v = (DataVertex) v;
                String v_type = data_v.getTypes().stream().findFirst().orElse("");
                sb.append("L").append("\t").append(data_v.getVertexURI()).append("\t").append(v_type).append("\n");
                for (Attribute attr : data_v.getAllAttributesList()) {
                    if (!attr.getAttrName().equals("uri"))
                        sb.append("A").append("\t").append(data_v.getVertexURI()).append("\t").append(attr.getAttrName()).append("\t").append(attr.getAttrValue()).append("\n");
                }
                for (RelationshipEdge edge : loader.getGraph().getGraph().outgoingEdgesOf(v)) {
                    DataVertex out_v = (DataVertex) edge.getTarget();
                    sb.append("E").append("\t").append(data_v.getVertexURI()).append("\t").append(out_v.getVertexURI()).append("\t").append(edge.getLabel()).append("\n");
                }
            }

            try {
                String fileName = year+"-dbpedia.tsv";
                FileWriter file = new FileWriter(fileName);
                file.write(sb.toString());
                file.close();
                System.out.println("Successfully wrote to the "+fileName+".");
                BufferedReader csvReader = new BufferedReader(new FileReader(fileName));
                String row;
                while ((row = csvReader.readLine()) != null) {
                    if (!row.contains("\t")){
                        System.out.println(row);
                    }
                }
                csvReader.close();
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
            year++;
        }
    }

}

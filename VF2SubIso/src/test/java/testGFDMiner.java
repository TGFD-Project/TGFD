package java;

import Loader.DBPediaLoader;
import Infra.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class testGFDMiner {

    public static void main(String []args)
    {
        ArrayList <String> paths=new ArrayList <>();
        paths.add(args[0]);
        DBPediaLoader imdb = new DBPediaLoader(null,new ArrayList <String>(),paths);

        StringBuilder sb=new StringBuilder();
        for (Vertex v:imdb.getGraph().getGraph().vertexSet()) {
            DataVertex data_v=(DataVertex) v;
            String v_type=data_v.getTypes().stream().findFirst().orElse("");
            sb.append("L").append("\t").append(data_v.getVertexURI()).append("\t").append(v_type).append("\n");
            for (Attribute attr:data_v.getAllAttributesList()) {
                if(!attr.getAttrName().equals("uri"))
                    sb.append("A").append("\t").append(data_v.getVertexURI()).append("\t").append(attr.getAttrName()).append("\t").append(attr.getAttrValue()).append("\n");
            }
            for (RelationshipEdge edge:imdb.getGraph().getGraph().outgoingEdgesOf(v)) {
                DataVertex out_v=(DataVertex) edge.getTarget();
                sb.append("E").append("\t").append(data_v.getVertexURI()).append("\t").append(out_v.getVertexURI()).append("\t").append(edge.getLabel()).append("\n");
            }
        }

        try {
            FileWriter file = new FileWriter(args[1]);
            file.write(sb.toString());
            file.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

}

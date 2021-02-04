package graphLoader;

import infra.VF2DataGraph;
import infra.dataVertex;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import java.nio.file.Path;
import java.nio.file.Paths;

public class dbPediaLoader {


    private VF2DataGraph graph;

    public dbPediaLoader(String nodeTypeFilePath, String nodeDataPath)
    {
        graph=new VF2DataGraph();
    }

    private void loadNodeMap(String nodeTypesPath) {

        if (nodeTypesPath == null || nodeTypesPath.length() == 0) {
            System.out.println("No Input Node Types File Path!");
            return;
        }
        System.out.println("Start Loading DBPedia Node Map...");

        Model model = ModelFactory.createDefaultModel();
        System.out.println("Loading Node Types...");

        Path input= Paths.get(nodeTypesPath);
        model.read(input.toUri().toString());

        StmtIterator typeTriples = model.listStatements();

        while (typeTriples.hasNext()) {
            Statement stmt = typeTriples.nextStatement();

            String subject = stmt.getSubject().getURI();
            if (subject.length() > 28) {
                subject = subject.substring(28).toLowerCase();
            }
            String object = stmt.getObject().asResource().getLocalName().toLowerCase();

            int nodeId = subject.hashCode();
            dataVertex v= (dataVertex) graph.getNode(nodeId);

            if (v==null) {
                v=new dataVertex(object,subject);
                graph.addVertex(v);
            }
            else {
                v.addTypes(object);
            }

        }
        System.out.println("Done Loading DBPedia Node Map!!!");
        System.out.println("DBPedia NodesMap Size: " + graph.getSize());
    }


}

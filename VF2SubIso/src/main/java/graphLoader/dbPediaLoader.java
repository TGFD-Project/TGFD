package graphLoader;

import infra.VF2DataGraph;
import infra.attribute;
import infra.dataVertex;
import infra.relationshipEdge;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.rdf.model.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class dbPediaLoader {


    private VF2DataGraph graph;

    public dbPediaLoader(String nodeTypeFilePath, String nodeDataPath)
    {
        graph=new VF2DataGraph();

        loadNodeMap(nodeTypeFilePath);

        loadDataGraph(nodeDataPath);
    }

    public VF2DataGraph getGraph() {
        return graph;
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

    private void loadDataGraph(String dataGraphFilePath) {

        if (dataGraphFilePath == null || dataGraphFilePath.length() == 0) {
            System.out.println("No Input Graph Data File Path!");
            return;
        }
        Model model = ModelFactory.createDefaultModel();
        System.out.println("Loading DBPedia Graph...");
        //model.read(dataGraphFilePath);
        Path input= Paths.get(dataGraphFilePath);
        model.read(input.toUri().toString());

        StmtIterator dataTriples = model.listStatements();

        while (dataTriples.hasNext()) {

            Statement stmt = dataTriples.nextStatement();
            String subjectString = stmt.getSubject().getURI();
            if (subjectString.length() > 28) {
                subjectString = subjectString.substring(28);
            }

            String predicate = stmt.getPredicate().getLocalName().toLowerCase();
            RDFNode object = stmt.getObject();
            String objectString;

            try {
                if (object.isLiteral()) {
                    objectString = object.asLiteral().getString().toLowerCase();
                } else {
                    objectString = object.asResource().getLocalName().toLowerCase();
                }
            } catch (DatatypeFormatException e) {
                System.out.println("Invalid DataType Skipped!");
                e.printStackTrace();
                continue;
            }

            int subjectNodeId = subjectString.toLowerCase().hashCode();
            int objectNodeId = objectString.hashCode();

            dataVertex subjVertex= (dataVertex) graph.getNode(subjectNodeId);

            if (subjVertex==null) {
                System.out.println("Node not found: " + subjectString);
                continue;
            }


            if (!object.isLiteral()) {
                dataVertex objVertex= (dataVertex) graph.getNode(objectNodeId);
                if(objVertex==null)
                {
                    System.out.println("Node not found: " + subjectString);
                    continue;
                }
                else if (subjectNodeId == objectNodeId) {
                    System.out.println("Loop found: " + subjectString + " -> " + objectString);
                    continue;
                }
                graph.addEdge(subjVertex, objVertex, new relationshipEdge(predicate));
            }
            else
            {
                subjVertex.addAttribute(new attribute(predicate,objectString));
            }
        }
        System.out.println("Number of Nodes: " + graph.getGraph().vertexSet().size());
        System.out.println("Number of Edges: " + graph.getGraph().edgeSet().size());
        System.out.println("Done Loading DBPedia Graph!!!");
    }
}

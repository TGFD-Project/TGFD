package graphLoader;

import infra.Literal;
import infra.*;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.rdf.model.*;
import util.properties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class dbPediaLoader {


    private VF2DataGraph graph;

    private Set<String> validTypes=new HashSet<>();

    public dbPediaLoader(ArrayList<String> typesPath, ArrayList<String> dataPath, TGFD tgfd)
    {
        graph=new VF2DataGraph();
        extractValidTypesFromTGFD(tgfd);

        System.out.println("Type files: " + typesPath);
        for (String typePath:typesPath) {
            loadNodeMap(typePath);
        }

        System.out.println("Data files: " + dataPath);
        for (String dataP:dataPath) {
            loadDataGraph(dataP);
        }
    }

    public VF2DataGraph getGraph() {
        return graph;
    }

    private void extractValidTypesFromTGFD(TGFD tgfd)
    {
        for (Literal x:tgfd.getDependency().getX()) {
            if(x instanceof ConstantLiteral)
                validTypes.add(((ConstantLiteral) x).getVertexType());
            else if(x instanceof VariableLiteral)
            {
                validTypes.add(((VariableLiteral) x).getVertexType_1());
                validTypes.add(((VariableLiteral) x).getVertexType_2());
            }

        }
        for (Literal x:tgfd.getDependency().getY()) {
            if(x instanceof ConstantLiteral)
                validTypes.add(((ConstantLiteral) x).getVertexType());
            else if(x instanceof VariableLiteral)
            {
                validTypes.add(((VariableLiteral) x).getVertexType_1());
                validTypes.add(((VariableLiteral) x).getVertexType_2());
            }

        }
        for (Vertex v:tgfd.getPattern().getGraph().vertexSet()) {
            if(v instanceof PatternVertex)
                validTypes.addAll(v.getTypes());
        }
    }

    private void loadNodeMap(String nodeTypesPath) {

        if (nodeTypesPath == null || nodeTypesPath.length() == 0) {
            System.out.println("No Input Node Types File Path!");
            return;
        }
        try
        {
            Model model = ModelFactory.createDefaultModel();
            System.out.println("Loading Node Types: " + nodeTypesPath);

            Path input= Paths.get(nodeTypesPath);
            model.read(input.toUri().toString());

            StmtIterator typeTriples = model.listStatements();

            while (typeTriples.hasNext()) {
                Statement stmt = typeTriples.nextStatement();

                String nodeURI = stmt.getSubject().getURI().toLowerCase();
                if (nodeURI.length() > 28) {
                    nodeURI = nodeURI.substring(28);
                }
                String nodeType = stmt.getObject().asResource().getLocalName().toLowerCase();

                // ignore the node if the type is not in the validTypes and
                // optimizedLoadingBasedOnTGFD is true
                if(properties.dbpediaProperties.optimizedLoadingBasedOnTGFD && !validTypes.contains(nodeType))
                    continue;
                //int nodeId = subject.hashCode();
                DataVertex v= (DataVertex) graph.getNode(nodeURI);

                if (v==null) {
                    v=new DataVertex(nodeURI,nodeType);
                    graph.addVertex(v);
                }
                else {
                    v.addTypes(nodeType);
                }
            }
            System.out.println("Done. DBPedia types Size: " + graph.getSize());
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void loadDataGraph(String dataGraphFilePath) {

        if (dataGraphFilePath == null || dataGraphFilePath.length() == 0) {
            System.out.println("No Input Graph Data File Path!");
            return;
        }
        System.out.println("Loading DBPedia Graph: "+dataGraphFilePath);
        int numberOfObjectsNotFound=0,numberOfSubjectsNotFound=0, numberOfLoops=0;

        try
        {
            Model model = ModelFactory.createDefaultModel();

            //model.read(dataGraphFilePath);
            Path input= Paths.get(dataGraphFilePath);
            model.read(input.toUri().toString());

            StmtIterator dataTriples = model.listStatements();

            while (dataTriples.hasNext()) {

                Statement stmt = dataTriples.nextStatement();
                String subjectNodeURI = stmt.getSubject().getURI().toLowerCase();
                if (subjectNodeURI.length() > 28) {
                    subjectNodeURI = subjectNodeURI.substring(28);
                }

                String predicate = stmt.getPredicate().getLocalName().toLowerCase();
                RDFNode object = stmt.getObject();
                String objectNodeURI;

                try {
                    if (object.isLiteral()) {
                        objectNodeURI = object.asLiteral().getString().toLowerCase();
                    } else {
                        objectNodeURI = object.toString().substring(object.toString().lastIndexOf("/")+1).toLowerCase();
                    }
                } catch (DatatypeFormatException e) {
                    //System.out.println("Invalid DataType Skipped!");
                    e.printStackTrace();
                    continue;
                }
                catch (Exception e)
                {
                    System.out.println(e.getMessage());
                    continue;
                }

                DataVertex subjVertex= (DataVertex) graph.getNode(subjectNodeURI);

                if (subjVertex==null) {

                    //System.out.println("Subject node not found: " + subjectNodeURI);
                    numberOfSubjectsNotFound++;
                    continue;
                }


                if (!object.isLiteral()) {
                    DataVertex objVertex= (DataVertex) graph.getNode(objectNodeURI);
                    if(objVertex==null)
                    {
                        //System.out.println("Object node not found: " + subjectNodeURI + "  ->  " + predicate + "  ->  " + objectNodeURI);
                        numberOfObjectsNotFound++;
                        continue;
                    }
                    else if (subjectNodeURI.equals(objectNodeURI)) {
                        //System.out.println("Loop found: " + subjectNodeURI + " -> " + objectNodeURI);
                        numberOfLoops++;
                        continue;
                    }
                    graph.addEdge(subjVertex, objVertex, new RelationshipEdge(predicate));
                }
                else
                {
                    subjVertex.addAttribute(new Attribute(predicate,objectNodeURI));
                }
            }
            System.out.println("Done Loading DBPedia Graph.");
            System.out.println("Number of Nodes: " + graph.getGraph().vertexSet().size());
            System.out.println("Number of Edges: " + graph.getGraph().edgeSet().size());
            System.out.println("Number of objects not found: " + numberOfObjectsNotFound);
            System.out.println("Number of subjects not found: " + numberOfSubjectsNotFound);
            System.out.println("Number of loops found: " + numberOfLoops);
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}

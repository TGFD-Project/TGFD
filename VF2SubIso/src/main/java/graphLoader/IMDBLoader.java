package graphLoader;

import infra.Attribute;
import infra.DataVertex;
import infra.RelationshipEdge;
import infra.TGFD;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.rdf.model.*;
import util.myConsole;
import util.properties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class IMDBLoader extends GraphLoader{

    public IMDBLoader(List <TGFD> alltgfd, String path) {

        super(alltgfd);
        loadIMDBGraph(path);
    }

    private void loadIMDBGraph(String dataGraphFilePath) {

        if (dataGraphFilePath == null || dataGraphFilePath.length() == 0) {
            myConsole.print("No Input Graph Data File Path!");
            return;
        }
        myConsole.print("Loading DBPedia Graph: "+dataGraphFilePath);

        try
        {
            Model model = ModelFactory.createDefaultModel();

            //model.read(dataGraphFilePath);
            Path input= Paths.get(dataGraphFilePath);
            model.read(input.toUri().toString());

            StmtIterator dataTriples = model.listStatements();

            while (dataTriples.hasNext()) {

                Statement stmt = dataTriples.nextStatement();
                String subjectNodeURL = stmt.getSubject().getURI().toLowerCase();
                if (subjectNodeURL.length() > 16) {
                    subjectNodeURL = subjectNodeURL.substring(16);
                }

                var temp=subjectNodeURL.split("/");
                if(temp.length!=2)
                {
                    // Error!
                    continue;
                }

                String subjectType=temp[0];
                String subjectID=temp[1];

                // ignore the node if the type is not in the validTypes and
                // optimizedLoadingBasedOnTGFD is true
                if(properties.myProperties.optimizedLoadingBasedOnTGFD && !validTypes.contains(subjectType))
                    continue;
                //int nodeId = subject.hashCode();
                DataVertex subjectVertex= (DataVertex) graph.getNode(subjectID);

                if (subjectVertex==null) {
                    subjectVertex=new DataVertex(subjectID,subjectType);
                    graph.addVertex(subjectVertex);
                }
                else {
                    subjectVertex.addTypes(subjectType);
                }

                String predicate = stmt.getPredicate().getLocalName().toLowerCase();
                RDFNode object = stmt.getObject();
                String objectNodeURI;

                try
                {
                    if (object.isLiteral())
                    {
                        objectNodeURI = object.asLiteral().getString().toLowerCase();
                        if(properties.myProperties.optimizedLoadingBasedOnTGFD && validAttributes.contains(predicate))
                            subjectVertex.addAttribute(new Attribute(predicate,objectNodeURI));
                    }
                    else
                    {
                        objectNodeURI = object.toString().toLowerCase();
                        if (objectNodeURI.length() > 16)
                            objectNodeURI = objectNodeURI.substring(16);

                        temp=objectNodeURI.split("/");
                        if(temp.length!=2)
                        {
                            // Error!
                            continue;
                        }

                        String objectType=temp[0];
                        String objectID=temp[1];

                        // ignore the node if the type is not in the validTypes and
                        // optimizedLoadingBasedOnTGFD is true
                        if(properties.myProperties.optimizedLoadingBasedOnTGFD && !validTypes.contains(objectType))
                            continue;
                        //int nodeId = subject.hashCode();
                        DataVertex objectVertex= (DataVertex) graph.getNode(objectID);

                        if (objectVertex==null) {
                            objectVertex=new DataVertex(objectID,objectType);
                            graph.addVertex(objectVertex);
                        }
                        else {
                            objectVertex.addTypes(objectType);
                        }
                        graph.addEdge(subjectVertex, objectVertex, new RelationshipEdge(predicate));
                    }
                } catch (DatatypeFormatException e) {
                    //System.out.println("Invalid DataType Skipped!");
                    e.printStackTrace();
                }
                catch (Exception e)
                {
                    System.out.println(e.getMessage());
                }
            }
            myConsole.print("Done. Nodes: " + graph.getGraph().vertexSet().size() + ",  Edges: " +graph.getGraph().edgeSet().size());
            //System.out.println("Done Loading DBPedia Graph.");
            //System.out.println("Number of subjects not found: " + numberOfSubjectsNotFound);
            //System.out.println("Number of loops found: " + numberOfLoops);
        }
        catch (Exception e)
        {
            myConsole.print(e.getMessage());
        }
    }
}

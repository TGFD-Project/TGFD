package graphLoader;

import infra.Attribute;
import infra.DataVertex;
import infra.RelationshipEdge;
import infra.TGFD;
import util.properties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class SyntheticLoader extends GraphLoader {

    //region --[Methods: Private]---------------------------------------

    /**
     * @param alltgfd List of TGFDs
     * @param dataPath Path to the Synthetic graph file
     */
    public SyntheticLoader(List<TGFD> alltgfd, List<String> dataPath)
    {
        super(alltgfd);

        for (String dataP:dataPath) {
            loadDataGraph(dataP);
        }
    }

    //endregion

    //region --[Methods: Private]---------------------------------------

    /**
     * This method will load DBPedia graph file
     * @param dataGraphFilePath Path to the graph file
     */
    private void loadDataGraph(String dataGraphFilePath) {

        if (dataGraphFilePath == null || dataGraphFilePath.length() == 0) {
            System.out.println("No Input Graph Data File Path!");
            return;
        }
        System.out.println("Loading Synthetic Graph: "+dataGraphFilePath);
        try
        {
            File file=new File(dataGraphFilePath);
            FileReader fr=new FileReader(file);
            BufferedReader br=new BufferedReader(fr);
            String line;
            while((line=br.readLine())!=null)
            {
                String []rdf=line.toLowerCase().split(" ");
                if(rdf.length==3)
                {
                    String []subject=rdf[0].split("_");
                    String []object=rdf[2].split("_");
                    if(subject.length==2 && object.length==2)
                    {
                        if(properties.myProperties.optimizedLoadingBasedOnTGFD && !validTypes.contains(subject[0]))
                            continue;
                        if(properties.myProperties.optimizedLoadingBasedOnTGFD && !validTypes.contains(object[0]))
                            continue;

                        DataVertex subjectVertex= (DataVertex) graph.getNode(subject[1]);
                        if (subjectVertex==null) {
                            subjectVertex=new DataVertex(subject[1],subject[0]);
                            graph.addVertex(subjectVertex);
                        }

                        // check if we have an attribute
                        if (object[0].equals("string"))
                        {
                            subjectVertex.addAttribute(new Attribute(rdf[1], object[1]));
                            graphSize++;
                        }
                        else // there is a node with a type
                        {
                            DataVertex objectVertex= (DataVertex) graph.getNode(object[1]);
                            if (objectVertex==null) {
                                objectVertex=new DataVertex(object[1],object[0]);
                                graph.addVertex(objectVertex);
                            }

                            graph.addEdge(subjectVertex, objectVertex, new RelationshipEdge(rdf[1]));
                            graphSize++;
                        }
                    }
                }
            }
            fr.close();    //close the stream and release the resources

            System.out.println("Done. Nodes: " + graph.getGraph().vertexSet().size() + ",  Edges: " +graph.getGraph().edgeSet().size());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //endregion

}

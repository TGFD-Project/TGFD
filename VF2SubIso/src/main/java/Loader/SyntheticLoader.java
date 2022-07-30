package Loader;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import Infra.Attribute;
import Infra.DataVertex;
import Infra.RelationshipEdge;
import ICs.TGFD;
import org.apache.commons.lang3.RandomStringUtils;
import Util.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

public class SyntheticLoader extends GraphLoader {

    //region --[Fields: Private]---------------------------------------

    private HashMap <String,Integer> typesDistribution =new HashMap <>();
    private HashMap <String, HashMap<String,String>> schema =new HashMap <>();

    //endregion

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
        BufferedReader br;
        FileReader fr=null;
        S3Object fullObject=null;
        try
        {
            if(Config.Amazon)
            {
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(Config.region)
                        //.withCredentials(new ProfileCredentialsProvider())
                        //.withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                        .build();

                //TODO: Need to check if the path is correct (should be in the form of bucketName/Key )
                String bucketName=dataGraphFilePath.substring(0,dataGraphFilePath.lastIndexOf("/"));
                String key=dataGraphFilePath.substring(dataGraphFilePath.lastIndexOf("/")+1);
                System.out.println("Downloading the object from Amazon S3 - Bucket name: " + bucketName +" - Key: " + key);
                fullObject = s3Client.getObject(new GetObjectRequest(bucketName, key));

                br = new BufferedReader(new InputStreamReader(fullObject.getObjectContent()));
            }
            else
            {
                File file=new File(dataGraphFilePath);
                fr=new FileReader(file);
                br=new BufferedReader(fr);
            }


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
                        if(Config.optimizedLoadingBasedOnTGFD && !validTypes.contains(subject[0]))
                            continue;
//                        if(Config.optimizedLoadingBasedOnTGFD && !validTypes.contains(object[0]))
//                            continue;

                        DataVertex subjectVertex= (DataVertex) graph.getNode(subject[1]);
                        if (subjectVertex==null) {
                            subjectVertex=new DataVertex(subject[1],subject[0]);
                            subjectVertex.addAttribute(new Attribute("name", RandomStringUtils.randomAlphabetic(10)));
                            graph.addVertex(subjectVertex);
                            if(typesDistribution.containsKey(subject[0]))
                                typesDistribution.put(subject[0], typesDistribution.get(subject[0])+1);
                            else
                                typesDistribution.put(subject[0],1);
                        }

                        // check if we have an attribute
                        if (object[0].equals("string") || object[0].equals("integer") || object[0].equals("datetime"))
                        {
                            subjectVertex.addAttribute(new Attribute(rdf[1], object[1]));
                            graphSize++;
                        }
                        else // there is a node with a type
                        {
                            DataVertex objectVertex= (DataVertex) graph.getNode(object[1]);
                            if (objectVertex==null) {
                                objectVertex=new DataVertex(object[1],object[0]);
                                objectVertex.addAttribute(new Attribute("name", RandomStringUtils.randomAlphabetic(10)));
                                graph.addVertex(objectVertex);
                                if(typesDistribution.containsKey(object[0]))
                                    typesDistribution.put(object[0], typesDistribution.get(object[0])+1);
                                else
                                    typesDistribution.put(object[0],1);
                            }

                            graph.addEdge(subjectVertex, objectVertex, new RelationshipEdge(rdf[1]));
                            graphSize++;

                            if(!schema.containsKey(subject[0]))
                                schema.put(subject[0],new HashMap <>());
                            schema.get(subject[0]).put(object[0],rdf[1]);
                        }
                    }
                }
            }
            if (fr != null) {
                fr.close();    //close the stream and release the resources
            }
            if (fullObject != null) {
                fullObject.close();
            }
            br.close();

            System.out.println("Done. Nodes: " + graph.getGraph().vertexSet().size() + ",  Edges: " +graph.getGraph().edgeSet().size());
            System.out.println("Number of types: " + typesDistribution.size() + "\n");
            typesDistribution.keySet().forEach(type -> System.out.print(type + ": " + typesDistribution.get(type) + " - "));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //endregion

    //region --[Properties: Public]------------------------------------

    public HashMap <String, Integer> getTypesDistribution() {
        return typesDistribution;
    }

    public HashMap <String, HashMap<String,String>> getSchema() {
        return schema;
    }

    //endregion
}

package Partitioner;

import Infra.Attribute;
import Infra.DataVertex;
import Infra.RelationshipEdge;
import Loader.DBPediaLoader;
import Loader.GraphLoader;
import Loader.IMDBLoader;
import Util.Config;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.jena.rdf.model.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class DBPediaPartitioner {

    private GraphLoader dbpedia;
    private final int numberOfPartitions;

    public DBPediaPartitioner(DBPediaLoader dbpedia, int numberOfPartitions)
    {
        this.dbpedia=dbpedia;
        this.numberOfPartitions=numberOfPartitions;
    }

    public void partition(String savingDirectory)
    {
        System.out.println("Start partitioning...");
        RangeBasedPartitioner partitioner=new RangeBasedPartitioner(dbpedia.getGraph());
        HashMap<DataVertex,Integer> partitionMapping=partitioner.fragment(numberOfPartitions);
        System.out.println("Partitioning done.");

        StringBuilder []types=new StringBuilder[numberOfPartitions];
        StringBuilder []data=new StringBuilder[numberOfPartitions];

        for (int i = 0; i < types.length; i++)
            types[i]=new StringBuilder();

        for (int i = 0; i < data.length; i++)
            data[i]=new StringBuilder();

        for (String typePath:Config.getFirstTypesFilePath())
            loadFile(typePath,partitionMapping,types);

        for (String dataPath:Config.getFirstDataFilePath())
            loadFile(dataPath,partitionMapping,data);

        try
        {
            for (int i = 0; i < types.length; i++) {
                FileWriter file = new FileWriter(savingDirectory + "DBPedia_type" + i + ".nt");
                file.write(types[i].toString());
                file.flush();
                file.close();
            }
            for (int i = 0; i < data.length; i++) {
                FileWriter file = new FileWriter(savingDirectory + "DBPedia_data" + i + ".nt");
                file.write(types[i].toString());
                file.flush();
                file.close();
            }

            FileWriter file = new FileWriter(savingDirectory + "map_"+numberOfPartitions+".txt");
            for (DataVertex v:partitionMapping.keySet()) {
                file.write(v.getVertexURI() + "\t" + partitionMapping.get(v));
            }
            file.flush();
            file.close();

            System.out.println("Done.");
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    //region --[Methods: Private]---------------------------------------

    /**
     * Load file in the format of (subject, predicate, object)
     * @param filePath Path to dbpedia file (data or type files)
     */
    private void loadFile(String filePath, HashMap<DataVertex,Integer> partitionMapping, StringBuilder []sb) {

        if (filePath == null || filePath.length() == 0) {
            System.out.println("No Input Node Types File Path!");
            return;
        }
        S3Object fullObject = null;
        BufferedReader br;
        try
        {
            System.out.println("Loading File: " + filePath);
            if(Config.Amazon)
            {
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(Config.region)
                        //.withCredentials(new ProfileCredentialsProvider())
                        //.withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                        .build();
                //TODO: Need to check if the path is correct (should be in the form of bucketName/Key )
                String bucketName=filePath.substring(0,filePath.lastIndexOf("/"));
                String key=filePath.substring(filePath.lastIndexOf("/")+1);
                System.out.println("Downloading the object from Amazon S3 - Bucket name: " + bucketName +" - Key: " + key);
                fullObject = s3Client.getObject(new GetObjectRequest(bucketName, key));

                br = new BufferedReader(new InputStreamReader(fullObject.getObjectContent()));
            }
            else
            {
                br = new BufferedReader(new FileReader(filePath));
            }

            String line= br.readLine();
            while (line!=null) {
                String []temp=line.toLowerCase().split(" ");
                if(temp.length>=1 && temp[0].startsWith("<http://dbpedia.org/resource/"))
                {
                    String nodeURI=temp[0].substring(temp[0].lastIndexOf("/")+1,temp[0].lastIndexOf(">"));
                    DataVertex v= (DataVertex) dbpedia.getGraph().getNode(nodeURI);
                    if (partitionMapping.containsKey(v)) {
                        int partitionID = partitionMapping.get(v);
                        sb[partitionID].append(line).append("\n");
                        //partitionData.get(partitionID).append(line).append("\n");
                    }
                }
                else
                    System.out.println("***ERROR*** ->" + line);
                line= br.readLine();
            }
            if (fullObject != null) {
                fullObject.close();
            }
            br.close();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    //endregion
}

package Partitioner;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import graphLoader.GraphLoader;
import graphLoader.IMDBLoader;
import infra.Attribute;
import infra.DataVertex;
import infra.RelationshipEdge;
import infra.Violation;
import org.apache.jena.rdf.model.*;
import util.ConfigParser;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

public class IMDBPartitioner {

    private GraphLoader imdb;
    private int numberOfPartitions;

    public IMDBPartitioner(IMDBLoader imdb, int numberOfPartitions)
    {
        this.imdb=imdb;
        this.numberOfPartitions=numberOfPartitions;
    }

    public void partition(String dataGraphFilePath, String savingDirectory)
    {
        SimpleGraphPartitioner partitioner=new SimpleGraphPartitioner(imdb.getGraph());
        HashMap<String,Integer> partitionMapping=partitioner.partition(numberOfPartitions);
        StringBuilder []partitionData=new StringBuilder[numberOfPartitions+1];

        if (dataGraphFilePath == null || dataGraphFilePath.length() == 0) {
            System.out.println("No Input Graph Data File Path!");
            return;
        }
        System.out.println("Partitioning IMDB Graph: "+dataGraphFilePath);

        S3Object fullObject = null;
        BufferedReader br;
        try
        {
            if(ConfigParser.Amazon)
            {
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(ConfigParser.region)
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
                br = new BufferedReader(new FileReader(dataGraphFilePath));
            }

            String line= br.readLine();
            while (line!=null) {
                String []temp=line.split(" ");
                if(temp.length>0)
                {
                    try {
                        String subjectNodeURL=temp[0].toLowerCase();
                        if (subjectNodeURL.length() > 16) {
                            subjectNodeURL = subjectNodeURL.substring(16);
                        }

                        var temp2=subjectNodeURL.split("/");
                        if(temp2.length!=2)
                        {
                            continue;
                        }
                        String subjectID=temp2[1];
                        if(partitionMapping.containsKey(subjectID))
                        {
                            int partitionID=partitionMapping.get(subjectID);
                            partitionData[partitionID].append(line).append("\n");
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                line= br.readLine();
            }
            System.out.println("Done.");
            if (fullObject != null) {
                fullObject.close();
            }
            br.close();

            for (int i=1;i<partitionData.length;i++)
            {
                savePartition(partitionData[i], savingDirectory+"/imdb"+i+".nt");
            }

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void savePartition(StringBuilder sb, String path)
    {
        try {
            FileWriter file = new FileWriter(path );
            file.write(sb.toString());
            file.close();
            System.out.println("Successfully wrote to the file: " + path);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

}

package Partitioner;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import Loader.GraphLoader;
import Loader.IMDBLoader;
import Infra.DataVertex;
import Util.Config;

import java.io.*;
import java.util.HashMap;

public class IMDBPartitioner {

    private GraphLoader imdb;
    private final int numberOfPartitions;

    public IMDBPartitioner(IMDBLoader imdb, int numberOfPartitions)
    {
        this.imdb=imdb;
        this.numberOfPartitions=numberOfPartitions;
    }

    public void partition(String dataGraphFilePath, String savingDirectory)
    {
        System.out.println("Start partitioning...");
        RangeBasedPartitioner partitioner=new RangeBasedPartitioner(imdb.getGraph());
        HashMap<DataVertex,Integer> partitionMapping=partitioner.fragment(numberOfPartitions);
        System.out.println("Partitioning done.");
        try
        {
            FileWriter []writers=new FileWriter[numberOfPartitions];
            for (int i=0;i<writers.length;i++) {
                writers[i]=new FileWriter(savingDirectory+"imdb"+i+".nt");
            }
            if (dataGraphFilePath == null || dataGraphFilePath.length() == 0) {
                System.out.println("No Input Graph Data File Path!");
                return;
            }
            System.out.println("Reading IMDB Graph: "+dataGraphFilePath);

            S3Object fullObject = null;
            BufferedReader br;

            if(Config.Amazon)
            {
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(Config.region)
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

            System.out.println("Reading the file line by line...");
            int lineCount=1;
            String line= br.readLine();
            while (line!=null) {
                String []temp=line.split(" ");
                if(temp.length>0 && temp[0].length()>2)
                {
                    try {
                        String subjectNodeURL=temp[0].toLowerCase().trim().substring(1,temp[0].trim().length()-1);
                        if (subjectNodeURL.length() > 16) {
                            subjectNodeURL = subjectNodeURL.substring(16);
                        }

                        var temp2=subjectNodeURL.split("/");
                        if(temp2.length==2) {
                            String subjectID = temp2[1];
                            DataVertex v= (DataVertex) imdb.getGraph().getNode(subjectID);
                            if (partitionMapping.containsKey(v)) {
                                int partitionID = partitionMapping.get(v);
                                writers[partitionID - 1].write(line + "\n");
                                //partitionData.get(partitionID).append(line).append("\n");
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                line= br.readLine();
                lineCount++;
                if(lineCount%1000000==0) {
                    System.out.println("Done reading lines: " + lineCount);
                    for (FileWriter writer:writers) {
                        writer.flush();
                    }
                }
            }
            System.out.println("Done.");
            if (fullObject != null) {
                fullObject.close();
            }
            br.close();

            for (FileWriter writer:writers) {
                writer.flush();
                writer.close();
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}

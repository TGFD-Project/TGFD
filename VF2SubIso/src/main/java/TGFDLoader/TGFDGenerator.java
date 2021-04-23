package TGFDLoader;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import infra.*;
import util.ConfigParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TGFDGenerator {

     private List<TGFD> tgfds;

    public TGFDGenerator(String path)
    {
        tgfds=new ArrayList<>();
        loadGraphPattern(path);
    }

    public List<TGFD> getTGFDs() {
        return tgfds;
    }

    private void loadGraphPattern(String path) {

        if(path.equals(""))
            return;
        HashMap<String, PatternVertex> allVertices=new HashMap<>();
        VF2PatternGraph currentPattern=null;
        TGFD currentTGFD=new TGFD();

        S3Object fullObject = null;
        BufferedReader br=null;

        String line="";
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
                String bucketName=path.substring(0,path.lastIndexOf("/"));
                String key=path.substring(path.lastIndexOf("/")+1);
                System.out.println("Downloading the object from Amazon S3 - Bucket name: " + bucketName +" - Key: " + key);
                fullObject = s3Client.getObject(new GetObjectRequest(bucketName, key));

                br = new BufferedReader(new InputStreamReader(fullObject.getObjectContent()));
            }
            else
            {
                br = new BufferedReader(new FileReader(path));
            }

            while ((line=br.readLine())!=null) {
                line = line.toLowerCase();
                if(line.startsWith("tgfd"))
                {
                    if(currentPattern!=null)
                    {
                        currentTGFD.setPattern(currentPattern);
                        tgfds.add(currentTGFD);
                    }
                    currentTGFD=new TGFD();
                    currentPattern=new VF2PatternGraph();
                    allVertices=new HashMap<>();
                    String []args = line.split("#");
                    if(args.length==2)
                        currentTGFD.setName(args[1]);
                }
                else if(line.startsWith("vertex"))
                {
                    String []args = line.split("#");
                    PatternVertex v=new PatternVertex(args[2]);
                    for (int i=3;i<args.length;i++)
                    {
                        if(args[i].contains("$"))
                        {
                            String []attr=args[i].split("\\$");
                            v.addAttribute(new Attribute(attr[0],attr[1]));
                        }
                        else
                        {
                            v.addAttribute(new Attribute(args[i]));
                        }
                    }
                    currentPattern.addVertex(v);
                    allVertices.put(args[1],v);
                }
                else if(line.startsWith("edge"))
                {
                    String []args = line.split("#");
                    currentPattern.addEdge(allVertices.get(args[1]),allVertices.get(args[2]),new RelationshipEdge(args[3]));
                }
                else if(line.startsWith("diameter"))
                {
                    String []args = line.split("#");
                    currentPattern.setDiameter(Integer.parseInt(args[1]));
                }
                else if(line.startsWith("literal"))
                {
                    String[] args = line.split("#");

                    String []temp=args[2].split("\\$");
                    Literal currentLiteral=null;

                    //Generate the literal based on the size, either constant or variable

                    if(temp.length==3)
                        currentLiteral=new ConstantLiteral(temp[0].toLowerCase(),
                                temp[1].toLowerCase(),temp[2].toLowerCase());
                    else if(temp.length==4)
                        currentLiteral=new VariableLiteral(temp[0].toLowerCase(),
                                temp[1].toLowerCase(),temp[2].toLowerCase(),temp[3].toLowerCase());

                    if(args[1].equals("x"))
                        currentTGFD.getDependency().addLiteralToX(currentLiteral);
                    else if(args[1].equals("y"))
                        currentTGFD.getDependency().addLiteralToY(currentLiteral);
                }
                else if(line.startsWith("delta"))
                {
                    String[] args = line.split("#");
                    Period pMin=Period.ofDays(Integer.parseInt(args[1]));
                    Period pMax=Period.ofDays(Integer.parseInt(args[2]));
                    Duration granularity=Duration.ofDays(Integer.parseInt(args[3]));
                    currentTGFD.setDelta(new Delta(pMin,pMax,granularity));
                }
            }
            if(currentPattern!=null)
            {
                currentTGFD.setPattern(currentPattern);
                tgfds.add(currentTGFD);
            }

            if (fullObject != null) {
                fullObject.close();
            }
            br.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

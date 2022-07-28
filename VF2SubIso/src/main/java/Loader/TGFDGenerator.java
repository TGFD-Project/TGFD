package Loader;

import ICs.TGFD;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import Infra.*;
import Util.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
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

        System.out.println("Loading TGFDs from the path: " + path);
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
            if(Config.Amazon)
            {
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(Config.region)
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
                        currentPattern.setDiameter();
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
                            if(attr.length==2)
                                v.addAttribute(new Attribute(attr[0],attr[1]));
                            else if(attr.length>2)
                            {
                                MultiValueAttribute attribute = new MultiValueAttribute(attr[0]);
                                Arrays.stream(attr, 1, attr.length).forEach(attribute::addValue);
                                v.addAttribute(attribute);
                            }
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
                //currentPattern.setDiameter();
                currentTGFD.setPattern(currentPattern);
                tgfds.add(currentTGFD);
            }

            if (fullObject != null) {
                fullObject.close();
            }
            br.close();
            System.out.println("Number of TGFDs loaded: " + tgfds.size());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public String toTextFormat(TGFD tgfd)
    {
        StringBuilder res=new StringBuilder();
        res.append("tgfd#").append(tgfd.getName()).append("\n");
        int index=1;
        HashMap<Vertex,String> map=new HashMap<>();
        for (Vertex v:tgfd.getPattern().getPattern().vertexSet()) {
            map.put(v,"v"+index);
            res.append("vertex#").append("v").append(index);
            v.getTypes().forEach(type -> res.append("#").append(type));
            v.getAllAttributesList().forEach(attr -> {
                if (attr.isNULL())
                {
                    res.append("#").append(attr.getAttrName());
                }
                else
                {
                    res.append("#").append(attr.getAttrName()).append("$").append(attr.getAttrValue());
                }
            });
            res.append("\n");
            index++;
        }
        tgfd.getPattern()
                .getPattern()
                .edgeSet()
                .forEach(e -> res
                        .append("edge#")
                        .append(map.get(e.getSource()))
                        .append("#")
                        .append(map.get(e.getTarget()))
                        .append("#")
                        .append(e.getLabel())
                        .append("\n"));

        res.append("diameter#")
                .append(tgfd.getPattern().getDiameter())
                .append("\n");

        tgfd.getDependency().getX().forEach(literal -> {
            res.append("literal#x#");
            if (literal instanceof ConstantLiteral)
            {
                ConstantLiteral l = (ConstantLiteral) literal;
                res.append(l.getVertexType())
                        .append("$")
                        .append(l.getAttrName())
                        .append("$")
                        .append(l.getAttrValue())
                        .append("\n");
            }
            else if (literal instanceof VariableLiteral)
            {
                VariableLiteral l = (VariableLiteral) literal;
                res.append(l.getVertexType_1())
                        .append("$")
                        .append(l.getAttrName_1())
                        .append("$")
                        .append(l.getVertexType_2())
                        .append("$")
                        .append(l.getAttrName_2())
                        .append("\n");
            }
        });

        tgfd.getDependency().getY().forEach(literal -> {
            res.append("literal#y#");
            if (literal instanceof ConstantLiteral)
            {
                ConstantLiteral l = (ConstantLiteral) literal;
                res.append(l.getVertexType())
                        .append("$")
                        .append(l.getAttrName())
                        .append("$")
                        .append(l.getAttrValue())
                        .append("\n");
            }
            else if (literal instanceof VariableLiteral)
            {
                VariableLiteral l = (VariableLiteral) literal;
                res.append(l.getVertexType_1())
                        .append("$")
                        .append(l.getAttrName_1())
                        .append("$")
                        .append(l.getVertexType_2())
                        .append("$")
                        .append(l.getAttrName_2())
                        .append("\n");
            }
        });

        res.append("delta#")
                .append(tgfd.getDelta().getMin().getDays())
                .append("#")
                .append(tgfd.getDelta().getMax().getDays())
                .append("#")
                .append(tgfd.getDelta().getGranularity().toDays())
                .append("\n");


        return  res.toString();
    }
}

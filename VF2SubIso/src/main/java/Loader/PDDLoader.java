package Loader;

import ICs.TGFD;
import Infra.*;
import Util.Config;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.jena.rdf.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PDDLoader extends GraphLoader {

    //region --[Methods: Public]---------------------------------------

    /**
     * @param alltgfd List of TGFDs
     * @param dataPath Path to the PDD graph file
     */
    public PDDLoader(List<TGFD> alltgfd, ArrayList<String> dataPath)
    {
        super(alltgfd);

        for (String dataP:dataPath) {
            loadNodeMap(dataP);
        }

        for (String dataP:dataPath) {
            loadDataGraph(dataP);
        }
    }

    public VF2DataGraph getGraph() {
        return graph;
    }

    //endregion

    //region --[Methods: Private]---------------------------------------

    /**
     * Load file in the format of (subject, predicate, object)
     * This will load the type file and create a DataVertex for each different subject with type of object
     * @param nodeTypesPath Path to the Type file
     */
    private void loadNodeMap(String nodeTypesPath) {

        if (nodeTypesPath == null || nodeTypesPath.length() == 0) {
            System.out.println("No Input Node Types File Path!");
            return;
        }
        S3Object fullObject = null;
        BufferedReader br=null;
        try
        {
            Model model = ModelFactory.createDefaultModel();
            System.out.println("Loading Node Types: " + nodeTypesPath);

            if(Config.Amazon)
            {
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(Config.region)
                        //.withCredentials(new ProfileCredentialsProvider())
                        //.withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                        .build();

                //TODO: Need to check if the path is correct (should be in the form of bucketName/Key )
                String bucketName=nodeTypesPath.substring(0,nodeTypesPath.lastIndexOf("/"));
                String key=nodeTypesPath.substring(nodeTypesPath.lastIndexOf("/")+1);
                System.out.println("Downloading the object from Amazon S3 - Bucket name: " + bucketName +" - Key: " + key);
                fullObject = s3Client.getObject(new GetObjectRequest(bucketName, key));

                br = new BufferedReader(new InputStreamReader(fullObject.getObjectContent()));
                model.read(br,null, Config.language);
            }
            else
            {
                Path input= Paths.get(nodeTypesPath);
                model.read(input.toUri().toString());
            }

            StmtIterator typeTriples = model.listStatements();

            while (typeTriples.hasNext()) {
                Statement stmt = typeTriples.nextStatement();

                String nodeURI = stmt.getSubject().getURI().toLowerCase();
                nodeURI = nodeURI.substring(nodeURI.lastIndexOf("/")+1).toLowerCase();

                String predicate = stmt.getPredicate().getLocalName().toLowerCase();
                if(predicate.contains("property") || predicate.equals("type"))
                {
                    String nodeType = stmt.getObject().asResource().getLocalName().toLowerCase();
                    // ignore the node if the type is not in the validTypes and
                    // optimizedLoadingBasedOnTGFD is true
                    if(Config.optimizedLoadingBasedOnTGFD && !validTypes.contains(nodeType))
                        continue;
                    //int nodeId = subject.hashCode();
                    //if(nodeTypesPath.contains("patients_basic.nt"))
                    //    System.out.println("Subject: "+ nodeURI+ " ->" + predicate);

                    DataVertex v= (DataVertex) graph.getNode(nodeURI);

                    if (v==null) {
                        v=new DataVertex(nodeURI,nodeType);
                        graph.addVertex(v);
                    }
                    else {
                        v.addType(nodeType);
                    }
                }
                else if(predicate.equals("diagnoses_icd9"))
                {
                    RDFNode object = stmt.getObject();
                    String objectNodeURI;

                    if (object.isLiteral()) {
                        objectNodeURI = object.asLiteral().getString().toLowerCase();
                    } else {
                        objectNodeURI = object.toString().substring(object.toString().lastIndexOf("/")+1).toLowerCase();
                    }

                    if (!object.isLiteral()) {
                        DataVertex objVertex= (DataVertex) graph.getNode(objectNodeURI);
                        if(objVertex==null)
                        {
                            objVertex=new DataVertex(objectNodeURI,"disease");
                            graph.addVertex(objVertex);
                        }
                        else
                        {
                            objVertex.addType("disease");
                        }
                    }
                }
                else if(predicate.equals("take_drugbank_id"))
                {
                    RDFNode object = stmt.getObject();
                    String objectNodeURI;

                    if (object.isLiteral()) {
                        objectNodeURI = object.asLiteral().getString().toLowerCase();
                    } else {
                        objectNodeURI = object.toString().substring(object.toString().lastIndexOf("/")+1).toLowerCase();
                    }

                    if (!object.isLiteral()) {
                        DataVertex objVertex= (DataVertex) graph.getNode(objectNodeURI);
                        if(objVertex==null)
                        {
                            objVertex=new DataVertex(objectNodeURI,"drug");
                            graph.addVertex(objVertex);
                        }
                        else
                        {
                            objVertex.addType("drug");
                        }
                    }
                }
                else if(predicate.equals("interact"))
                {
                    DataVertex subject= (DataVertex) graph.getNode(nodeURI);

                    if (subject==null) {
                        subject=new DataVertex(nodeURI,"drug");
                        graph.addVertex(subject);
                    }
                    else {
                        subject.addType("drug");
                    }

                    RDFNode object = stmt.getObject();
                    String objectNodeURI;

                    if (object.isLiteral()) {
                        objectNodeURI = object.asLiteral().getString().toLowerCase();
                    } else {
                        objectNodeURI = object.toString().substring(object.toString().lastIndexOf("/")+1).toLowerCase();
                    }

                    if (!object.isLiteral()) {
                        DataVertex objVertex= (DataVertex) graph.getNode(objectNodeURI);
                        if(objVertex==null)
                        {
                            objVertex=new DataVertex(objectNodeURI,"drug");
                            graph.addVertex(objVertex);
                        }
                        else
                        {
                            objVertex.addType("drug");
                        }
                    }
                }

            }
            System.out.println("Done. Number of Types: " + graph.getSize());
            if (fullObject != null) {
                fullObject.close();
            }
            if (br != null) {
                br.close();
            }
            model.close();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    /**
     * This method will load PDD graph file
     * @param dataGraphFilePath Path to the graph file
     */
    private void loadDataGraph(String dataGraphFilePath) {

        if (dataGraphFilePath == null || dataGraphFilePath.length() == 0) {
            System.out.println("No Input Graph Data File Path!");
            return;
        }
        System.out.println("Loading PDD Graph: "+dataGraphFilePath);
        int numberOfObjectsNotFound=0,numberOfSubjectsNotFound=0;

        S3Object fullObject = null;
        BufferedReader br=null;
        try
        {
            Model model = ModelFactory.createDefaultModel();
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
                model.read(br,null, Config.language);
            }
            else
            {
                Path input= Paths.get(dataGraphFilePath);
                model.read(input.toUri().toString());
            }

            StmtIterator dataTriples = model.listStatements();

            while (dataTriples.hasNext()) {

                Statement stmt = dataTriples.nextStatement();
                String subjectNodeURI = stmt.getSubject().getURI().toLowerCase();
                subjectNodeURI = subjectNodeURI.substring(subjectNodeURI.lastIndexOf("/")+1).toLowerCase();

                String predicate = stmt.getPredicate().getLocalName().toLowerCase();
                if(predicate.equals("type"))
                    continue;

                if(predicate.equals("take_drugbank_id"))
                {
                    RDFNode object = stmt.getObject();
                    String objectNodeURI = object.toString();

                    DataVertex subjVertex= (DataVertex) graph.getNode(subjectNodeURI);
                    subjVertex.addAttribute(new Attribute(predicate,objectNodeURI));
                    continue;
                }

                RDFNode object = stmt.getObject();
                String objectNodeURI;

                if (object.isLiteral()) {
                    objectNodeURI = object.asLiteral().getString().toLowerCase();
                } else {
                    objectNodeURI = object.toString().substring(object.toString().lastIndexOf("/")+1).toLowerCase();
                }

                DataVertex subjVertex= (DataVertex) graph.getNode(subjectNodeURI);

                if (subjVertex==null) {

                    //System.out.println("Subject node not found: " + subjectNodeURI);
                    numberOfSubjectsNotFound++;
//                    if(dataGraphFilePath.contains("patients_basic.nt"))
//                        System.out.println("Subject missing: "+ subjectNodeURI+ " ->" + predicate);
                    continue;
                }

                if (!object.isLiteral()) {
                    DataVertex objVertex= (DataVertex) graph.getNode(objectNodeURI);
                    if(objVertex==null)
                    {
                        //System.out.println("Object node not found: " + subjectNodeURI + "  ->  " + predicate + "  ->  " + objectNodeURI);
                        numberOfObjectsNotFound++;
//                        System.out.println("Object missing: "+ objectNodeURI+ " ->" + predicate);
                        continue;
                    }
                    else if (subjectNodeURI.equals(objectNodeURI)) {
                        //System.out.println("Loop found: " + subjectNodeURI + " -> " + objectNodeURI);
                        continue;
                    }
                    graph.addEdge(subjVertex, objVertex, new RelationshipEdge(predicate));
                    graphSize++;
                }
                else
                {
                    if(!Config.optimizedLoadingBasedOnTGFD || validAttributes.contains(predicate))
                    {
                        objectNodeURI = roundAttributeValue(predicate,objectNodeURI);
                        subjVertex.addAttribute(new Attribute(predicate,objectNodeURI));
                    }
                }
            }
            System.out.println("Subjects and Objects not found: " + numberOfSubjectsNotFound + " ** " + numberOfObjectsNotFound);
            System.out.println("Done. Nodes: " + graph.getGraph().vertexSet().size() + ",  Edges: " +graph.getGraph().edgeSet().size());
            //System.out.println("Number of subjects not found: " + numberOfSubjectsNotFound);
            //System.out.println("Number of loops found: " + numberOfLoops);

            if (fullObject != null) {
                fullObject.close();
            }
            if (br != null) {
                br.close();
            }
            model.close();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private String roundAttributeValue(String predicate, String value)
    {
        if(predicate.equals("bmi_first"))
        {
            double bmi = Double.parseDouble(value);
            if(bmi<=18.5)
                return "18.5";
            else if(bmi<=24.9)
                return "24.9";
            else if(bmi<=29.9)
                return "29.9";
            else
                return "35";
        }
        else if(predicate.equals("age"))
        {
            double age = Double.parseDouble(value);
            if(age<=14)
                return "children";
            else if(age<=24)
                return "youth";
            else if(age<=64)
                return "adult";
            else
                return "senior";
        }
        return value;
    }

    //endregion

}
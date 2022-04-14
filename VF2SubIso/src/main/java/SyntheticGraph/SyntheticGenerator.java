package SyntheticGraph;

import ChangeExploration.*;
import Loader.SyntheticLoader;
import Infra.Attribute;
import Infra.DataVertex;
import Infra.RelationshipEdge;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.*;

public class SyntheticGenerator {

    private SyntheticLoader graph;
    private int changeID=1;
    public SyntheticGenerator(SyntheticLoader graph)
    {
        this.graph=graph;
    }

    public HashMap<Integer, List<Change>> generateSnapshot(int numberOfSnapshots,double changeRate)
    {
        HashMap<Integer, List<Change>> snapshotsChanges=new HashMap <>();
        for (int i=0;i<numberOfSnapshots;i++)
        {
            List<Change> changes=generateChange(changeRate);
            snapshotsChanges.put(i+2,changes);
            graph.updateGraphWithChanges(changes);
        }
        return snapshotsChanges;
    }

    private List<Change> generateChange(double changeRate)
    {
        List<Change> changes=new ArrayList <>();
        int numberOfChanges= (int) (graph.getGraphSize() *changeRate);
        HashMap <String, HashMap<String,String>> schema= graph.getSchema();



        Random random = new Random();

        // Random edges are picked to be deleted!
        RelationshipEdge[] arrayEdges = graph.getGraph().getGraph().edgeSet().
                toArray(new RelationshipEdge[graph.getGraph().getGraph().edgeSet().size()]);
        HashSet<Integer> alreadyPicked=new HashSet <>();
        for (int i=0;i<numberOfChanges/8;i++)
        {
            int rndmNumber = random.nextInt(arrayEdges.length);
            while (alreadyPicked.contains(rndmNumber))
                rndmNumber = random.nextInt(arrayEdges.length);
            changes.add(new EdgeChange(ChangeType.deleteEdge,changeID++,arrayEdges[rndmNumber]));
            alreadyPicked.add(rndmNumber);
        }

        // Random new edges are inserted
        String []sourceTypes=schema.keySet().toArray(new String[schema.keySet().size()]);
        for (int i=0;i<(3*numberOfChanges)/4;i+=3)
        {
            int rndmNumber = random.nextInt(sourceTypes.length);
            String srcType=sourceTypes[rndmNumber];
            String []destinationTypes=schema.get(srcType).keySet().toArray(new String[schema.get(srcType).keySet().size()]);
            rndmNumber = random.nextInt(destinationTypes.length);
            String dstType=destinationTypes[rndmNumber];

            DataVertex srcVertex=new DataVertex(RandomStringUtils.randomAlphabetic(10),srcType);

            DataVertex dstVertex=new DataVertex(RandomStringUtils.randomAlphabetic(10),dstType);

            Change c1=new VertexChange(ChangeType.insertVertex,changeID++,srcVertex);
            Change c2=new VertexChange(ChangeType.insertVertex,changeID++,dstVertex);
            Change c3=new EdgeChange(ChangeType.insertEdge,changeID++,
                    srcVertex.getVertexURI(),dstVertex.getVertexURI(),schema.get(srcType).get(dstType));

            changes.add(c1);
            changes.add(c2);
            changes.add(c3);
        }

        // Random nodes are picked to change their attribute
        DataVertex[] vertices = graph.getGraph().getGraph().vertexSet().
                toArray(new DataVertex[graph.getGraph().getGraph().vertexSet().size()]);
        alreadyPicked=new HashSet <>();
        for (int i=0;i<numberOfChanges/8;i++)
        {
            int rndmNumber = random.nextInt(vertices.length);
            while (alreadyPicked.contains(rndmNumber))
                rndmNumber = random.nextInt(vertices.length);
            changes.add(new AttributeChange(ChangeType.changeAttr,changeID++,
                    vertices[rndmNumber].getVertexURI(),new Attribute("name",RandomStringUtils.randomAlphabetic(10))));
            alreadyPicked.add(rndmNumber);
        }
        return changes;
    }

}

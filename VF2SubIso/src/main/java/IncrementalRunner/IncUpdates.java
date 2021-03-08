package IncrementalRunner;

import VF2Runner.VF2SubgraphIsomorphism;
import changeExploration.*;
import infra.*;
import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class IncUpdates {

    private VF2DataGraph baseGraph;

    private VF2SubgraphIsomorphism VF2;

    private int numberOfIgnoredEdgeChanges=0;
    private int numberOfIgnoredَAttributeChange=0;

    public IncUpdates(VF2DataGraph baseGraph)
    {
        this.baseGraph=baseGraph;
        this.VF2= new VF2SubgraphIsomorphism();
    }

    public HashMap<String,IncrementalChange> updateGraph(Change change, List<TGFD> affectedTGFDs)
    {
        if(change instanceof EdgeChange)
        {
            EdgeChange edgeChange=(EdgeChange) change;
            DataVertex v1= (DataVertex) baseGraph.getNode(edgeChange.getSrc());
            DataVertex v2= (DataVertex) baseGraph.getNode(edgeChange.getDst());
            if(v1==null || v2==null)
            {
                // Node doesn't exist in the base graph, we need to igonre the change
                // We keep the number of these ignored edges in a variable
                numberOfIgnoredEdgeChanges++;
                return null;
            }
            if(edgeChange.getTypeOfChange()== ChangeType.insertEdge)
                return updateGraphByAddingNewEdge(v1,v2,new RelationshipEdge(edgeChange.getLabel()),affectedTGFDs);
            else if(edgeChange.getTypeOfChange()== ChangeType.deleteEdge)
                return updateGraphByDeletingAnEdge(v1,v2,new RelationshipEdge(edgeChange.getLabel()),affectedTGFDs);
            else
                throw new IllegalArgumentException("The change is instnace of EdgeChange, but type of change is: " + edgeChange.getTypeOfChange());
        }
        else if(change instanceof AttributeChange)
        {
            AttributeChange attributeChange=(AttributeChange) change;
            DataVertex v1=(DataVertex) baseGraph.getNode(attributeChange.getUri());
            if(v1==null)
            {
                // Node doesn't exist in the base graph, we need to igonre the change
                // We store the number of these ignored changes
                numberOfIgnoredَAttributeChange++;
                return null;
            }
            if(attributeChange.getTypeOfChange()==ChangeType.changeAttr || attributeChange.getTypeOfChange()==ChangeType.insertAttr)
            {
                return updateGraphByUpdatingAnAttribute(v1,attributeChange.getAttribute(),affectedTGFDs);
            }
            else if(attributeChange.getTypeOfChange()==ChangeType.deleteAttr)
            {
                return updateGraphByDeletingAnAttribute(v1,attributeChange.getAttribute(),affectedTGFDs);
            }
            else
                throw new IllegalArgumentException("The change is instnace of AttributeChange, but type of change is: " + attributeChange.getTypeOfChange());
        }
        else
            return null;
    }

    public void AddNewVertices(List<Change> allChange)
    {
        for (Change change:allChange) {
            if(change instanceof VertexChange && change.getTypeOfChange()==ChangeType.insertVertex)
            {
                baseGraph.addVertex(((VertexChange) change).getVertex());
            }
        }
    }

    public int getNumberOfIgnoredEdgeChanges() {
        return numberOfIgnoredEdgeChanges;
    }

    private HashMap<String,IncrementalChange> updateGraphByAddingNewEdge(
            DataVertex v1, DataVertex v2, RelationshipEdge edge, List<TGFD> affectedTGFDs)
    {

        Graph<Vertex, RelationshipEdge> subgraph= baseGraph.getSubGraphByDiameter(v1,getMaxDiameter(affectedTGFDs));

        HashMap<String,IncrementalChange> incrementalChangeHashMap=new HashMap <>();

        // run VF2
        for (TGFD tgfd:affectedTGFDs) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeChange = VF2.execute(subgraph,tgfd.getPattern(),false);
            IncrementalChange incrementalChange=new IncrementalChange(beforeChange,tgfd.getPattern());
            incrementalChangeHashMap.put(tgfd.getName(),incrementalChange);
        }

        //perform the change...
        if(!subgraph.containsVertex(v2))
        {
            subgraph.addVertex(v2);
        }
        subgraph.addEdge(v1,v2,edge);
        baseGraph.addEdge(v1, v2,edge);

        // Run VF2 again...
        for (TGFD tgfd:affectedTGFDs) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> afterChange = VF2.execute(subgraph,tgfd.getPattern(),false);
            incrementalChangeHashMap.get(tgfd.getName()).addAfterMatches(afterChange);
        }

        return incrementalChangeHashMap;
    }

    private HashMap<String,IncrementalChange> updateGraphByDeletingAnEdge(
            DataVertex v1, DataVertex v2, RelationshipEdge edge, List<TGFD> affectedTGFDs)
    {
        Graph<Vertex, RelationshipEdge> subgraph= baseGraph.getSubGraphByDiameter(v1,getMaxDiameter(affectedTGFDs));

        HashMap<String,IncrementalChange> incrementalChangeHashMap=new HashMap <>();

        // run VF2
        for (TGFD tgfd:affectedTGFDs) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeChange = VF2.execute(subgraph,tgfd.getPattern(),false);
            IncrementalChange incrementalChange=new IncrementalChange(beforeChange,tgfd.getPattern());
            incrementalChangeHashMap.put(tgfd.getName(),incrementalChange);
        }

        // Now, perform the change and remove the edge from the subgraph
        for (RelationshipEdge e:subgraph.outgoingEdgesOf(v1)) {
            DataVertex target=(DataVertex) e.getTarget();
            if(target.getVertexURI().equals(v2.getVertexURI()) && edge.getLabel().equals(e.getLabel()))
            {
                subgraph.removeEdge(e);
                break;
            }
        }
        //remove from the base graph.
        baseGraph.removeEdge(v1,v2,edge);

        // Run VF2 again...
        for (TGFD tgfd:affectedTGFDs) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> afterChange = VF2.execute(subgraph,tgfd.getPattern(),false);
            incrementalChangeHashMap.get(tgfd.getName()).addAfterMatches(afterChange);
        }

        return incrementalChangeHashMap;
    }

    private HashMap<String,IncrementalChange> updateGraphByUpdatingAnAttribute(
            DataVertex v1, Attribute attribute, List<TGFD> affectedTGFDs)
    {
        Graph<Vertex, RelationshipEdge> subgraph= baseGraph.getSubGraphByDiameter(v1,getMaxDiameter(affectedTGFDs));

        HashMap<String,IncrementalChange> incrementalChangeHashMap=new HashMap <>();

        // run VF2
        for (TGFD tgfd:affectedTGFDs) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeChange = VF2.execute(subgraph,tgfd.getPattern(),false);
            IncrementalChange incrementalChange=new IncrementalChange(beforeChange,tgfd.getPattern());
            incrementalChangeHashMap.put(tgfd.getName(),incrementalChange);
        }

        //Now, perform the change...
        v1.setOrAddAttribute(attribute);

        // Run VF2 again...
        for (TGFD tgfd:affectedTGFDs) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> afterChange = VF2.execute(subgraph,tgfd.getPattern(),false);
            incrementalChangeHashMap.get(tgfd.getName()).addAfterMatches(afterChange);
        }

        return incrementalChangeHashMap;
    }

    private HashMap<String,IncrementalChange> updateGraphByDeletingAnAttribute(
            DataVertex v1, Attribute attribute, List<TGFD> affectedTGFDs)
    {
        Graph<Vertex, RelationshipEdge> subgraph= baseGraph.getSubGraphByDiameter(v1,getMaxDiameter(affectedTGFDs));
        HashMap<String,IncrementalChange> incrementalChangeHashMap=new HashMap <>();

        // run VF2
        for (TGFD tgfd:affectedTGFDs) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeChange = VF2.execute(subgraph,tgfd.getPattern(),false);
            IncrementalChange incrementalChange=new IncrementalChange(beforeChange,tgfd.getPattern());
            incrementalChangeHashMap.put(tgfd.getName(),incrementalChange);
        }

        //Now, perform the change...
        v1.deleteAttribute(attribute);

        // Run VF2 again...
        for (TGFD tgfd:affectedTGFDs) {
            Iterator<GraphMapping<Vertex, RelationshipEdge>> afterChange = VF2.execute(subgraph,tgfd.getPattern(),false);
            incrementalChangeHashMap.get(tgfd.getName()).addAfterMatches(afterChange);
        }
        return incrementalChangeHashMap;
    }

    private int getMaxDiameter(List<TGFD> tgfds)
    {
        int maxDiameter=0;
        for (TGFD tgfd:tgfds) {
            if(tgfd.getPattern().getDiameter()>maxDiameter)
                maxDiameter=tgfd.getPattern().getDiameter();
        }
        return maxDiameter;
    }

}

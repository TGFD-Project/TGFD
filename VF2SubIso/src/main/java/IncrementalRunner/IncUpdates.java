package IncrementalRunner;

import VF2Runner.VF2SubgraphIsomorphism;
import changeExploration.*;
import infra.*;
import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;

import java.util.Iterator;
import java.util.List;

public class IncUpdates {

    private VF2DataGraph baseGraph;

    private VF2SubgraphIsomorphism VF2;

    private VF2PatternGraph patternGraph;

    public IncUpdates(VF2DataGraph graph, VF2SubgraphIsomorphism VF2, VF2PatternGraph patternGraph)
    {
        this.baseGraph=graph;
        this.VF2= VF2;
        this.patternGraph=patternGraph;
    }

    public Iterator<GraphMapping<Vertex, RelationshipEdge>> updateGraph(Change change)
    {
        if(change instanceof EdgeChange)
        {
            EdgeChange edgeChange=(EdgeChange) change;
            DataVertex v1= (DataVertex) baseGraph.getNode(edgeChange.getSrc());
            DataVertex v2= (DataVertex) baseGraph.getNode(edgeChange.getDst());
            if(edgeChange.getTypeOfChange()== ChangeType.insertEdge)
                return updateGraphByAddingNewEdge(v1,v2,new RelationshipEdge(edgeChange.getLabel()));
            else if(edgeChange.getTypeOfChange()== ChangeType.deleteEdge)
                return updateGraphByDeletingAnEdge(v1,v2,new RelationshipEdge(edgeChange.getLabel()));
            else
                throw new IllegalArgumentException("The change is instnace of EdgeChange, but type of change is: " + edgeChange.getTypeOfChange());
        }
        else if(change instanceof AttributeChange)
        {
            AttributeChange attributeChange=(AttributeChange) change;
            DataVertex v1=(DataVertex) baseGraph.getNode(attributeChange.getUri());
            if(attributeChange.getTypeOfChange()==ChangeType.changeAttr)
            {
                return updateGraphByUpdatingAnAttribute(v1,attributeChange.getAttribute());
            }
            else if(attributeChange.getTypeOfChange()==ChangeType.deleteAttr)
            {
                return updateGraphByDeletingAnAttribute(v1,attributeChange.getAttribute());
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

    private Iterator<GraphMapping<Vertex, RelationshipEdge>> updateGraphByAddingNewEdge(
            DataVertex v1, DataVertex v2, RelationshipEdge edge)
    {
        Graph<Vertex, RelationshipEdge> subgraph= baseGraph.getSubGraphByDiameter(v1,patternGraph.getDiameter());

        //perform the change...

        subgraph.addEdge(v1,v2,edge);
        baseGraph.addEdge(v1, v2,edge);

        // Perform VF2 again...
        //myConsole.print("========================AFTER CHANGE========================");
        //myConsole.print("+++++NEW EDGE: "+ v1.getVertexURI() + " --> (" +edge.getLabel()
        //        +") --> " + v2.getVertexURI()+" +++++");
        Iterator<GraphMapping<Vertex, RelationshipEdge>> newMatches = VF2.execute(subgraph,patternGraph,false);

        return newMatches;
    }

    private Iterator<GraphMapping<Vertex, RelationshipEdge>> updateGraphByDeletingAnEdge(
            DataVertex v1, DataVertex v2, RelationshipEdge edge)
    {
        Graph<Vertex, RelationshipEdge> subgraph= baseGraph.getSubGraphByDiameter(v1,patternGraph.getDiameter());


        //Now, perform the change

        // Remove from the subgraph
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

        // Perform VF2
        //myConsole.print("========================AFTER CHANGE========================");
        //myConsole.print("-----REMOVE EDGE: "+((DataVertex) edge.getSource()).getVertexURI() + " --> (" +edge.getLabel()
        //        +") --> " + ((DataVertex) edge.getTarget()).getVertexURI()+" -----");
        Iterator<GraphMapping<Vertex, RelationshipEdge>> afterResults = VF2.execute(subgraph,patternGraph,false);

        return afterResults;
    }

    private Iterator<GraphMapping<Vertex, RelationshipEdge>> updateGraphByUpdatingAnAttribute(
            DataVertex v1, Attribute attribute)
    {
        Graph<Vertex, RelationshipEdge> subgraph= baseGraph.getSubGraphByDiameter(v1,patternGraph.getDiameter());

        //Now, perform the change...

        v1.setOrAddAttribute(attribute);

        // Perform VF2
        //myConsole.print("========================AFTER CHANGE========================");
        //myConsole.print("+++++ATTRIBUTE: "+ v1.getVertexURI() + " --> "+attribute.toString()+" +++++");

        Iterator<GraphMapping<Vertex, RelationshipEdge>> afterResults = VF2.execute(subgraph,patternGraph,false);

        return afterResults;
    }

    private Iterator<GraphMapping<Vertex, RelationshipEdge>> updateGraphByDeletingAnAttribute(
            DataVertex v1, Attribute attribute)
    {
        Graph<Vertex, RelationshipEdge> subgraph= baseGraph.getSubGraphByDiameter(v1,patternGraph.getDiameter());

        //Now, perform the change...

        v1.deleteAttribute(attribute);

        // Perform VF2

        //myConsole.print("========================AFTER CHANGE========================");
        //myConsole.print("+++++ATTRIBUTE: "+ v1.getVertexURI() + " --> "+attribute.toString()+" +++++");

        Iterator<GraphMapping<Vertex, RelationshipEdge>> afterResults = VF2.execute(subgraph,patternGraph,false);
        return afterResults;
    }

}

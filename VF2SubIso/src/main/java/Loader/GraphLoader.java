package Loader;

import ChangeExploration.*;
import ICs.TGFD;
import Infra.*;
import Util.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for graph loaders
 * DBPedia and IMDB loaders extend this class
 */

public class GraphLoader {

    //region --[Fields: Protected]---------------------------------------

    /** size of the graph: #edges + #attributes */
    protected int graphSize=0;

    // Graph instance
    protected VF2DataGraph graph;

    // This will be used to filter the nodes that are not from the types in our TGFD lists
    // For example, if the tgfd is about "soccerplayer" and "team", we will only load the node types of "soccerplayer" and "team"
    // The filtering will be done if "properties.myProperties.optimizedLoadingBasedOnTGFD" set to TRUE
    protected Set <String> validTypes;

    // Same as validTypes, this will also be used to filter the attributes that are not from the types in our TGFD lists
    // The filtering will be done if "properties.myProperties.optimizedLoadingBasedOnTGFD" set to TRUE
    protected Set<String> validAttributes;

    //endregion

    //region --[Constructors]--------------------------------------------

    public GraphLoader(List <TGFD> alltgfd)
    {
        graph=new VF2DataGraph();
        validTypes=new HashSet <>();
        validAttributes=new HashSet<>();

        if(Config.optimizedLoadingBasedOnTGFD)
            for (TGFD tgfd:alltgfd) {
                extractValidTypesFromTGFD(tgfd);
                extractValidAttributesFromTGFD(tgfd);
            }
    }

    public GraphLoader()
    {
        graph=new VF2DataGraph();
        validTypes=new HashSet <>();
        validAttributes=new HashSet<>();
    }


    //endregion

    //region --[Properties: Public]--------------------------------------

    public VF2DataGraph getGraph() {
        return graph;
    }

    public void setGraph(VF2DataGraph graph) {
        this.graph = graph;
    }

    /**
     * @return Size of the graph
     */
    public int getGraphSize() {
        return graphSize;
    }

    //endregion

    //region --[Private Methods]-----------------------------------------

    public void updateGraphWithChanges(List<Change> changes)
    {
        for (Change change:changes) {
            if(change instanceof VertexChange && change.getTypeOfChange()==ChangeType.insertVertex)
                this.graph.addVertex(((VertexChange) change).getVertex());
        }

        for (Change change:changes)
        {
            if(change instanceof EdgeChange)
            {
                EdgeChange edgeChange=(EdgeChange) change;
                DataVertex v1= (DataVertex) this.graph.getNode(edgeChange.getSrc());
                DataVertex v2= (DataVertex) this.graph.getNode(edgeChange.getDst());
                if(v1==null || v2==null)
                    continue;
                if(edgeChange.getTypeOfChange()== ChangeType.insertEdge)
                    this.graph.addEdge(v1, v2,new RelationshipEdge(edgeChange.getLabel()));
                else if(edgeChange.getTypeOfChange()== ChangeType.deleteEdge)
                    this.graph.removeEdge(v1,v2,new RelationshipEdge(edgeChange.getLabel()));
            }
            else if(change instanceof AttributeChange)
            {
                AttributeChange attributeChange=(AttributeChange) change;
                DataVertex v1=(DataVertex) this.graph.getNode(attributeChange.getUri());
                if(v1==null)
                    continue;
                if(attributeChange.getTypeOfChange()==ChangeType.changeAttr || attributeChange.getTypeOfChange()==ChangeType.insertAttr)
                    v1.setOrAddAttribute(attributeChange.getAttribute());
                else if(attributeChange.getTypeOfChange()==ChangeType.deleteAttr)
                    v1.deleteAttribute(attributeChange.getAttribute());
            }
        }
    }

    //endregion

    //region --[Private Methods]-----------------------------------------

    /**
     * Extracts all the types being used in a TGFD from from X->Y dependency and the graph pattern
     * @param tgfd input TGFD
     */
    private void extractValidTypesFromTGFD(TGFD tgfd)
    {
        for (Literal x:tgfd.getDependency().getX()) {
            if(x instanceof ConstantLiteral)
                validTypes.add(((ConstantLiteral) x).getVertexType());
            else if(x instanceof VariableLiteral)
            {
                validTypes.add(((VariableLiteral) x).getVertexType_1());
                validTypes.add(((VariableLiteral) x).getVertexType_2());
            }

        }
        for (Literal x:tgfd.getDependency().getY()) {
            if(x instanceof ConstantLiteral)
                validTypes.add(((ConstantLiteral) x).getVertexType());
            else if(x instanceof VariableLiteral)
            {
                validTypes.add(((VariableLiteral) x).getVertexType_1());
                validTypes.add(((VariableLiteral) x).getVertexType_2());
            }

        }
        for (Vertex v:tgfd.getPattern().getPattern().vertexSet()) {
            if(v instanceof PatternVertex)
                validTypes.addAll(v.getTypes());
        }
    }

    /**
     * Extracts all the attributes names being used in a TGFD from from X->Y dependency and the graph pattern
     * @param tgfd input TGFD
     */
    private void extractValidAttributesFromTGFD(TGFD tgfd)
    {
        for (Literal x:tgfd.getDependency().getX()) {
            if(x instanceof ConstantLiteral)
                validAttributes.add(((ConstantLiteral) x).getAttrName());
            else if(x instanceof VariableLiteral)
            {
                validAttributes.add(((VariableLiteral) x).getAttrName_1());
                validAttributes.add(((VariableLiteral) x).getAttrName_2());
            }

        }
        for (Literal x:tgfd.getDependency().getY()) {
            if(x instanceof ConstantLiteral)
                validAttributes.add(((ConstantLiteral) x).getAttrName());
            else if(x instanceof VariableLiteral)
            {
                validAttributes.add(((VariableLiteral) x).getAttrName_1());
                validAttributes.add(((VariableLiteral) x).getAttrName_2());
            }

        }
        for (Vertex v:tgfd.getPattern().getPattern().vertexSet()) {
            if(v instanceof PatternVertex)
                validTypes.addAll(v.getAllAttributesNames());
        }
    }

    //endregion

}

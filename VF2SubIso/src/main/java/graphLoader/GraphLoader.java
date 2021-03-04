package graphLoader;

import infra.*;
import util.properties;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphLoader {

    // Graph instance
    protected VF2DataGraph graph;

    // This will be used to filter the nodes that are not from the types in our TGFD lists
    // For example, if the tgfd is about "soccerplayer" and "team", we will only load the node types of "soccerplayer" and "team"
    // The filtering will be done if "properties.myProperties.optimizedLoadingBasedOnTGFD" set to TRUE
    protected Set <String> validTypes;

    // Same as validTypes, this will also be used to filter the attributes that are not from the types in our TGFD lists
    // The filtering will be done if "properties.myProperties.optimizedLoadingBasedOnTGFD" set to TRUE
    protected Set<String> validAttributes;

    public GraphLoader(List <TGFD> alltgfd)
    {
        graph=new VF2DataGraph();
        validTypes=new HashSet <>();
        validAttributes=new HashSet<>();

        if(properties.myProperties.optimizedLoadingBasedOnTGFD)
            for (TGFD tgfd:alltgfd) {
                extractValidTypesFromTGFD(tgfd);
                extractValidAttributesFromTGFD(tgfd);
            }
    }

    public VF2DataGraph getGraph() {
        return graph;
    }


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
        for (Vertex v:tgfd.getPattern().getGraph().vertexSet()) {
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
        for (Vertex v:tgfd.getPattern().getGraph().vertexSet()) {
            if(v instanceof PatternVertex)
                validTypes.addAll(v.getAllAttributesNames());
        }
    }

}

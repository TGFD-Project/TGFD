package Infra;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PatternVertex extends Vertex{

    public PatternVertex(String type) {
        super(type.toLowerCase());
    }
    private boolean isPatternNode=true;

    @Override
    public String toString() {
        return "pattern vertex{" +
                "type='" + getTypes() + '\'' +
                ", literals=" + getAllAttributesList() +
                '}';
    }

    // This is being used to check if a PatternVertex can be mapped to a DataVertex
    @Override
    public boolean isMapped(Vertex v)
    {
        if(v instanceof PatternVertex)
            return false;
        if (!v.getTypes().containsAll(super.getTypes()))
            return false;
        if(!v.getAllAttributesNames().containsAll(super.getAllAttributesNames()))
            return false;
        for (Attribute attr:super.getAllAttributesList())
            if(!attr.isNULL() && !v.getAttributeValueByName(attr.getAttrName()).equals(attr.getAttrValue()))
                return false;
        return true;
    }

    @Override
    public int compareTo(@NotNull Vertex o) {
        if(o instanceof PatternVertex)
        {
            PatternVertex v=(PatternVertex) o;
            //TODO: How can we say two PatternVertex are the same? Have the same type?

            if(this.getTypes().containsAll(v.getTypes()))
                return 1;
            else
                return 0;
            //Old code to just check if the first type is the same. Assuming we set only one type for each PatternVertex
            //return this.getTypes().toArray()[0].toString().compareTo(v.getTypes().toArray()[0].toString());
        }
        else
            return 0;
    }

    public PatternVertex copy(){
        PatternVertex newV = new PatternVertex(new ArrayList<>(this.getTypes()).get(0));
        for (Attribute attr : this.getAllAttributesList()) {
            newV.addAttribute(attr.getAttrName(), attr.getAttrValue());
        }
        return newV;
    }
}

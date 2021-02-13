package infra;

public class patternVertex extends Vertex {

    public patternVertex(String type) {
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

    @Override
    public boolean isEqual(Vertex v)
    {
        if (!v.getTypes().containsAll(super.getTypes()))
            return false;
        if(!v.getAllAttributesNames().containsAll(super.getAllAttributesNames()))
            return false;
        for (Attribute attr:super.getAllAttributesList())
            if(!attr.isNull() && !v.getAttributeByName(attr.getAttrName()).equals(attr.getAttrValue()))
                return false;
        return true;
    }
}

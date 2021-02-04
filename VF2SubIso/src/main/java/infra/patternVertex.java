package infra;

public class patternVertex extends vertex {

    public patternVertex(String type) {
        super(type);
    }
    private boolean isPatternNode=true;

    @Override
    public String toString() {
        return "pattern vertex{" +
                "type='" + getType() + '\'' +
                ", literals=" + getAllAttributesList() +
                '}';
    }

    @Override
    public boolean isEqual(vertex v)
    {
        if (!super.getType().equals(v.getType()))
            return false;
        if(!v.getAllAttributesNames().containsAll(super.getAllAttributesNames()))
            return false;
        for (attribute attr:super.getAllAttributesList())
            if(!attr.isNull() && !v.getAttributeByName(attr.getAttrName()).equals(attr.getAttrValue()))
                return false;
        return true;
    }
}

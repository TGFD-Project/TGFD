package infra;

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

    @Override
    public boolean isEqual(Vertex v)
    {
        if (!v.getTypes().containsAll(super.getTypes()))
            return false;
        if(!v.getAllAttributesNames().containsAll(super.getAllAttributesNames()))
            return false;
        for (Attribute attr:super.getAllAttributesList())
            if(!attr.isNull() && !v.getAttributeValueByName(attr.getAttrName()).equals(attr.getAttrValue()))
                return false;
        return true;
    }

    @Override
    public int compareTo(Vertex o) {
        if(o instanceof PatternVertex)
        {
            PatternVertex v=(PatternVertex) o;
            return this.getTypes().toArray()[0].toString().compareTo(v.getTypes().toArray()[0].toString());
        }
        else
            return 0;
    }
}

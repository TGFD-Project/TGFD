package infra;

public class dataVertex extends vertex {


    private String vertexURI="";
    private final int hashValue;

    public dataVertex(String type, String uri) {
        super(type);
        this.vertexURI=uri;
        this.hashValue=vertexURI.hashCode();
    }


    @Override
    public String toString() {
        return "vertex{" +
                "type='" + super.getType() + '\'' +
                ", attributes=" + super.getAllAttributesList() +
                '}';
    }

    public int getHashValue() {
        return hashValue;
    }

    public String getVertexURI() {
        return vertexURI;
    }

    @Override
    public boolean isEqual(vertex v) {
        if (!super.getType().equals(v.getType()))
            return false;
        if(!super.getAllAttributesNames().containsAll(v.getAllAttributesNames()))
            return false;
        for (attribute attr:v.getAllAttributesList())
            if(!attr.isNull() && !super.getAttributeByName(attr.getAttrName()).equals(attr.getAttrValue()))
                return false;
        return true;
    }
}

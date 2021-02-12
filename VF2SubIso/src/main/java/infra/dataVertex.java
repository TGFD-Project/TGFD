package infra;

public class dataVertex extends vertex {


    private String vertexURI="";
//    private final int hashValue;


    public dataVertex(String uri, String type) {
        super(type.toLowerCase());
        this.vertexURI=uri.toLowerCase();
        // ???: Is Integer large enough for our use case of possible 10+ million vertices? [2021-02-07]
//        this.hashValue=vertexURI.hashCode();
    }

    @Override
    public String toString() {
        return "vertex{" +
                "type='" + getTypes() + '\'' +
                ", attributes=" + super.getAllAttributesList() +
                '}';
    }

//    public int getHashValue() {
//        return hashValue;
//    }

    public String getVertexURI() {
        return vertexURI;
    }

    @Override
    public boolean isEqual(vertex v) {
        if (!super.getTypes().containsAll(v.getTypes()))
            return false;
        if(!super.getAllAttributesNames().containsAll(v.getAllAttributesNames()))
            return false;
        for (attribute attr:v.getAllAttributesList())
            if(!attr.isNull() && !super.getAttributeByName(attr.getAttrName()).equals(attr.getAttrValue()))
                return false;
        return true;
    }
}

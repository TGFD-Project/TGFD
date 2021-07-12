package changeExploration;

import Infra.Attribute;

public class AttributeChange extends Change {


    Attribute attribute;
    String uri;

    public AttributeChange(ChangeType cType, int id, String vertexURI, Attribute attr) {
        super(cType,id);
        this.attribute=attr;
        uri=vertexURI;
    }

    @Override
    public String toString() {
        return "attributeChange ("+getTypeOfChange()+"){" +
                "attribute=" + attribute +
                ", uri=" + uri +
                '}';
    }

    public Infra.Attribute getAttribute() {
        return attribute;
    }

    public String getUri() {
        return uri;
    }
}

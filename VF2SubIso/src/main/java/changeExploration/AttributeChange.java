package changeExploration;

import infra.Attribute;

public class AttributeChange extends Change {


    Attribute attribute;
    String uri;

    public AttributeChange(ChangeType cType, String vertexURI, Attribute attr) {
        super(cType);
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

    public infra.Attribute getAttribute() {
        return attribute;
    }

    public String getUri() {
        return uri;
    }
}

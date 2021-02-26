package changeExploration;

import infra.Attribute;
import infra.DataVertex;

public class attributeChange extends change {


    Attribute attribute;
    String uri;

    public attributeChange(changeType cType, DataVertex vertex, Attribute attr) {
        super(cType);
        attribute=attr;
        uri=vertex.getVertexURI();
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

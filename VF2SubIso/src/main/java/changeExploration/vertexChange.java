package changeExploration;

import infra.DataVertex;

public class vertexChange extends change{

    private String uri;
    public vertexChange(changeType cType, DataVertex v) {
        super(cType);
        this.uri=v.getVertexURI();
    }

    @Override
    public String toString() {
        return "vertexChange{" +
                "uri='" + uri + '\'' +
                '}';
    }

    public String getUri() {
        return uri;
    }
}

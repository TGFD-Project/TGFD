package changeExploration;

import infra.DataVertex;

public class VertexChange extends Change {

    private DataVertex vertex;
    public VertexChange(ChangeType cType, DataVertex v) {
        super(cType);
        this.vertex=v;
    }

    @Override
    public String toString() {
        return "vertexChange{" +
                "vertex='" + vertex + '\'' +
                '}';
    }

    public DataVertex getVertex() {
        return vertex;
    }
}

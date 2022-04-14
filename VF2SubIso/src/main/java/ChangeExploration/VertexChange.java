package ChangeExploration;

import Infra.DataVertex;

public class VertexChange extends Change {

    private DataVertex vertex;
    public VertexChange(ChangeType cType, int id, DataVertex v) {
        super(cType,id);
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

package ChangeExploration;

import Infra.DataVertex;
import Infra.RelationshipEdge;

public class EdgeChange extends Change {

    String src,dst;
    String label;

    public EdgeChange(ChangeType cType, int id, String srcURI, String dstURI, String label) {
        super(cType,id);
        src=srcURI;
        dst=dstURI;
        this.label=label;
    }

    public EdgeChange(ChangeType cType, int id, RelationshipEdge edge) {
        super(cType,id);
        src=((DataVertex)edge.getSource()).getVertexURI();
        dst=((DataVertex)edge.getTarget()).getVertexURI();
        this.label=edge.getLabel();
    }

    @Override
    public String toString() {
        return "edgeChange ("+getTypeOfChange()+"){" +
                "src=" + src +
                ", dst=" + dst +
                ", label='" + label + '\'' +
                '}';
    }

    public String getDst() {
        return dst;
    }

    public String getSrc() {
        return src;
    }

    public String getLabel() {
        return label;
    }


}

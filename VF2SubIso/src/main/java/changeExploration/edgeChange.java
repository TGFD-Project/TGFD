package changeExploration;

import infra.DataVertex;

public class edgeChange extends change {

    String src,dst;
    String label;

    public edgeChange(changeType cType, DataVertex source, DataVertex destination, String label) {
        super(cType);
        src=source.getVertexURI();
        dst=destination.getVertexURI();
        this.label=label;
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

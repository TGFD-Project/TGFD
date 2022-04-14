package Infra;

public class SimpleEdge
{
    private String src, dst, label;
    public SimpleEdge(String src, String dst, String label)
    {
        this.src=src;
        this.dst=dst;
        this.label=label;
    }

    public SimpleEdge(RelationshipEdge edge)
    {
        this.src=((DataVertex)edge.getSource()).getVertexURI();
        this.dst=((DataVertex)edge.getTarget()).getVertexURI();
        this.label=edge.getLabel();
    }

    public String getSrc() {
        return src;
    }

    public String getDst() {
        return dst;
    }

    public String getLabel() {
        return label;
    }
}
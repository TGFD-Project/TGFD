package Infra;

public class SimpleEdge
{
    private String src,dst;
    public SimpleEdge(String src, String dst)
    {
        this.src=src;
        this.dst=dst;
    }

    public SimpleEdge(RelationshipEdge edge)
    {
        this.src=((DataVertex)edge.getSource()).getVertexURI();
        this.dst=((DataVertex)edge.getTarget()).getVertexURI();
    }

    public String getSrc() {
        return src;
    }

    public String getDst() {
        return dst;
    }
}
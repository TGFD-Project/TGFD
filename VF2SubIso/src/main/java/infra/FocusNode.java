package infra;

public class FocusNode {

    private int diameter;
    private String node;
    private String TGFDName;

    public FocusNode(String node,String TGFDName, int diameter)
    {
        this.diameter=diameter;
        this.node=node;
        this.TGFDName=TGFDName;
    }

    public int getDiameter() {
        return diameter;
    }

    public String getNodeURI() {
        return node;
    }

    public String getTGFDName() {
        return TGFDName;
    }
}

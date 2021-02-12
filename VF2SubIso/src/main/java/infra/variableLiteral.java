package infra;

public class variableLiteral extends literal{

    String vertexType_1, vertexType_2, attrName_1, attrName_2;
    public variableLiteral(literalType t,String vertexType_1, String vertexType_2,String attrName_1,String attrName_2) {
        super(t);

        this.vertexType_1=vertexType_1;
        this.vertexType_2=vertexType_2;
        this.attrName_1=attrName_1;
        this.attrName_2=attrName_2;
    }

    public String getAttrName_1() {
        return attrName_1;
    }

    public String getAttrName_2() {
        return attrName_2;
    }

    public String getVertexType_1() {
        return vertexType_1;
    }

    public String getVertexType_2() {
        return vertexType_2;
    }
}

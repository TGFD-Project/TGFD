package infra;

public class ConstantLiteral extends Literal {

    String vertexType, attrName, attrValue;
    public ConstantLiteral(literalType t, String vertexType, String attrName, String attrValue ) {
        super(t);
        this.attrName=attrName;
        this.vertexType=vertexType;
        this.attrValue=attrValue;
    }

    public String getAttrName() {
        return attrName;
    }

    public String getAttrValue() {
        return attrValue;
    }

    public String getVertexType() {
        return vertexType;
    }
}

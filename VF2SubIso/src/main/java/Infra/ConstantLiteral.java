package Infra;

public class ConstantLiteral extends Literal {

    private String vertexType, attrName, attrValue;
    public ConstantLiteral(String vertexType, String attrName, String attrValue ) {
        super(LiteralType.Constant);
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

    @Override
    public String toString() {
        return "ConstantLiteral{" +
                "vertexType='" + vertexType + '\'' +
                ", attrName='" + attrName + '\'' +
                ", attrValue='" + attrValue + '\'' +
                '}';
    }
}

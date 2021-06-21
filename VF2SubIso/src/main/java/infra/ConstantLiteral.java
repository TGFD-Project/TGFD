package infra;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ConstantLiteral extends Literal {

    String vertexType, attrName, attrValue;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof ConstantLiteral)) return false;
        if (this.attrValue == null && ((ConstantLiteral)obj).attrValue == null) {
            return this.vertexType.equals(((ConstantLiteral)obj).vertexType) && this.attrName.equals(((ConstantLiteral)obj).attrName);
        } else if (this.attrValue == null ^ ((ConstantLiteral)obj).attrValue == null) {
            return false;
        } else {// if (this.attrValue != null && ((ConstantLiteral)obj).attrValue != null) {
            assert this.attrValue != null;
            return this.vertexType.equals(((ConstantLiteral)obj).vertexType) &&
                this.attrName.equals(((ConstantLiteral)obj).attrName) &&
                this.attrValue.equals(((ConstantLiteral)obj).attrValue);
        }
//        return this.vertexType.equals(((ConstantLiteral)obj).vertexType) &&
//                this.attrName.equals(((ConstantLiteral)obj).attrName) &&
//                ((this.attrValue != null && ((ConstantLiteral)obj).attrValue != null) ? this.attrValue.equals(((ConstantLiteral)obj).attrValue) : true);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertexType, attrName, attrValue);
    }
}

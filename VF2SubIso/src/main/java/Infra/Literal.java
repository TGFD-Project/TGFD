package Infra;

public abstract class Literal {

    public enum LiteralType
    {
        Constant,
        Variable
    }

    private LiteralType type;

    public Literal(LiteralType t)
    {
        this.type=t;
    }

    public LiteralType getLiteralType() {
        return type;
    }
}

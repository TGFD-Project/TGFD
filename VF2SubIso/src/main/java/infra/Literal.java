package infra;

public class Literal {

    public enum literalType
    {
        constant, variable
    }

    private literalType type;

    public Literal(literalType t)
    {
        this.type=t;
    }

    public literalType getLiteralType() {
        return type;
    }
}

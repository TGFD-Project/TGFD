package infra;

public class literal {

    public enum literalType
    {
        constant, variable
    }

    private literalType type;

    public literal(literalType t)
    {
        this.type=t;
    }

    public literalType getLiteralType() {
        return type;
    }
}

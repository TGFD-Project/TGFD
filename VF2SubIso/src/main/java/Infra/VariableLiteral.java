package Infra;

/**
 * A variable literal to assert that a value of an attribute of a pair of vertices are the same.
 *
 * Example:
 *   `player.city = team.city` is represented by:
 *     new VariableLiteral(
 *       "player", // vertexType_1
 *       "team",   // vertexType_2
 *       "city",   // attrName_1
 *       "city")   // attrName_2
 */
public class VariableLiteral extends Literal
{
    //region --[Fields: Private]---------------------------------------
    /** Vertex type of 1 */
    private String vertexType_1;
    /** Vertex type of 2 */
    private String vertexType_2;
    /** Attribute name of 1 */
    private String attrName_1;
    /** Attribute name of 2 */
    private String attrName_2;
    //endregion

    //region --[Constructors]------------------------------------------
    /**
     * Creates a VariableLiteral.
     */
    public VariableLiteral(String vertexType_1, String attrName_1, String vertexType_2, String attrName_2)
    {
        super(LiteralType.Variable);

        this.vertexType_1=vertexType_1;
        this.vertexType_2=vertexType_2;
        this.attrName_1=attrName_1;
        this.attrName_2=attrName_2;
    }
    //endregion

    //region --[Properties: Public]------------------------------------
    /** Gets the attribute name of 1. */
    public String getAttrName_1() { return attrName_1; }

    /** Gets the attribute name of 2. */
    public String getAttrName_2() { return attrName_2; }

    /** Gets the vertex type of 1. */
    public String getVertexType_1() { return vertexType_1; }

    /** Gets the vertex type of 2. */
    public String getVertexType_2() { return vertexType_2; }
    //endregion


    @Override
    public String toString() {
        return "VariableLiteral{" +
                "vertexType_1='" + vertexType_1 + '\'' +
                ", vertexType_2='" + vertexType_2 + '\'' +
                ", attrName_1='" + attrName_1 + '\'' +
                ", attrName_2='" + attrName_2 + '\'' +
                '}';
    }
}

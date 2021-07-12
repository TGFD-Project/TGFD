package Infra;

import java.util.ArrayList;
import java.util.HashSet;

public class LiteralTreeNode {
    private LiteralTreeNode parent = null;
    private final ConstantLiteral literal;
    ArrayList<LiteralTreeNode> children;
    private boolean isPruned = false;

    public LiteralTreeNode(ConstantLiteral literal, LiteralTreeNode parent) {
        this.literal = literal;
        this.parent = parent;
    }

    // TO-DO: Recursion takes up too much memory. However, if we can find an efficient way to do it, it will save a lot of subset checks in Delta Discovery
    public LiteralTreeNode(ConstantLiteral literal, HashSet<ConstantLiteral> otherLiterals) {
        this.literal = literal;
        HashSet<ConstantLiteral> subset = new HashSet<>(otherLiterals);
        subset.remove(literal);
        for (ConstantLiteral otherLiteral : subset) {
            this.children.add(new LiteralTreeNode(otherLiteral, subset));
        }
    }

    public ArrayList<ConstantLiteral> getPathToRoot() {
        ArrayList<ConstantLiteral> literalPath = new ArrayList<>();
        literalPath.add(literal);
        LiteralTreeNode parentLiteralNode = parent;
        while (parentLiteralNode != null) {
            literalPath.add(parentLiteralNode.getLiteral());
            parentLiteralNode = parentLiteralNode.getParent();
        }
        return literalPath;
    }

    public ConstantLiteral getLiteral() {
        return this.literal;
    }

    public LiteralTreeNode getParent() {
        return this.parent;
    }

    public void setIsPruned() {
        this.isPruned = true;
    }

    public boolean isPruned() {
        return this.isPruned;
    }
}
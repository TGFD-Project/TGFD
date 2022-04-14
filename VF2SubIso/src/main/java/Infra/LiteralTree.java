package Infra;

import java.util.ArrayList;

public class LiteralTree {
    private ArrayList<ArrayList<LiteralTreeNode>> tree;

    public LiteralTree() {
        tree = new ArrayList<>();
    }

    public void addLevel() {
        tree.add(new ArrayList<>());
        System.out.println("TgfdDiscovery.LiteralTree levels: " + tree.size());
    }

    public LiteralTreeNode createNodeAtLevel(int level, ConstantLiteral literal, LiteralTreeNode parentNode) {
        LiteralTreeNode node = new LiteralTreeNode(literal, parentNode);
        tree.get(level).add(node);
        return node;
    }

    public ArrayList<LiteralTreeNode> getLevel(int i) {
        return tree.get(i);
    }
}
package main.java.Infra;

import java.util.ArrayList;

public class PatternTree {
    private ArrayList<ArrayList<PatternTreeNode>> tree;

    public PatternTree() {
        tree = new ArrayList<>();
    }

    public void addLevel() {
        tree.add(new ArrayList<>());
        System.out.println("GenerationTree levels: " + tree.size());
    }

    public PatternTreeNode createNodeAtLevel(int level, VF2PatternGraph pattern, double patternSupport, PatternTreeNode parentNode) {
        PatternTreeNode node = new PatternTreeNode(pattern, patternSupport, parentNode);
        tree.get(level).add(node);
        return node;
    }

    public ArrayList<PatternTreeNode> getLevel(int i) {
        return tree.get(i);
    }

    public int getNumOfTreeLevels() {
        return this.tree.size();
    }

}
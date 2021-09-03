package Infra;

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

    public PatternTreeNode createNodeAtLevel(int level, VF2PatternGraph pattern, PatternTreeNode parentNode, String candidateEdgeString) {
        PatternTreeNode node = new PatternTreeNode(pattern, parentNode, candidateEdgeString);
        tree.get(level).add(node);
        findSubgraphParents(level-1, node); // TO-DO
        findCenterVertexParent(level-1, node);
        return node;
    }

    public PatternTreeNode createNodeAtLevel(int level, VF2PatternGraph pattern, double patternSupport) {
        PatternTreeNode node = new PatternTreeNode(pattern, patternSupport);
        tree.get(level).add(node);
        return node;
    }

    public void findSubgraphParents(int level, PatternTreeNode node) {
        if (level < 0) return;
        System.out.println("Finding subgraph parents...");
        if (level == 0) {
            System.out.println("Finding subgraph parents...");
            ArrayList<String> newPatternVertices = new ArrayList<>();
            node.getGraph().vertexSet().forEach((vertex) -> {newPatternVertices.add(vertex.getTypes().iterator().next());});
            for (PatternTreeNode otherPatternNode : this.tree.get(level)) {
                if (newPatternVertices.containsAll(otherPatternNode.getGraph().vertexSet().iterator().next().getTypes())) {
                    System.out.println("New pattern: " + node.getPattern());
                    System.out.println("is a child of subgraph  parent pattern: " + otherPatternNode.getGraph().vertexSet());
                    node.addSubgraphParent(otherPatternNode);
                }
            }
            return;
        }
        ArrayList<String> newPatternEdges = new ArrayList<>();
        node.getGraph().edgeSet().forEach((edge) -> {newPatternEdges.add(edge.toString());});
        for (PatternTreeNode otherPatternNode : this.tree.get(level)) {
            ArrayList<String> otherPatternEdges = new ArrayList<>();
            otherPatternNode.getGraph().edgeSet().forEach((edge) -> {otherPatternEdges.add(edge.toString());});
            if (newPatternEdges.containsAll(otherPatternEdges)) { // TO-DO: Should we also check for center vertex equality here?
                System.out.println("New pattern: " + node.getPattern());
                if (otherPatternNode.getGraph().edgeSet().size() == 0) {
                    System.out.println("is a child of subgraph parent pattern: " + otherPatternNode.getGraph().vertexSet());
                } else {
                    System.out.println("is a child of subgraph parent pattern: " + otherPatternNode.getPattern());
                }
                node.addSubgraphParent(otherPatternNode);
            }
        }
    }

    public void findCenterVertexParent(int level, PatternTreeNode node) {
        if (level < 0) return;
        System.out.println("Finding center vertex parent...");
        ArrayList<String> newPatternEdges = new ArrayList<>();
        node.getGraph().edgeSet().forEach((edge) -> {newPatternEdges.add(edge.toString());});
        for (PatternTreeNode otherPatternNode : this.tree.get(level)) {
            ArrayList<String> otherPatternEdges = new ArrayList<>();
            otherPatternNode.getGraph().edgeSet().forEach((edge) -> {otherPatternEdges.add(edge.toString());});
            if (newPatternEdges.containsAll(otherPatternEdges)) {
                System.out.println("New pattern: " + node.getPattern());
                if (otherPatternNode.getGraph().edgeSet().size() == 0) {
                    System.out.println("is a child of center vertex parent pattern: " + otherPatternNode.getGraph().vertexSet());
                } else {
                    System.out.println("is a child of center vertex parent pattern: " + otherPatternNode.getPattern());
                }
                if (otherPatternNode.getPattern().getCenterVertexType().equals(node.getPattern().getCenterVertexType())) {
                    node.setCenterVertexParent(otherPatternNode);
                    return;
                }
            }
        }
    }

    public ArrayList<PatternTreeNode> getLevel(int i) {
        return this.tree.get(i);
    }

    public int getNumOfTreeLevels() {
        return this.tree.size();
    }

}

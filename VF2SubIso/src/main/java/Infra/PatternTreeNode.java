package main.java.Infra;

import org.jgrapht.Graph;

import java.util.ArrayList;

public class PatternTreeNode {
    private final VF2PatternGraph pattern;
    private final double patternSupport;
    private final PatternTreeNode parentNode;
    private boolean isPruned = false;
    private ArrayList<ArrayList<ConstantLiteral>> zeroEntityDependencies = new ArrayList<>();
    private ArrayList<ArrayList<ConstantLiteral>> minimalDependencies = new ArrayList<>();

    public PatternTreeNode(VF2PatternGraph pattern, double patternSupport, PatternTreeNode parentNode) {
        this.pattern = pattern;
        this.patternSupport = patternSupport;
        this.parentNode = parentNode;
    }

    public Graph<Vertex, RelationshipEdge> getGraph() {
        return pattern.getPattern();
    }

    public VF2PatternGraph getPattern() {
        return pattern;
    }

    public double getPatternSupport() {
        return patternSupport;
    }

    public PatternTreeNode parentNode() {
        return this.parentNode;
    }

    public void setIsPruned() {
        this.isPruned = true;
    }

    public boolean isPruned() {
        return this.isPruned;
    }

    @Override
    public String toString() {
        return "GenTreeNode{" +
                "pattern=" + pattern +
                ",\n support=" + patternSupport +
                '}';
    }

    public void addZeroEntityDependency(ArrayList<ConstantLiteral> dependency) {
        this.zeroEntityDependencies.add(dependency);
    }

    public ArrayList<ArrayList<ConstantLiteral>> getZeroEntityDependencies() {
        return this.zeroEntityDependencies;
    }

    public ArrayList<ArrayList<ConstantLiteral>> getAllZeroEntityDependenciesOnThisPath() {
        PatternTreeNode currPatternTreeNode = this;
        ArrayList<ArrayList<ConstantLiteral>> zeroEntityPaths = new ArrayList<>(currPatternTreeNode.getZeroEntityDependencies());
        while (currPatternTreeNode.parentNode() != null) {
            currPatternTreeNode = currPatternTreeNode.parentNode();
            zeroEntityPaths.addAll(currPatternTreeNode.getZeroEntityDependencies());
        }
        return zeroEntityPaths;
    }

    public void addMinimalDependency(ArrayList<ConstantLiteral> dependency) {
        this.minimalDependencies.add(dependency);
    }

    public ArrayList<ArrayList<ConstantLiteral>> getMinimalDependencies() {
        return this.minimalDependencies;
    }

    public ArrayList<ArrayList<ConstantLiteral>> getAllMinimalDependenciesOnThisPath() {
        PatternTreeNode currPatternTreeNode = this;
        ArrayList<ArrayList<ConstantLiteral>> minimalPaths = new ArrayList<>(currPatternTreeNode.getMinimalDependencies());
        while (currPatternTreeNode.parentNode() != null) {
            currPatternTreeNode = currPatternTreeNode.parentNode();
            minimalPaths.addAll(currPatternTreeNode.getMinimalDependencies());
        }
        return minimalPaths;
    }

}
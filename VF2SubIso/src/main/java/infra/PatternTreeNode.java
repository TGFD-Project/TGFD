package infra;

import TgfdDiscovery.TgfdDiscovery;
import org.jgrapht.Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PatternTreeNode {
    private final VF2PatternGraph pattern;
    private final double patternSupport;
    private final PatternTreeNode parentNode;
    private final String edgeString;
    private boolean isPruned = false;
    private ArrayList<ArrayList<ConstantLiteral>> zeroEntityDependencies = new ArrayList<>();
    private ArrayList<ArrayList<ConstantLiteral>> minimalDependencies = new ArrayList<>();
    private HashMap<ArrayList<ConstantLiteral>, ArrayList<TgfdDiscovery.Pair>> lowSupportGeneralTgfdList = new HashMap<>();

    public PatternTreeNode(VF2PatternGraph pattern, double patternSupport, PatternTreeNode parentNode, String edgeString) {
        this.pattern = pattern;
        this.patternSupport = patternSupport;
        this.parentNode = parentNode;
        this.edgeString = edgeString;
    }

    public PatternTreeNode(VF2PatternGraph pattern, double patternSupport, PatternTreeNode parentNode) {
        this.pattern = pattern;
        this.patternSupport = patternSupport;
        this.parentNode = parentNode;
        this.edgeString = null;
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

    public void addToLowSupportGeneralTgfdList(ArrayList<ConstantLiteral> dependencyPath, TgfdDiscovery.Pair deltaPair) {
        this.lowSupportGeneralTgfdList.putIfAbsent(dependencyPath, new ArrayList<>());
        this.lowSupportGeneralTgfdList.get(dependencyPath).add(deltaPair);
    }

    public HashMap<ArrayList<ConstantLiteral>, ArrayList<TgfdDiscovery.Pair>> getLowSupportGeneralTgfdList() {
        return this.lowSupportGeneralTgfdList;
    }

    public PatternTreeNode getParentNode() {
        return this.parentNode;
    }

    public HashMap<ArrayList<ConstantLiteral>, ArrayList<TgfdDiscovery.Pair>> getAllLowSupportGeneralTgfds() {
        HashMap<ArrayList<ConstantLiteral>, ArrayList<TgfdDiscovery.Pair>> allTGFDs = new HashMap<>(this.getLowSupportGeneralTgfdList());
        PatternTreeNode currentNode = this;
        while (currentNode.getParentNode() != null) {
            currentNode = currentNode.getParentNode();
            for (Map.Entry<ArrayList<ConstantLiteral>, ArrayList<TgfdDiscovery.Pair>> tgfdEntry : currentNode.getLowSupportGeneralTgfdList().entrySet()) {
                allTGFDs.putIfAbsent(tgfdEntry.getKey(), new ArrayList<>());
                allTGFDs.get(tgfdEntry.getKey()).addAll(tgfdEntry.getValue());
            }
        }
        return this.lowSupportGeneralTgfdList;
    }

    public String getEdgeString() {
        return this.edgeString;
    }

    public ArrayList<String> getAllEdgeStrings() {
        ArrayList<String> edgeStrings = new ArrayList<>();
        edgeStrings.add(this.edgeString);
        PatternTreeNode currentNode = this;
        while (currentNode.getParentNode().getEdgeString() != null) {
            currentNode = currentNode.getParentNode();
            edgeStrings.add(currentNode.getEdgeString());
        }
        return edgeStrings;
    }
}

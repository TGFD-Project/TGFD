package QPath;

import Infra.PatternVertex;
import Infra.Vertex;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class Triple implements Comparable<Triple>, Serializable {
    private final PatternVertex src;
    private final PatternVertex dst;
    private final String edge;

    public Triple(PatternVertex src, PatternVertex dst, String edge)
    {
        this.src=src;
        this.dst=dst;
        this.edge=edge;
    }

    public Triple(Vertex src, Vertex dst, String edge)
    {
        this.src=(PatternVertex) src;
        this.dst=(PatternVertex) dst;
        this.edge=edge;
    }

    public PatternVertex getDst() {
        return dst;
    }

    public PatternVertex getSrc() {
        return src;
    }

    public String getEdge() {
        return edge;
    }

    public boolean isTopologicalMapped(@NotNull Triple dataInstance)
    {
        if (!dataInstance.src.getTypes().containsAll(this.src.getTypes()))
            return false;
        if (!dataInstance.dst.getTypes().containsAll(this.dst.getTypes()))
            return false;
        if (!dataInstance.edge.equals(this.edge))
            return false;
        return true;
    }

    public boolean isMapped(@NotNull Triple dataInstance)
    {
        if (!dataInstance.src.isMapped(this.src))
            return false;
        if (!dataInstance.dst.isMapped(this.dst))
            return false;
        if (!dataInstance.edge.equals(this.edge))
            return false;
        return true;
    }

    public boolean isTopologicalMappedToSRC(@NotNull Vertex v)
    {
        if (!v.getTypes().containsAll(this.src.getTypes()))
            return false;
        return true;
    }

    public boolean isTopologicalMappedToDST(@NotNull Vertex v)
    {
        if (!v.getTypes().containsAll(this.dst.getTypes()))
            return false;
        return true;
    }

    public boolean isMappedToSRC(@NotNull Vertex v)
    {
        if (!v.isMapped(this.src))
            return false;
        return true;
    }

    public boolean isMappedToDST(@NotNull Vertex v)
    {
        if (!v.isMapped(this.dst))
            return false;
        return true;
    }

    @Override
    public int compareTo(@NotNull Triple o) {
        return 0;
    }
}

package QPath;

import Infra.Attribute;
import Infra.PatternVertex;
import Infra.Vertex;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;

public class Triple implements Comparable<Triple>, Serializable {
    private final Vertex src;
    private final Vertex dst;
    private final String edge;

    public Triple(Vertex src, Vertex dst, String edge)
    {
        this.src= src;
        this.dst= dst;
        this.edge=edge;
    }

    public Vertex getDst() {
        return dst;
    }

    public Vertex getSrc() {
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

    public ArrayList<Attribute> getUnSatSRC(@NotNull Vertex vertex)
    {
        ArrayList<Attribute> unSat=new ArrayList<>();
        for (Attribute attr:src.getAllAttributesList())
            if(!attr.isNULL() && !vertex.getAttributeValueByName(attr.getAttrName()).equals(attr.getAttrValue()))
                unSat.add(attr);
        return unSat;
    }

    public ArrayList<Attribute> getUnSatDST(@NotNull Vertex vertex)
    {
        ArrayList<Attribute> unSat=new ArrayList<>();
        for (Attribute attr:dst.getAllAttributesList())
            if(!attr.isNULL() && !vertex.getAttributeValueByName(attr.getAttrName()).equals(attr.getAttrValue()))
                unSat.add(attr);
        return unSat;
    }

    @Override
    public int compareTo(@NotNull Triple o) {
        return 0;
    }
}

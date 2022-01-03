package QPathBasedWorkload;

import Infra.Attribute;
import Infra.Vertex;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashSet;

public class Triple implements Comparable<Triple>, Serializable {
    private final Vertex src;
    private final Vertex dst;
    private HashSet<Attribute> unSatForSRC;
    private HashSet<Attribute> unSatForDST;
    private final String edge;
    private int tripleID;
    private int precTripleID;

    public Triple(Vertex src, Vertex dst, String edge)
    {
        this.src= src;
        this.dst= dst;
        this.edge=edge;
        this.unSatForSRC=new HashSet<>();
        this.unSatForDST=new HashSet<>();
    }

    public Triple(Vertex src, Vertex dst, String edge,int precTripleID, int tripleID)
    {
        this.src= src;
        this.dst= dst;
        this.edge=edge;
        this.unSatForSRC=new HashSet<>();
        this.unSatForDST=new HashSet<>();
        this.precTripleID=precTripleID;
        this.tripleID=tripleID;
    }

    public int getPrecTripleID() {
        return precTripleID;
    }

    public int getTripleID() {
        return tripleID;
    }

    public Vertex getDst() {
        return dst;
    }

    public Vertex getSrc() {
        return src;
    }

    public boolean isUnSatEmpty()
    {
        return (unSatForDST.isEmpty() && unSatForSRC.isEmpty());
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

    public HashSet<Attribute> getUnSatSRC(@NotNull Vertex vertex)
    {
        unSatForSRC.clear();
        for (Attribute attr:src.getAllAttributesList())
            if(!attr.isNULL() && !vertex.getAttributeValueByName(attr.getAttrName()).equals(attr.getAttrValue()))
                unSatForSRC.add(attr);
        return unSatForSRC;
    }

    public HashSet<Attribute> getUnSatDST(@NotNull Vertex vertex)
    {
        unSatForDST.clear();
        for (Attribute attr:dst.getAllAttributesList())
            if(!attr.isNULL() && !vertex.getAttributeValueByName(attr.getAttrName()).equals(attr.getAttrValue()))
                unSatForDST.add(attr);
        return unSatForDST;
    }

    public boolean hasAttrSRC(@NotNull Attribute attribute)
    {
        for (Attribute attr:src.getAllAttributesList())
            if(!attr.isNULL() && attr.getAttrName().equals(attribute.getAttrName()))
                return true;
        return false;
    }

    public boolean hasAttrDST(@NotNull Attribute attribute)
    {
        for (Attribute attr:dst.getAllAttributesList())
            if(!attr.isNULL() && attr.getAttrName().equals(attribute.getAttrName()))
                return true;
        return false;
    }

    @Override
    public int compareTo(@NotNull Triple o) {
        return 0;
    }
}

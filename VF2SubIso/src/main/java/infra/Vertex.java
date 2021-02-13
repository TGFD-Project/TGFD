package infra;

import java.util.*;

public abstract class Vertex {

    private Set<String> types;

    private Map<String, Attribute> attributes;

    // TODO: consider adding an id field (e.g. vertexURI from dataVertex) [2021-02-07]

    public Vertex(String type) {
        this.types=new HashSet<>();
        types.add(type);
        attributes= new HashMap<>();
    }


    public Map<String, Attribute> getAllAttributesHashMap() {
        return attributes;
    }

    public Collection<Attribute> getAllAttributesList() {
        return attributes.values();
    }

    public Collection<String> getAllAttributesNames() {
        return attributes.keySet();
    }

    public void setAllAttributes(List<Attribute> attributes) {
        for (Attribute attr:attributes)
            this.attributes.put(attr.getAttrName(),attr);
    }

    public Set<String> getTypes() {
        return types;
    }

    public void addTypes(String type)
    {
        this.types.add(type);
    }

    public void addAttribute(String name, String value)
    {
        attributes.put(name.toLowerCase(),new Attribute(name.toLowerCase(),value.toLowerCase()));
    }

    public void addAttribute(Attribute attr)
    {
        attributes.put(attr.getAttrName(),attr);
    }

    public boolean attContains(String name)
    {
        return attributes.containsKey(name.toLowerCase());
    }

    public String getAttributeByName(String name)
    {
        return attributes.get(name.toLowerCase()).getAttrValue();
    }

    public boolean isEqual(Vertex v)
    {
        return true;
    }

    // TODO: implement hashCode because Match uses vertex's hashcode as the signature [2021-02-07]
    //@Override
    //public int hashCode() {
    //    return Objects.hash(intervals, vertices);
    //}
}

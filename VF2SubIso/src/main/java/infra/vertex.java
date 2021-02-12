package infra;

import java.util.*;

public abstract class vertex {

    private Set<String> types;

    private Map<String, attribute> attributes;

    // TODO: consider adding an id field (e.g. vertexURI from dataVertex) [2021-02-07]

    public vertex(String type) {
        this.types=new HashSet<>();
        types.add(type);
        attributes= new HashMap<>();
    }


    public Map<String, attribute> getAllAttributesHashMap() {
        return attributes;
    }

    public Collection<attribute> getAllAttributesList() {
        return attributes.values();
    }

    public Collection<String> getAllAttributesNames() {
        return attributes.keySet();
    }

    public void setAllAttributes(List<attribute> attributes) {
        for (attribute attr:attributes)
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
        attributes.put(name.toLowerCase(),new attribute(name.toLowerCase(),value.toLowerCase()));
    }

    public void addAttribute(attribute attr)
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

    public boolean isEqual(vertex v)
    {
        return true;
    }

    // TODO: implement hashCode because Match uses vertex's hashcode as the signature [2021-02-07]
    //@Override
    //public int hashCode() {
    //    return Objects.hash(intervals, vertices);
    //}
}

package infra;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class vertex {

    private String type = "";

    private Map<String, attribute> attributes;

    public vertex(String type) {
        this.type = type;
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

    public String getType() {
        return type;
    }

    public void setAllAttributes(List<attribute> attributes) {
        for (attribute attr:attributes)
            this.attributes.put(attr.getAttrName(),attr);
    }

    public void setType(String type) {
        this.type = type.toLowerCase();
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
}

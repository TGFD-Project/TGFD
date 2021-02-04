package infra;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class vertex {

    //private String name;
    //private NodeType nodeType;
    private String type = "";
    private String vertexURI="";
    private int hashValue;
    private boolean isPatternNode=false;

    private Map<String, attribute> attributes;

    public vertex(String uri, String type, List<attribute> attributes) {
        this.vertexURI=uri;
        this.hashValue=vertexURI.hashCode();
        this.type = type.toLowerCase();
        for (attribute attr:attributes)
            this.attributes.put(attr.getAttrName(),attr);
    }

    public vertex(String uri, String type) {
        this.vertexURI=uri;
        this.hashValue=vertexURI.hashCode();
        this.type = type;
        attributes=new HashMap<String, attribute>();
    }

    public vertex(String type, boolean isPatternNode) {
        this.type = type;
        this.isPatternNode=true;
        attributes=new HashMap<String, attribute>();
    }

    @Override
    public String toString() {
        if(!isPatternNode)
            return "vertex{" +
                    "type='" + type + '\'' +
                    ", attributes=" + attributes.values() +
                    '}';
        else
            return "pattern vertex{" +
                    "type='" + type + '\'' +
                    ", literals=" + attributes.values() +
                    '}';
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
        if (!this.type.equals(v.getType()))
            return false;
        if(this.isPatternNode)
        {
            if(!v.getAllAttributesNames().containsAll(this.attributes.keySet()))
                return false;
            for (attribute attr:attributes.values())
                if(!attr.isNull() && v.getAttributeByName(attr.getAttrName())!=attr.getAttrValue())
                    return false;
        }
        else
        {
            if(!this.attributes.keySet().containsAll(v.getAllAttributesNames()))
                return false;
            for (attribute attr:v.getAllAttributesList())
                if(!attr.isNull() && getAttributeByName(attr.getAttrName())!=attr.getAttrValue())
                    return false;
        }
        return true;
    }
}

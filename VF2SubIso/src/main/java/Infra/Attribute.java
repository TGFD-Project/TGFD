package Infra;

import java.io.Serializable;

public class Attribute implements Comparable<Attribute>, Serializable {

    protected String attrName;
    protected String attrValue;

    //If this class is being used for DataVertex, then this is false
    //If this class is being used as a constant attribute for PatternVertex, then this is false
    //If this class is being used as variable attribute for PatternVertex, then set to true.
    //This is being used in both DataVertex and PatternVertex.
    //If it is isNULL = true, then in the mapping, we only check if the DataVertex has this attribute,
    //otherwise, the DataVertex has to have it and also has to have the same value
    protected boolean isNULL;

    public Attribute(String attrName, String attrValue)
    {
        this.attrName=attrName.toLowerCase();
        this.attrValue=attrValue.toLowerCase();
        isNULL =false;
    }

    public Attribute(String attrName)
    {
        this.attrName=attrName.toLowerCase();
        this.attrValue=null;
        isNULL =true;
    }

    @Override
    public String toString() {
        if(!isNULL)
            return "(" +
                    "'" + attrName + '\'' +
                    ", '" + attrValue + '\'' +
                    ')';
        else
            return "(" +
                    "'" + attrName + '\'' +
                    ", -" +
                    ')';
    }

    public String getAttrName() {
        return attrName;
    }

    public String getAttrValue() {
        return attrValue;
    }

    public boolean isNULL() {
        return isNULL;
    }

    public void setAttrName(String attrName) { this.attrName = attrName.toLowerCase();}


    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue.toLowerCase();
    }

    @Override
    public int compareTo(Attribute o) {
        return this.attrName.compareTo(o.attrName);
    }
}

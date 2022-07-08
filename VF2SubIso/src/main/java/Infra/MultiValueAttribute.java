package Infra;

import java.util.HashSet;

public class MultiValueAttribute extends Attribute{
    HashSet<String> multiValues = new HashSet<>();

    public MultiValueAttribute(String attrName, String attrValue) {
        super(attrName, attrValue);
        this.multiValues.add(attrValue);
    }

    public MultiValueAttribute(String attrName) {
        super(attrName);
    }
    public MultiValueAttribute(String attrName, HashSet<String> multiValues) {
        super(attrName);
        this.multiValues=multiValues;
        super.isNULL = false;
    }

    public void addValue(String value)
    {
        multiValues.add(value);
    }

    public boolean exists(String value)
    {
        return multiValues.contains(value);
    }

    @Override
    public String toString() {
        if(!isNULL)
            return "(" +
                    "'" + super.attrName + '\'' +
                    ", '" + this.multiValues.toString() + '\'' +
                    ')';
        else
            return "(" +
                    "'" + super.attrName + '\'' +
                    ", -" +
                    ')';
    }

}

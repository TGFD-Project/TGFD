package infra;

import org.jgrapht.graph.DefaultEdge;

public class relationshipEdge extends DefaultEdge {

    private String label;


    @Override
    public String toString() {
        return "(" + getSource() + " : " + getTarget() + " : " + label + ")";
    }

//    public int hashCode() {
//
//        int result = 17;
//        result = 31 * result + label.hashCode();
//        result = 31 * result + getSource().hashCode();
//        result = 31 * result + getTarget().hashCode();
//
//        return result;
//    }

    public boolean equals(Object obj) {

        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof relationshipEdge))
            return false;

        relationshipEdge edge = (relationshipEdge) obj;
        return label.equals(edge.label) && getSource().equals(edge.getSource()) && getTarget().equals(edge.getTarget());

    }

    public String getLabel() {
        return label;
    }

    public relationshipEdge(String label) {
        this.label = label;
    }

}

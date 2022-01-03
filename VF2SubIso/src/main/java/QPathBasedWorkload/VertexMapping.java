package QPathBasedWorkload;

import Infra.PatternVertex;
import Infra.Vertex;

import java.util.HashMap;

public class VertexMapping {

    private HashMap<String, Vertex> mapping;

    public VertexMapping()
    {
        this.mapping=new HashMap<>();
    }

    public Vertex getVertexCorrespondence(Vertex patternVertex)
    {
        if(!((PatternVertex)patternVertex).getPatternVertexRandomID().equals(""))
        {
            return mapping.getOrDefault(((PatternVertex)patternVertex).getPatternVertexRandomID(),null);
        }
        return null;
    }

    public void addMapping(Vertex vertex, PatternVertex patternVertex)
    {
        mapping.put(patternVertex.getPatternVertexRandomID(),vertex);
    }


}

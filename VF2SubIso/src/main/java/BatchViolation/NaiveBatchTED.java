package BatchViolation;

import infra.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NaiveBatchTED {

    private MatchCollection matches;
    private TGFD tgfd;

    private HashMap<Integer, HashMap<String,Match>> detailedMatches;

    public NaiveBatchTED(MatchCollection allMatches, TGFD tgfd)
    {
        this.tgfd=tgfd;
        this.matches=allMatches;
        detailedMatches=new HashMap<>();
    }

    public void findViolations()
    {
        Interval delta=tgfd.getDelta();
        for (int i=delta.getStart();i<=delta.getEnd();i+=delta.getGranularity())
        {
            List<Match> currentMatches= matches.get(i);
        }
    }

    private void analyzeMatches(List<Match> currentMaches, int timePoint)
    {

    }

    private String matchSigniture(Match match)
    {
        String signiture="";
        ArrayList<String> res=new ArrayList<>();

        for (vertex v : tgfd.getPattern().getGraph().vertexSet()) {
            vertex currentMatchedVertex = match.getMapping().getVertexCorrespondence(v, false);
            if (currentMatchedVertex != null) {
                for (literal l:tgfd.getDependency().getX()) {
                    if(l instanceof constantLiteral)
                    {
                        if(currentMatchedVertex.getTypes().contains(((constantLiteral) l).getVertexType()))
                        {
                            //if(currentMatchedVertex.attContains())
                        }
                    }
                    else if(l instanceof variableLiteral)
                    {
                    }
                }
            }
        }



        return signiture;
    }

}

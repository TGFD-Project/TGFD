package QPath;

import java.util.ArrayList;

public class QPathMatch {

    private int matchID;
    private ArrayList<Triple> matchesInTriple;

    public QPathMatch(int matchID,ArrayList<Triple> matchesInTriple)
    {
        this.matchesInTriple=matchesInTriple;
        this.matchID=matchID;
    }

    public QPathMatch(int matchID,Triple firstMatchInTriple)
    {
        this.matchesInTriple=new ArrayList<>();
        this.matchesInTriple.add(firstMatchInTriple);
        this.matchID=matchID;
    }

    public void addTripleMatch(Triple nextMatch)
    {
        this.matchesInTriple.add(nextMatch);
    }

    public ArrayList<Triple> getMatchesInTriple() {
        return matchesInTriple;
    }

    public int getMatchID() {
        return matchID;
    }

    public boolean isUnSatEmpty()
    {
        for (Triple triple: this.matchesInTriple) {
            if(!triple.isUnSatEmpty())
                return false;
        }
        return true;
    }
}

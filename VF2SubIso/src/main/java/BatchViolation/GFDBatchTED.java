package BatchViolation;

import ICs.TGFD;
import Infra.*;
import Violations.Violation;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GFDBatchTED {

    private MatchCollection matches;
    private TGFD tgfd;


    /**
     * We simulate GFD inconsistency detection using TGFDs
     * @param allMatches set of matches
     * @param tgfd input TGFD, This will be considered as GFD by ignoring the interval and compare matches at the same timestamp
     */
    public GFDBatchTED(MatchCollection allMatches, TGFD tgfd)
    {
        this.tgfd=tgfd;
        this.matches=allMatches;
    }

    /**
     * @return Returns a set of violations for the given TGFD (simulated as GFD) over the matches
     */
    public Set<Violation> findViolations()
    {
        Set<Violation> violations=new HashSet<>();
        LocalDate[] allSnapshots = matches.getTimestamps();
        for(int i = 0; i < allSnapshots.length; i++)
        {
            List<Match> firstMatches=matches.getMatches(allSnapshots[i]);
            Interval intv=new Interval(allSnapshots[i],allSnapshots[i]);
            List<Match> secondMatches=matches.getMatches(allSnapshots[i]);
            for (Match first:firstMatches) {
                String firstSignatureX=first.getSignatureX();
                String firstSignatureY="";
                firstSignatureY=first.getSignatureY();
                for (Match second:secondMatches) {
                    if(firstSignatureX.equals(second.getSignatureX()))
                    {
                        //Here, they both should have the same signature Y
                        String secondSignatureY="";
                        secondSignatureY=second.getSignatureY();
                        if(!firstSignatureY.equals(secondSignatureY))
                        {
                            //Violation happened.
                            violations.add(new Violation(first,second,intv,firstSignatureX,firstSignatureY,secondSignatureY));
                        }
                    }
                }
            }
        }
        return violations;
    }
}

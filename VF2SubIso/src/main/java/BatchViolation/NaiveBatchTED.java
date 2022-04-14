package BatchViolation;

import Violations.Violation;

import Infra.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NaiveBatchTED {

    private MatchCollection matches;
    private TGFD tgfd;


    public NaiveBatchTED(MatchCollection allMatches, TGFD tgfd)
    {
        this.tgfd=tgfd;
        this.matches=allMatches;
    }

    public Set<Violation> findViolations()
    {
        Set<Violation> violations=new HashSet<>();
        Delta delta=tgfd.getDelta();

        LocalDate[] allSnapshots = matches.getTimestamps();
        for(int i = 0; i < allSnapshots.length; i++)
        {
            List<Match> firstMatches=matches.getMatches(allSnapshots[i]);
            for (int j = 0; j < allSnapshots.length; j++)
            {
                Interval intv=new Interval(allSnapshots[i],allSnapshots[j]);
                if(intv.inDelta(delta.getMin(),delta.getMax()))
                {
                    List<Match> secondMatches=matches.getMatches(allSnapshots[j]);
                    for (Match first:firstMatches) {
                        String firstSignatureX=first.getSignatureX();
                        String firstSignatureY=first.getSignatureY(allSnapshots[i]);
//                        if(first.getMatchMapping()!=null)
//                            firstSignatureY=Match.signatureFromY(tgfd.getPattern(),first.getMatchMapping(),tgfd.getDependency().getY());
//                        else
//                            firstSignatureY=Match.signatureFromY(tgfd.getPattern(),first.getMatchVertexMapping(),tgfd.getDependency().getY());
                        for (Match second:secondMatches) {
                            if(firstSignatureX.equals(second.getSignatureX()))
                            {
                                //Here, they both should have the same signature Y
                                String secondSignatureY=second.getSignatureY(allSnapshots[j]);
//                                if(second.getMatchMapping()!=null)
//                                    secondSignatureY=Match.signatureFromY(tgfd.getPattern(),second.getMatchMapping(),tgfd.getDependency().getY());
//                                else
//                                    secondSignatureY=Match.signatureFromY(tgfd.getPattern(),second.getMatchVertexMapping(),tgfd.getDependency().getY());
                                if(!firstSignatureY.equals(secondSignatureY))
                                {
                                    //Violation happened.
                                    violations.add(new Violation(first,second,intv,firstSignatureX,firstSignatureY,secondSignatureY));
                                    // counter of violations
                                }
                                else
                                {
                                    // counter of satisfied matches
                                }
                            }
                        }
                    }
                }
            }
        }
        return violations;
    }
}

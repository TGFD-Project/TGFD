package BatchViolation;

import ICs.TGFD;
import Infra.*;
import Violations.Violation;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class OptBatchTED {

    private MatchCollection matches;
    private TGFD tgfd;


    public OptBatchTED(MatchCollection allMatches, TGFD tgfd)
    {
        this.tgfd=tgfd;
        this.matches=allMatches;
    }

    public Set<Violation> findViolations()
    {
        Set<Violation> violations=new HashSet<>();
        //TODO: Rewrite the class.
//        Delta delta=tgfd.getDelta();
//        var Y=tgfd.getDependency().getY().get(0);
//
//        for (Match match:matches.getMatches()) {
//            // If Y is a constant literal
//            if(Y.getLiteralType()==Literal.LiteralType.Constant)
//            {
//                for (String signatureY:match.getSignatureYWithInterval().keySet()) {
//                    ConstantLiteral cstY=(ConstantLiteral) Y;
//                    if(!signatureY.equals(cstY.getAttrValue()))
//                    {
//                        //Violation is detected. Constant literal violation
//                        violations.add(new Violation(match,match,match.getSignatureYWithInterval().get(signatureY).get(0),"","",""));
//                    }
//                }
//            }
//            else // Y is a variable literal
//            {
//                //Create a HashMap based on Intervals to access the signatureY
//                HashMap<Interval, String> signatureYs=new HashMap<>();
//                //Create a HashMap to add the checking intervals for each signatureY
//                //Any interval intersects with a checking interval, should have the same signatureY
//                HashMap<Interval,String> checks=new HashMap<>();
//
//                for (String signatureY:match.getSignatureYWithInterval().keySet()) {
//                    for (Interval interval:match.getSignatureYWithInterval().get(signatureY)) {
//
//                        // Add to the reversed HasMap
//                        signatureYs.put(interval,signatureY);
//
//                        // Add the checking interval for each signatureY based on its interval
//                        LocalDate newStart=interval.getStart().plusDays(delta.getMin().getDays());
//                        if(newStart.isBefore(interval.getEnd()))
//                            newStart=interval.getStart();
//                        LocalDate newEnd=interval.getEnd().plusDays(delta.getMax().getDays());
//                        checks.put(new Interval(newStart,newEnd),signatureY);
//                    }
//                }
//                // Now, we iterate the intervals again to check if they intersect with any checking interval
//                // If they intersect and have a different signatureY, then we have a violation.
//                for (Interval v:signatureYs.keySet()) {
//                    for (Interval v2:checks.keySet()) {
//                        if(v2.intersects(v) && !checks.get(v2).equals(signatureYs.get(v)))
//                        {
//                            //Violation is detected...
//                            violations.add(new Violation(match,match,v2,"","",""));
//                        }
//                    }
//                }
//            }
//        }
        return violations;
    }
}

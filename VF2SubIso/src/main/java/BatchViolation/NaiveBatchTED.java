package BatchViolation;

import ICs.TGFD;
import Infra.*;
import Violations.Violation;

import java.time.LocalDate;
import java.util.*;

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
        int totalNumberOfMatches = 0;
        HashSet<String> uniquePatients = new HashSet<>();
        //TODO: Sort the allSnapshots so j starts at i in the inner loop
        for(int i = 0; i < allSnapshots.length; i++)
        {
            List<Match> firstMatches=matches.getMatches(allSnapshots[i]);
            totalNumberOfMatches+= firstMatches.size();
            for (int j = 0; j < allSnapshots.length; j++)
            {
                Interval intv=new Interval(allSnapshots[i],allSnapshots[j]);
                if(intv.inDelta(delta.getMin(),delta.getMax()))
                {
                    List<Match> secondMatches=matches.getMatches(allSnapshots[j]);
                    for (Match first:firstMatches) {
                        String firstSignatureX=first.getSignatureX();
                        uniquePatients.add(first.getSignatureFromPattern().split(",")[0]);
                        String firstSignatureY=first.getSignatureY();
//                        if(first.getMatchMapping()!=null)
//                            firstSignatureY=Match.signatureFromY(tgfd.getPattern(),first.getMatchMapping(),tgfd.getDependency().getY());
//                        else
//                            firstSignatureY=Match.signatureFromY(tgfd.getPattern(),first.getMatchVertexMapping(),tgfd.getDependency().getY());
                        for (Match second:secondMatches) {
                            uniquePatients.add(second.getSignatureFromPattern().split(",")[0]);


                            if(firstSignatureX.equals(second.getSignatureX()))
                            {
                                //Here, they both should have the same signature Y
                                String secondSignatureY=second.getSignatureY();
                                if(secondSignatureY==null)
                                {
                                    //System.out.println("Error(2)!" + second.getSignatureX());
                                    continue;
                                }
                                if(firstSignatureY==null)
                                {
                                    //System.out.println("Error(1)!" + first.getSignatureX());
                                    continue;
                                }
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
        System.out.println("Total Number of matches: "+ totalNumberOfMatches + " - Unique: " + uniquePatients.size());
        uniquePatients.forEach(System.out::println);
        return violations;
    }
}

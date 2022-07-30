package ErrorInjection;

import ICs.TGFD;
import Infra.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDate;
import java.util.*;

public class ErrorGenerator {

    private MatchCollection matches;
    private TGFD tgfd;
    private double errorRate;
    private HashMap<Integer,ArrayList<String>> domainX=new HashMap <>();
    private HashSet<Integer> positiveErrors=new HashSet <>();
    private HashSet<Integer> negativeErrors=new HashSet <>();


    public ErrorGenerator(MatchCollection allMatches, TGFD tgfd)
    {
        this.tgfd=tgfd;
        this.matches=allMatches;
    }

    public void evaluate(double errorRate)
    {
        this.errorRate=errorRate;
        List<PairsOfMatches> pairs=findpairs();
        findDomain(pairs);
        injectError(pairs,tgfd.getName().hashCode());
        findViolations(pairs);
    }

    private List<PairsOfMatches> findpairs()
    {
        Delta delta=tgfd.getDelta();
        List<PairsOfMatches> pairs=new ArrayList <>();

        LocalDate[] allSnapshots = matches.getTimestamps();
        for(int i = 0; i < allSnapshots.length; i++)
        {
            List <Match> firstMatches=matches.getMatches(allSnapshots[i]);
            for (int j = i; j < allSnapshots.length; j++)
            {
                Interval intv=new Interval(allSnapshots[i],allSnapshots[j]);
                if(intv.inDelta(delta.getMin(),delta.getMax()))
                {
                    List<Match> secondMatches=matches.getMatches(allSnapshots[j]);
                    for (Match first:firstMatches) {
                        String firstSignatureX=first.getSignatureX();
                        String firstSignatureY=first.getSignatureY();
                        for (Match second:secondMatches) {
                            if(firstSignatureX.equals(second.getSignatureX()))
                            {
                                String secondSignatureY=second.getSignatureY();
                                if(firstSignatureY.equals(secondSignatureY))
                                {
                                    PairsOfMatches pair=new PairsOfMatches(first,second,firstSignatureX,firstSignatureY,intv);
                                    pairs.add(pair);
                                }
                            }
                        }
                    }
                }
            }
        }
        return pairs;
    }

    private void injectError(List<PairsOfMatches> pairs, int seed)
    {
        int numberOfErrorsToBeInjected= (int) (pairs.size()*errorRate);
        Random random=new Random(seed);
        for (int i=0;i<numberOfErrorsToBeInjected;i++)
        {
            while (true)
            {
                int id = random.nextInt(pairs.size());
                if(!positiveErrors.contains(id))
                {
                    positiveErrors.add(id);
                    break;
                }
            }
        }
        for (int i=0;i<numberOfErrorsToBeInjected;i++)
        {
            while (true)
            {
                int id = random.nextInt(pairs.size());
                if(!positiveErrors.contains(id) && !negativeErrors.contains(id))
                {
                    negativeErrors.add(id);
                    break;
                }
            }
        }
        for (Integer id:positiveErrors) {
            PairsOfMatches pair=pairs.get(id);
            pair.Y1 = RandomStringUtils.randomAlphabetic(10);
        }
        for (Integer id:negativeErrors) {
            PairsOfMatches pair=pairs.get(id);
            makeNegativeChange(pair,random);
        }
    }

    private void makeNegativeChange(PairsOfMatches pair,Random random)
    {
        String []temp=pair.X.split(",");
        while (true)
        {
            int id = random.nextInt(temp.length);
            if(!temp[id].equals(","))
            {
                String newValue=domainX.get(id).get(random.nextInt(domainX.get(id).size()));
                pair.X = pair.X.replace(temp[id],newValue);
                break;
            }
        }
    }

    private void findDomain(List<PairsOfMatches> pairs)
    {
        for (PairsOfMatches pair:pairs) {
            String []temp=pair.X.split(",");
            for (int i=0;i<temp.length;i++)
            {
                if(!temp[i].equals(","))
                {
                    if(!domainX.containsKey(i))
                        domainX.put(i,new ArrayList <>());
                    if(!domainX.get(i).contains(temp[i]))
                        domainX.get(i).add(temp[i]);
                }
            }
        }
    }

    private void findViolations(List<PairsOfMatches> pairs) {
        System.out.println("Total number of positive errors: " + positiveErrors.size());
        System.out.println("Total number of negative errors: " + negativeErrors.size());
        int TGFDPositiveErrors = 0, GFDPositiveErrors = 0;
        int TGFDNegativeErrors = 0, GFDNegativeErrors = 0;
        for (int id : positiveErrors) {
            PairsOfMatches pair = pairs.get(id);
            if (!pair.Y1.equals(pair.Y2))
                TGFDPositiveErrors++;
            if (pair.interval.getStart().equals(pair.interval.getEnd()) && !pair.Y1.equals(pair.Y2))
                GFDPositiveErrors++;
        }

        for (int id : negativeErrors) {
            PairsOfMatches pair = pairs.get(id);

            Delta delta = tgfd.getDelta();
            LocalDate[] allSnapshots = matches.getTimestamps();
            for (int i = 0; i < allSnapshots.length; i++) {
                List <Match> firstMatches = matches.getMatches(allSnapshots[i]);
                Interval intv = new Interval(allSnapshots[i], pair.interval.getStart());
                if (intv.inDelta(delta.getMin(), delta.getMax())) {
                    for (Match first : firstMatches) {
                        String firstSignatureX = first.getSignatureX();
                        String firstSignatureY = first.getSignatureY();
                        if (firstSignatureX.equals(pair.X)) {
                            if (!firstSignatureY.equals(pair.Y1)) {
                                TGFDNegativeErrors++;
                                if (pair.interval.getStart().equals(intv.getStart()))
                                    GFDNegativeErrors++;
                            }
                        }
                    }
                }
            }
        }
        int TGFDTotal=TGFDPositiveErrors+TGFDNegativeErrors;
        int GFDTotal=GFDPositiveErrors+GFDNegativeErrors;
        ////////////////////////////////

        double TGFDPrecision=(double)TGFDPositiveErrors/(double)TGFDTotal;
        double TGFDRecall=(double)TGFDPositiveErrors/(double)positiveErrors.size();
        double TGFDFP=(double)TGFDNegativeErrors/(double)negativeErrors.size();
        double TGFDF1Score=2*((TGFDPrecision*TGFDRecall)/(TGFDPrecision+TGFDRecall));

        ////////////////////////////////

        double GFDPrecision=(double)GFDPositiveErrors/(double)GFDTotal;
        double GFDRecall=(double)GFDPositiveErrors/(double)positiveErrors.size();
        double GFDFP=(double)GFDNegativeErrors/(double)negativeErrors.size();
        double GFDF1Score=2*((GFDPrecision*GFDRecall)/(GFDPrecision+GFDRecall));

        System.out.println("TGFD precision: " + TGFDPrecision + " ** TGFD recall:" + TGFDRecall+ " ** TGFD F.P.:" + TGFDFP+ " ** TGFD F1:" + TGFDF1Score);
        System.out.println("GFD precision: " + GFDPrecision + " ** GFD recall:" + GFDRecall+ " ** GFD F.P.:" + GFDFP+ " ** GFD F1:" + GFDF1Score);
    }

    private class PairsOfMatches
    {
        Match first,second;
        String X,Y1,Y2;
        Interval interval;

        public PairsOfMatches(Match first, Match second, String X, String Y, Interval interval)
        {
            this.first=first;
            this.second=second;
            this.X=X;
            this.Y1=Y;
            this.Y2=Y;
            this.interval=interval;
        }
    }


}

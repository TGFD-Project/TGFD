package Violations;

import Infra.Interval;
import Infra.Match;

public class Violation {
    private Match match1,match2;
    private String X,Y1,Y2;
    private Interval interval;

    public Violation(Match match1,Match match2, Interval interval,String X, String Y1, String Y2)
    {
        this.match1=match1;
        this.match2=match2;
        this.interval=interval;
        this.X=X;
        this.Y1=Y1;
        this.Y2=Y2;
    }

    public String getX() {
        return X;
    }

    public String getY1() {
        return Y1;
    }

    public String getY2() {
        return Y2;
    }

    public Interval getInterval() {
        return interval;
    }

    public Match getMatch1() {
        return match1;
    }

    public Match getMatch2() {
        return match2;
    }

    @Override
    public String toString() {
        return "Violation{" +
                "X=" + X +
                ", Y1=" + Y1 +
                ", Y2=" + Y2 +
                ", interval=" + interval +
                '}';
    }
}

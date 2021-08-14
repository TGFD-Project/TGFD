package Infra;

public class Violation {
    private Match match1,match2;
    private Interval interval;

    public Violation(Match match1,Match match2, Interval interval)
    {
        this.match1=match1;
        this.match2=match2;
        this.interval=interval;
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
                "match1=" + match1 +
                ", match2=" + match2 +
                ", interval=" + interval +
                '}';
    }
}

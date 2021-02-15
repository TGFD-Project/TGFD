package infra;

import java.time.Period;

public class Delta {

    private Period min;
    private Period max;

    public Delta(Period min, Period max)
    {
        this.min=min;
        this.max=max;
    }

    public Period getMax() {
        return max;
    }

    public Period getMin() {
        return min;
    }
}

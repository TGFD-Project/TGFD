package Infra;

import java.time.Duration;
import java.time.Period;

public class Delta {

    private Period min;
    private Period max;
    private Duration granularity;

    public Delta(Period min, Period max, Duration granularity)
    {
        this.min=min;
        this.max=max;
        this.granularity=granularity;
    }

    public Period getMax() {
        return max;
    }

    public Period getMin() {
        return min;
    }

    public Duration getGranularity() {
        return granularity;
    }


    @Override
    public String toString() {
        return "Delta{" +
                "min=" + min +
                ", max=" + max +
                ", granularity=" + granularity +
                '}';
    }
}

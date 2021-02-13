package infra;

/**
 * Represents an interval.
 */
public class Interval {
    // TODO: change to date and remove granularity [2021-02-13]
    private int start;
    private int end;
    private int granularity;

    /**
     * Creates an interval.
     * @param start Start of the interval.
     * @param end   End of the inteval.
     * @param granularity granularity of the steps from start to end
     */
    public Interval(int start, int end, int granularity) {
        this.start = start;
        this.end = end;
        this.granularity=granularity;
    }

    /**
     * Returns true if timepoint within the interval.
     */
    public boolean contains(int timepoint) {
        // TODO: determine if we should make the end exclusive? [2021-02-12]
        return start >= timepoint && timepoint <= end;
    }

    /**
     * Returns the start of the interval.
     */
    public int getStart() {
        return this.start;
    }

    /**
     * Returns the end of the interval.
     */
    public int getEnd() {
        return this.end;
    }

    public int getGranularity() {
        return granularity;
    }
}
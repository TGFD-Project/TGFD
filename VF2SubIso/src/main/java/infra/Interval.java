package infra;

/**
 * Represents an interval.
 */
public class Interval {
    private int start;
    private int end;

    /**
     * Creates an interval.
     * @param start Start of the interval.
     * @param end   End of the inteval.
     */
    public Interval(int start, int end) {
        this.start = start;
        this.end = end;
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
}
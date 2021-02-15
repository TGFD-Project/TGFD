package infra;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Represents an interval.
 */
public class Interval {
    //region --[Fields: Private]---------------------------------------
    // Start of the interval.
    private LocalDate start;
    // End of the interval.
    private LocalDate end;
    //endregion

    //region --[Constructors]------------------------------------------
    /**
     * Creates an interval.
     * @param start Start of the interval.
     * @param end   End of the inteval.
     */
    public Interval(LocalDate start, LocalDate end)
    {
        this.start = start;
        this.end = end;
    }
    //endregion

    //region --[Methods: Public]---------------------------------------
    /**
     * Returns true if timepoint within the interval (start and end inclusive).
     */
    public boolean contains(LocalDate timepoint) {
        return
            (timepoint.isEqual(start) || timepoint.isAfter(start)) &&
            (timepoint.isEqual(end)   || timepoint.isBefore(end));
    }

    /**
     * Returns true if interval within delta, otherwise, teruns false.
     * @param min Minimum time span of delta.
     * @param max Maximum time span of delta.
     * @return min <= (end - start) <= max
     */
    public boolean inDelta(Duration min, Duration max)
    {
        var difference = Duration.between(start, end);
        return difference.compareTo(min) >= 0 &&
               difference.compareTo(max) <= 0;
    }
    //endregion

    //region --[Properties: Public]------------------------------------
    /**
     * Returns the start of the interval.
     */
    public LocalDate getStart() {
        return this.start;
    }

    /**
     * Sets the start of the interval
     */
    public void setStart(LocalDate start) {
        this.start = start;
    }

    /**
     * Returns the end of the interval.
     */
    public LocalDate getEnd() {
        return this.end;
    }

    /**
     * Sets the end of the interval
     */
    public void setEnd(LocalDate end) {
        this.end = end;
    }
    //endregion
}
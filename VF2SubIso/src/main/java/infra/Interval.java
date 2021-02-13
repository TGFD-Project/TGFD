package infra;

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
    //endregion

    //region --[Properties: Public]------------------------------------
    /**
     * Returns the start of the interval.
     */
    public LocalDate getStart() {
        return this.start;
    }

    /**
     * Returns the end of the interval.
     */
    public LocalDate getEnd() {
        return this.end;
    }
    //endregion
}
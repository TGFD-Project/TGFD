package Infra;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;

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

    //region --[Methods: Private]--------------------------------------
    /** Returns approximate number of days in a period */
    private static double approxDaysFromPeriod(Period period) {
        if (period == null) {
            return 0d;
        }
        return 30.4167 * (12 * period.getYears() + period.getMonths()) + period.getDays();
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
     * Returns true if the other interval intersects with the interval (inclusive).
     */
    public boolean intersects(Interval other) {
        if(other.getStart().isAfter(end) || other.getStart().equals(end)
                || other.end.isBefore(start) || other.end.equals(start))
            return false;
        else
            return true;
    }


    /**
     * Returns true if interval within delta, otherwise, returns false.
     * @param min Minimum timespan of delta.
     * @param max Maximum timespan of delta.
     * @return min <= (end - start) <= max
     */
    public boolean inDelta(Duration min, Duration max)
    {
        var between = Duration.between(
            start.atStartOfDay(),
            end.atStartOfDay());
        return between.compareTo(min) >= 0 && // min <= between
               between.compareTo(max) <= 0;   // between <= max
    }

    /**
     * Returns true if interval within delta, otherwise, returns false.
     * @param min Minimum timespan of delta.
     * @param max Maximum timespan of delta.
     * @return min <= (end - start) <= max
     */
    public boolean inDelta(Period min, Period max)
    {
        var between = Period.between(start, end);
        if (min.getDays() > 0 || max.getDays() > 0)
        {
            // Period does not have a compareTo method because Period cannot be accurately compared
            // if it contains months and days since month has an undefined standard of length.
            // Best we can do is compare the approximate number of days.
            var daysBetween = approxDaysFromPeriod(between);
            return approxDaysFromPeriod(min) <= daysBetween && daysBetween <= approxDaysFromPeriod(max);
        }
        else
        {
            var monthsBetween = between.toTotalMonths();
            return
                min.toTotalMonths() <= monthsBetween &&
                (
                    monthsBetween < max.toTotalMonths() ||
                    (monthsBetween == max.toTotalMonths() && between.getDays() == 0)
                );
        }
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


    @Override
    public String toString() {
        return "Interval{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
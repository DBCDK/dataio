package dk.dbc.dataio.harvester;

import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TimeIntervalGeneratorTest {
    @Test
    public void endPointMustBeLaterThanStartingPoint() {
        final Instant pointInTime = Instant.now();
        final TimeIntervalGenerator generator = new TimeIntervalGenerator()
                .withStartingPoint(pointInTime)
                .withEndPoint(pointInTime.minusSeconds(60));
        assertThat(generator::iterator, isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void defaultValuesGeneratesSingleInterval() {
        final TimeIntervalGenerator generator = new TimeIntervalGenerator();
        final Iterator<TimeInterval> iterator = generator.iterator();
        assertThat("1st hasNext()", iterator.hasNext(), is(true));
        assertThat("1st next()", iterator.next(), is(notNullValue()));
        assertThat("2nd hasNext()", iterator.hasNext(), is(false));
        assertThat("2nd next()", iterator.next(), is(nullValue()));
    }

    @Test
    public void endPointEqualToStartingPointGeneratesSingleInterval() {
        final Instant pointInTime = Instant.now();
        final TimeIntervalGenerator generator = new TimeIntervalGenerator()
                .withStartingPoint(pointInTime)
                .withEndPoint(pointInTime);
        final Iterator<TimeInterval> iterator = generator.iterator();
        assertThat("1st hasNext()", iterator.hasNext(), is(true));
        assertThat("1st next()", iterator.next(), is(notNullValue()));
        assertThat("2nd hasNext()", iterator.hasNext(), is(false));
        assertThat("2nd next()", iterator.next(), is(nullValue()));
    }

    @Test
    public void intervalGeneration() {
        final Instant start = new Date(1485326980959L).toInstant();     // 2017-01-25T06:49:40.959Z
        final Instant end = start.plusSeconds(3900);                          // 2017-01-25T07:54:40.959Z

        final TimeIntervalGenerator generator = new TimeIntervalGenerator()
                .withStartingPoint(start)
                .withEndPoint(end)
                .withIntervalDuration(10, ChronoUnit.MINUTES);

        final Iterator<TimeInterval> iterator = generator.iterator();
        assertThat("1st hasNext()", iterator.hasNext(), is(true));
        assertThat("1st next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T06:49:40.959Z, to=2017-01-25T06:59:40.959Z}"));
        assertThat("2nd hasNext()", iterator.hasNext(), is(true));
        assertThat("2nd next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T06:59:40.959Z, to=2017-01-25T07:09:40.959Z}"));
        assertThat("3rd hasNext()", iterator.hasNext(), is(true));
        assertThat("3rd next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T07:09:40.959Z, to=2017-01-25T07:19:40.959Z}"));
        assertThat("4th hasNext()", iterator.hasNext(), is(true));
        assertThat("4th next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T07:19:40.959Z, to=2017-01-25T07:29:40.959Z}"));
        assertThat("5th hasNext()", iterator.hasNext(), is(true));
        assertThat("5th next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T07:29:40.959Z, to=2017-01-25T07:39:40.959Z}"));
        assertThat("6th hasNext()", iterator.hasNext(), is(true));
        assertThat("6th next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T07:39:40.959Z, to=2017-01-25T07:49:40.959Z}"));
        assertThat("7th hasNext()", iterator.hasNext(), is(true));
        assertThat("7th next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T07:49:40.959Z, to=2017-01-25T07:54:40.959Z}"));
        assertThat("8th hasNext()", iterator.hasNext(), is(false));
        assertThat("8th next()", iterator.next(), is(nullValue()));
    }

    @Test
    public void intervalDurationIsGreaterThanStartingPointEndPointDuration() {
        final Instant start = new Date(1485326980959L).toInstant();     // 2017-01-25T06:49:40.959Z
        final Instant end = start.plusSeconds(60);                            // 2017-01-25T06:50:40.959Z

        final TimeIntervalGenerator generator = new TimeIntervalGenerator()
                .withStartingPoint(start)
                .withEndPoint(end)
                .withIntervalDuration(10, ChronoUnit.MINUTES);

        final Iterator<TimeInterval> iterator = generator.iterator();
        assertThat("1st hasNext()", iterator.hasNext(), is(true));
        assertThat("1st next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T06:49:40.959Z, to=2017-01-25T06:50:40.959Z}"));
        assertThat("2nd hasNext()", iterator.hasNext(), is(false));
        assertThat("2nd next()", iterator.next(), is(nullValue()));
    }

    @Test
    public void intervalDurationHitsEndPointBoundaryExactly() {
        final Instant start = new Date(1485326980959L).toInstant();     // 2017-01-25T06:49:40.959Z
        final Instant end = start.plusSeconds(1200);                          // 2017-01-25T07:09:40.959Z

        final TimeIntervalGenerator generator = new TimeIntervalGenerator()
                .withStartingPoint(start)
                .withEndPoint(end)
                .withIntervalDuration(10, ChronoUnit.MINUTES);

        final Iterator<TimeInterval> iterator = generator.iterator();
        assertThat("1st hasNext()", iterator.hasNext(), is(true));
        assertThat("1st next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T06:49:40.959Z, to=2017-01-25T06:59:40.959Z}"));
        assertThat("2nd hasNext()", iterator.hasNext(), is(true));
        assertThat("2nd next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T06:59:40.959Z, to=2017-01-25T07:09:40.959Z}"));
        assertThat("3rd hasNext()", iterator.hasNext(), is(false));
        assertThat("3rd next()", iterator.next(), is(nullValue()));
    }

    @Test
    public void endPointSetWithLag() {
        final Instant start = new Date(1485326980959L).toInstant();     // 2017-01-25T06:49:40.959Z
        final Instant end = start.plusSeconds(60);                            // 2017-01-25T06:50:40.959Z

        final TimeIntervalGenerator generator = new TimeIntervalGenerator()
                .withStartingPoint(start)
                .withEndPoint(end, 50, ChronoUnit.SECONDS)
                .withIntervalDuration(10, ChronoUnit.MINUTES);

        final Iterator<TimeInterval> iterator = generator.iterator();
        assertThat("1st hasNext()", iterator.hasNext(), is(true));
        assertThat("1st next()", iterator.next().toString(),
                is("TimeInterval{from=2017-01-25T06:49:40.959Z, to=2017-01-25T06:49:50.959Z}"));
        assertThat("2nd hasNext()", iterator.hasNext(), is(false));
        assertThat("2nd next()", iterator.next(), is(nullValue()));
    }
}

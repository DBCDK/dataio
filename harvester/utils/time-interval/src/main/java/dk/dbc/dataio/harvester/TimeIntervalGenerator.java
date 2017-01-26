/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Iterator;

/**
 * Generator for {@link TimeInterval}s of a specified duration over a period from start and end points in time.
 */
public class TimeIntervalGenerator implements Iterable<TimeInterval> {
    private long intervalDuration = 30;
    private TemporalUnit unit = ChronoUnit.SECONDS;
    private Instant endPoint = Instant.now();
    private Instant startingPoint = endPoint.minus(intervalDuration, unit);

    public TimeIntervalGenerator withStartingPoint(Instant startingPoint) {
        this.startingPoint = startingPoint;
        return this;
    }

    public TimeIntervalGenerator withEndPoint(Instant endPoint) {
        this.endPoint = endPoint;
        return this;
    }

    public TimeIntervalGenerator withEndPoint(Instant endPoint, long lag, TemporalUnit unit) {
        this.endPoint = endPoint.minus(lag, unit);
        return this;
    }

    public TimeIntervalGenerator withIntervalDuration(long duration, TemporalUnit unit) {
        this.intervalDuration = duration;
        this.unit = unit;
        return this;
    }

    @Override
    public Iterator<TimeInterval> iterator() {
        return new TimeIntervalIterator(startingPoint, endPoint, intervalDuration, unit);
    }

    private static class TimeIntervalIterator implements Iterator<TimeInterval> {
        private final Instant endPoint;
        private final long intervalDuration;
        private final TemporalUnit unit;

        private Instant from;
        private Instant to;

        TimeIntervalIterator(Instant startingPoint, Instant endPoint, long intervalDuration, TemporalUnit unit) {
            if (startingPoint.isAfter(endPoint)) {
                throw new IllegalArgumentException(String.format("Starting point %s must be before end point %s", startingPoint, endPoint));
            }
            this.endPoint = endPoint;
            this.intervalDuration = intervalDuration;
            this.unit = unit;
            this.from = startingPoint;
        }

        @Override
        public boolean hasNext() {
            return !endPoint.equals(to);
        }

        @Override
        public TimeInterval next() {
            if (endPoint.equals(to)) {
                return null;
            }
            to = from.plus(intervalDuration, unit);
            if (to.isAfter(endPoint)) {
                to = endPoint;
            }
            final TimeInterval timeInterval = new TimeInterval(from, to);
            from = to;
            return timeInterval;
        }
    }
}

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

package dk.dbc.dataio.jobstore.service.util;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TrackingIdGeneratorTest {


    @Test
    public void getTrackingId_ipAddressLocated_trackingIdReturned() throws UnknownHostException {
        String separator = "-";
        String trackingId = TrackingIdGenerator.getTrackingId(42, 1, (short)0);
        assertThat(trackingId, is(InetAddress.getLocalHost().getHostAddress() + separator + 42 + separator + 1 + separator + 0));
    }

    @Test
    public void getTrackingId_marcRecord() {
        String trackingId = TrackingIdGenerator.getTrackingId(101010, "876592823", 42, 1, (short)0);
        assertThat(trackingId, is("{876592823:101010}-42-1-0"));
    }
}

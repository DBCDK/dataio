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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * This static class is used to generate dataIO specific trackingId's
 */

public final class TrackingIdGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackingIdGenerator.class);
    private static String ipAddress;
    private static final String separator = "-";
    static{
        try {
            //the raw IP address in a string format.
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ipAddress = UUID.randomUUID().toString();
            LOGGER.info("IP address of host could not be determined. Using immutable universally unique identifier with value: {}", ipAddress, e);
        }
    }

    public static String getTrackingId(int jobId, int chunkId, short itemId) {
        return ipAddress + separator + jobId + separator + chunkId + separator + itemId;
    }

}

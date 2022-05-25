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
    private static final String SEPARATOR = "-";
    static{
        try {
            //the raw IP address in a string format.
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ipAddress = UUID.randomUUID().toString();
            LOGGER.warn("IP address of host could not be determined. Using immutable universally unique identifier with value: {}", ipAddress, e);
        }
    }

    public static String getTrackingId(int jobId, int chunkId, short itemId) {
        return ipAddress + SEPARATOR + jobId + SEPARATOR + chunkId + SEPARATOR + itemId;
    }

    public static String getTrackingId(long submitterId, String recordId,  int jobId, int chunkId, short itemId) {
        return "{" + recordId + ":" + submitterId + "}" + SEPARATOR + jobId + SEPARATOR + chunkId + SEPARATOR + itemId;
    }

}

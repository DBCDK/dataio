package dk.dbc.dataio.jobstore.service.util;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TrackingIdGeneratorTest {


    @Test
    public void getTrackingId_ipAddressLocated_trackingIdReturned() throws UnknownHostException {
        String separator = "-";
        String trackingId = TrackingIdGenerator.getTrackingId(42, 1, (short) 0);
        assertThat(trackingId, is(InetAddress.getLocalHost().getHostAddress() + separator + 42 + separator + 1 + separator + 0));
    }

    @Test
    public void getTrackingId_marcRecord() {
        String trackingId = TrackingIdGenerator.getTrackingId(101010, "876592823", 42, 1, (short) 0);
        assertThat(trackingId, is("{876592823:101010}-42-1-0"));
    }
}

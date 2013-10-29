package dk.dbc.dataio.flowstore.ejb;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Ignore;

public class SinkBeanTest {

    @Test
    public void sinkBean_validConstructor_newInstance() {
        SinkBean sink = new SinkBean();
    }

    @Test(expected = NullPointerException.class)
    public void createSink_nullSinkContent_throws() {
        SinkBean sink = new SinkBean();
        sink.createSink(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createSink_emptySinkContent_throws() {
        SinkBean sink = new SinkBean();
        sink.createSink(null, "");
    }

    @Ignore
    @Test
    public void createSink_validSinkComponent_success() {
        SinkBean sink = new SinkBean();
        sink.createSink(null, "");
    }
}
package dk.dbc.dataio.sink.worldcat;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HoldingTest {
    @Test
    public void wciruValues() {
        assertThat("INSERT", Holding.Action.INSERT.getWciruValue(), is("I"));
        assertThat("DELETE", Holding.Action.DELETE.getWciruValue(), is("D"));
    }

}

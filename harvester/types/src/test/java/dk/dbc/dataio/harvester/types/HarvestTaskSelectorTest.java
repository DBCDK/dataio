package dk.dbc.dataio.harvester.types;

import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HarvestTaskSelectorTest {
    @Test
    public void stringRepresentation() {
        assertThat(new HarvestTaskSelector("key", "val").toString(), is("key = val"));
        assertThat(new HarvestTaskSelector("key", null).toString(), is("key = null"));
        assertThat(new HarvestTaskSelector(null, "val").toString(), is("null = val"));
    }

    @Test
    public void of() {
        assertThat("key=val", HarvestTaskSelector.of("key=val").toString(), is("key = val"));
        assertThat("  key  =  val  ", HarvestTaskSelector.of("  key  =  val  ").toString(), is("key = val"));
        assertThat("key=val1=val2", HarvestTaskSelector.of("key=val1=val2").toString(), is("key = val1=val2"));
        assertThat("no value", () -> HarvestTaskSelector.of("key"), isThrowing(IllegalArgumentException.class));
        assertThat("no equals", () -> HarvestTaskSelector.of("key : val"), isThrowing(IllegalArgumentException.class));
    }
}

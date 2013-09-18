package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.flowstore.util.json.JsonException;
import org.junit.Test;

/**
  * FlowBinder unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class FlowBinderTest {
    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent("<not_json/>");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent("");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent(null);
    }

    @Test(expected = NullPointerException.class)
    public void generateSearchIndexEntries_flowBinderArgIsNull_throws() throws Exception {
        FlowBinder.generateSearchIndexEntries(null);
    }

    @Test(expected = JsonException.class)
    public void generateSearchIndexEntries_flowBinderHasNoContent_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        FlowBinder.generateSearchIndexEntries(binder);
    }
}

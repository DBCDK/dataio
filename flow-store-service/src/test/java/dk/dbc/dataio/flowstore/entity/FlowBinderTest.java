package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
  * FlowBinder unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class FlowBinderTest {
    @Test
    public void setContent_jsonDataArgIsValidFlowBinderContentJson_setsNameIndexValue() throws Exception {
        final String name = "testbinder";
        final String flowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setName(name)
                .build();

        final FlowBinder binder = new FlowBinder();
        binder.setContent(flowBinderContent);
        assertThat(binder.getNameIndexValue(), is(name));
    }

    @Test
    public void setContent_jsonDataArgIsValidFlowBinderContentJson_setsSubmitterIds() throws Exception {
        final Set<Long> submitterIds = new HashSet<>(2);
        submitterIds.add(42L);
        submitterIds.add(43L);
        final String flowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setSubmitterIds(new ArrayList<Long>(submitterIds))
                .build();

        final FlowBinder binder = new FlowBinder();
        binder.setContent(flowBinderContent);
        assertThat(binder.getSubmitterIds(), is(submitterIds));
    }

    @Test
    public void setContent_jsonDataArgIsValidFlowBinderContentJson_setsFlowId() throws Exception {
        final long flowId = 42L;
        final String flowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setFlowId(flowId)
                .build();

        final FlowBinder binder = new FlowBinder();
        binder.setContent(flowBinderContent);
        assertThat(binder.getFlowId(), is(flowId));
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidFlowBinderContentJson_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent("{}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent("{");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent("");
    }

    @Test(expected = NullPointerException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent(null);
    }

    @Test(expected = NullPointerException.class)
    public void generateSearchIndexEntries_flowBinderArgIsNull_throws() throws Exception {
        FlowBinder.generateSearchIndexEntries(null);
    }

    @Test(expected = NullPointerException.class)
    public void generateSearchIndexEntries_flowBinderHasNoContent_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        FlowBinder.generateSearchIndexEntries(binder);
    }

    @Test
    public void generateSearchIndexEntries_flowBinderIsAttachedToMultipleSubmitters_createsSearchIndexEntryForEachSubmitter() throws Exception {
        final String packaging = "packaging";
        final String format = "format";
        final String charset = "charset";
        final String destination = "destination";
        final Submitter submitter1 = new Submitter();
        final Submitter submitter2 = new Submitter();
        submitter1.setContent(new ITUtil.SubmitterContentJsonBuilder().build());
        submitter2.setContent(new ITUtil.SubmitterContentJsonBuilder().build());

        Set<Submitter> submitters = new HashSet<>(2);
        submitters.add(submitter1);
        submitters.add(submitter2);
        final String flowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setPackaging(packaging)
                .setFormat(format)
                .setCharset(charset)
                .setDestination(destination)
                .build();

        final FlowBinder binder = new FlowBinder();
        binder.setContent(flowBinderContent);
        binder.setSubmitters(submitters);
        final List<FlowBinderSearchIndexEntry> entries = FlowBinder.generateSearchIndexEntries(binder);
        assertThat(entries.size(), is(2));
    }
}

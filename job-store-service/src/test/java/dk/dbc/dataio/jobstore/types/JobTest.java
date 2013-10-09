package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class JobTest {
    private static final long ID = 42L;
    private static final Path ORIGINAL_DATA_FILE = Paths.get("data-file");
    private static final Flow FLOW = createDefaultFlow();

    @Test(expected = IllegalArgumentException.class)
    public void constructor_idArgIsBelowThreshold_throws() {
        new Job(Job.ID_LOWER_THRESHOLD, ORIGINAL_DATA_FILE, FLOW);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_originalDataFileArgIsNull_throws() {
        new Job(ID, null, FLOW);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_flowArgIsNull_throws() {
        new Job(ID, ORIGINAL_DATA_FILE, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Job instance = new Job(ID, ORIGINAL_DATA_FILE, FLOW);
        assertThat(instance, is(notNullValue()));
    }

    public static Flow createDefaultFlow() {
        try {
            return JsonUtil.fromJson(new ITUtil.FlowJsonBuilder().build(), Flow.class, MixIns.getMixIns());
        } catch (JsonException e) {
            throw new IllegalStateException(e);
        }
    }
}

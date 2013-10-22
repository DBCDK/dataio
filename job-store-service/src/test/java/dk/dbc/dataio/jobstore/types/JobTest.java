package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class JobTest {
    private static final JobInfo JOB_INFO = createDefaultJobinfo();
    private static final Flow FLOW = createDefaultFlow();

    @Test(expected = NullPointerException.class)
    public void constructor_jobInfoArgIsNull_throws() {
        new Job(null, FLOW);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_flowArgIsNull_throws() {
        new Job(JOB_INFO, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Job instance = new Job(JOB_INFO, FLOW);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void getId_returnsIdFromEmbeddedJobInfo() {
        final Job instance = new Job(JOB_INFO, FLOW);
        assertThat(instance.getId(), is(JOB_INFO.getJobId()));
    }

    public static JobInfo createDefaultJobinfo() {
        try {
            return JsonUtil.fromJson(new ITUtil.JobInfoJsonBuilder().build(), JobInfo.class, MixIns.getMixIns());
        } catch (JsonException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Flow createDefaultFlow() {
        try {
            return JsonUtil.fromJson(new ITUtil.FlowJsonBuilder().build(), Flow.class, MixIns.getMixIns());
        } catch (JsonException e) {
            throw new IllegalStateException(e);
        }
    }
}

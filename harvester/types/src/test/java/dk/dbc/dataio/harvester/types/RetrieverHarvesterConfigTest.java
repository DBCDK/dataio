package dk.dbc.dataio.harvester.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class RetrieverHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaults() throws JSONBException {
        final RetrieverHarvesterConfig config = new RetrieverHarvesterConfig(1, 2,
                new RetrieverHarvesterConfig.Content());
        final String configAsString = jsonbContext.marshall(config);

        final RetrieverHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, RetrieverHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString,
                is(config));
        assertThat("enabled default", configFromString.getContent().isEnabled(),
                is(false));
    }

    @Test
    public void getRetrieverSourceId_nullId_returnsNull() {
        RetrieverHarvesterConfig.Content content = new RetrieverHarvesterConfig.Content();
        assertThat(content.getRetrieverSourceId(), is(nullValue()));
    }

    @Test
    public void getRetrieverSourceId_numericOnlyId_returnsSameValue() {
        RetrieverHarvesterConfig.Content content = new RetrieverHarvesterConfig.Content()
                .withId("35010");
        assertThat(content.getRetrieverSourceId(), is("35010"));
    }

    @Test
    public void getRetrieverSourceId_numericIdWithSuffix_returnsLeadingDigitsOnly() {
        RetrieverHarvesterConfig.Content content = new RetrieverHarvesterConfig.Content()
                .withId("35010-Politiken-dm2");
        assertThat(content.getRetrieverSourceId(), is("35010"));
    }

    @Test
    public void getRetrieverSourceId_notIncludedInJson() throws JSONBException {
        RetrieverHarvesterConfig config = new RetrieverHarvesterConfig(1, 2,
                new RetrieverHarvesterConfig.Content().withId("35010-Politiken-dm2"));
        assertThat(jsonbContext.marshall(config), not(containsString("retrieverSourceId")));
    }

    @Test
    public void marshalling() throws JSONBException {
        final RetrieverHarvesterConfig config = new RetrieverHarvesterConfig(1, 2,
                new RetrieverHarvesterConfig.Content()
                        .withId("-id-")
                        .withSchedule("* * * * *")
                        .withDescription("-description-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withEnabled(true)
                        .withTimeOfLastHarvest(new Date())
        );
        final String configAsString = jsonbContext.marshall(config);

        final RetrieverHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, RetrieverHarvesterConfig.class);
        assertThat(configFromString, is(config));
    }
}

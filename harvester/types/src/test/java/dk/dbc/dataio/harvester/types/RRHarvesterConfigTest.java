package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jsonb.JSONBContext;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * Created by ja7 on 17-05-16.
 */
public class RRHarvesterConfigTest {

    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaultJsonEncodeDecode() throws Exception {
        RRHarvesterConfig rrHarvesterConfig=new RRHarvesterConfig(1,2,new RRHarvesterConfig.Content());
        String rrHarvestrConfigAsString=jsonbContext.marshall( rrHarvesterConfig );

        RRHarvesterConfig rrFromString=jsonbContext.unmarshall( rrHarvestrConfigAsString, RRHarvesterConfig.class);
        assertThat( rrFromString, is( rrHarvesterConfig ));
    }


    @Test
    public void complexEncodeDecode() throws Exception {

        RRHarvesterConfig rrHarvesterConfig=new RRHarvesterConfig(1,2,
                new RRHarvesterConfig.Content()
                        .wihtFormat("format")
                        .withBatchSize(12)
                .withConsumerId("ConsumerId")
                .withDestination("Destination")
                .withIncludeRelations(false)
                .withOpenAgencyTarget(new OpenAgencyTarget())
                .withResource("Resource")
                .withType(JobSpecification.Type.ACCTEST)
                .withFormatOverridesEntry(12,"overwride 12")
                .withFormatOverridesEntry(191919, "must be 870970")
        );
        String rrHarvestrConfigAsString=jsonbContext.marshall( rrHarvesterConfig );

        RRHarvesterConfig rrFromString=jsonbContext.unmarshall( rrHarvestrConfigAsString, RRHarvesterConfig.class);
        assertThat( rrFromString, is( rrHarvesterConfig ));

        rrFromString=jsonbContext.unmarshall("{\"type\":\"dk.dbc.dataio.harvester.types.RRHarvesterConfig\",\"id\":1,\"version\":2,\"content\":{\"resource\":\"Resource\",\"consumerId\":\"ConsumerId\",\"destination\":\"Destination\",\"type\":\"ACCTEST\",\"format\":\"format\",\"batchSize\":12,\"openAgencyTarget\":{\"url\":null,\"group\":null,\"user\":null,\"password\":null},\"formatOverrides\":{\"12\":\"overwride 12\",\"191919\":\"must be 870970\"},\"includeRelations\":false}}", RRHarvesterConfig.class);

        assertThat( rrFromString, is(rrHarvesterConfig));
    }
}
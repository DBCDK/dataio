package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
                        .withFormat("format")
                        .withBatchSize(12)
                .withConsumerId("ConsumerId")
                .withDestination("Destination")
                .withIncludeRelations(false)
                .withOpenAgencyTarget(new OpenAgencyTarget())
                .withResource("Resource")
                .withType(JobSpecification.Type.ACCTEST)
                .withFormatOverridesEntry(12,"overwride 12")
                .withFormatOverridesEntry(191919, "must be 870970")
                .withId("harvest log id")
                .withEnabled( true )
        );
        String rrHarvestrConfigAsString=jsonbContext.marshall( rrHarvesterConfig );

        RRHarvesterConfig rrFromString=jsonbContext.unmarshall( rrHarvestrConfigAsString, RRHarvesterConfig.class);
        assertThat( rrFromString, is( rrHarvesterConfig ));

        rrFromString=jsonbContext.unmarshall("{\"type\":\"dk.dbc.dataio.harvester.types.RRHarvesterConfig\",\"id\":1,\"version\":2,\"content\":{\"resource\":\"Resource\",\"consumerId\":\"ConsumerId\",\"destination\":\"Destination\",\"type\":\"ACCTEST\",\"format\":\"format\",\"batchSize\":12,\"enabled\": true,\"openAgencyTarget\":{\"url\":null,\"group\":null,\"user\":null,\"password\":null},\"formatOverrides\":{\"12\":\"overwride 12\",\"191919\":\"must be 870970\"},\"includeRelations\":false, \"id\": \"harvest log id\"}}", RRHarvesterConfig.class);

        assertThat( rrFromString, is(rrHarvesterConfig));
    }

    @Test
    public void decodeTestFromLiveData() throws Exception {
        RRHarvesterConfig rrHarvesterConfig=jsonbContext.unmarshall("{\"id\": 1, \"type\": \"dk.dbc.dataio.harvester.types.RRHarvesterConfig\", \"content\": {\"id\": \"broend-sync\",\"enabled\": false,\"type\": \"ACCTEST\", \"format\": \"katalog\", \"resource\": \"jdbc/dataio/rawrepo\", \"batchSize\": 10000, \"consumerId\": \"broend-sync\", \"destination\": \"testbroend-i01\", \"formatOverrides\": {\"870970\": \"basis\"}, \"includeRelations\": true, \"openAgencyTarget\": {\"url\": \"http://openagency.addi.dk/2.25/\"}}, \"version\": 1}", RRHarvesterConfig.class);
        assertThat( rrHarvesterConfig.getContent().isEnabled(), is(false));
    }
}
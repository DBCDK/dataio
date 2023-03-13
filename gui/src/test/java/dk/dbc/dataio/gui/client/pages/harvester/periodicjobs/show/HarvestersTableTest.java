package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class HarvestersTableTest {
    @Mock
    Presenter presenter;
    @Mock
    Texts texts;

    private dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show.HarvestersTable harvestersTable;

    @Before
    public void createHarvestersTable() {
        harvestersTable = new HarvestersTable();
        harvestersTable.presenter = presenter;
        harvestersTable.texts = texts;
    }

    @Before
    public void setupExpectations() {
        when(texts.value_Enabled()).thenReturn("enabled");
        when(texts.value_Disabled()).thenReturn("disabled");
        when(texts.columnValue_HarvesterType_STANDARD()).thenReturn("std");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void columns() {
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withHarvesterType(PeriodicJobsHarvesterConfig.HarvesterType.STANDARD)
                        .withName("-periodic-job-")
                        .withSchedule("* * * * *")
                        .withDescription("-description-")
                        .withResource("jdbc/dataio/rawrepo-cisterne")
                        .withCollection("-collection-")
                        .withHoldingsSolrUrl("-holdings-solr-url-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withSubmitterNumber("123456")
                        .withEnabled(true)
                        .withTimeOfLastHarvest(new Date())
                        .withEnabled(true));

        assertThat("Name column", harvestersTable.getColumn(0).getValue(config),
                is(config.getContent().getName()));
        assertThat("Schedule column", harvestersTable.getColumn(1).getValue(config),
                is(config.getContent().getSchedule()));
        assertThat("Description column", harvestersTable.getColumn(2).getValue(config),
                is(config.getContent().getDescription()));
        assertThat("Resource column", harvestersTable.getColumn(3).getValue(config),
                is("cisterne"));
        assertThat("Collection column", harvestersTable.getColumn(4).getValue(config),
                is(config.getContent().getCollection()));
        assertThat("HoldingsSolrUrl column", harvestersTable.getColumn(5).getValue(config),
                is(config.getContent().getHoldingsSolrUrl()));
        assertThat("Destination column", harvestersTable.getColumn(6).getValue(config),
                is(config.getContent().getDestination()));
        assertThat("Format column", harvestersTable.getColumn(7).getValue(config),
                is(config.getContent().getFormat()));
        assertThat("Submitter column", harvestersTable.getColumn(8).getValue(config),
                is(config.getContent().getSubmitterNumber()));
        assertThat("HarvesterType column", harvestersTable.getColumn(9).getValue(config),
                is("std"));
        assertThat("TimeOfLastHarvest column", harvestersTable.getColumn(10).getValue(config),
                is(Format.formatLongDate(config.getContent().getTimeOfLastHarvest())));
        assertThat("Enabled column", harvestersTable.getColumn(11).getValue(config),
                is(texts.value_Enabled()));

        assertThat("Number of columns tested", harvestersTable.getColumnCount(), is(13));
    }
}

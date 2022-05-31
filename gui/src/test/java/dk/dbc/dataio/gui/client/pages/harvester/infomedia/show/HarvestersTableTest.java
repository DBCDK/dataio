package dk.dbc.dataio.gui.client.pages.harvester.infomedia.show;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
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

    private HarvestersTable harvestersTable;

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
    }

    @Test
    @SuppressWarnings("unchecked")
    public void columns() {
        final Date nextPublicationDate = new Date(42L);

        final InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(1, 1,
                new InfomediaHarvesterConfig.Content()
                        .withId("One")
                        .withSchedule("* * * * *")
                        .withDescription("Number one")
                        .withDestination("somewhere")
                        .withFormat("infomedia")
                        .withEnabled(true)
                        .withTimeOfLastHarvest(new Date())
                        .withNextPublicationDate(nextPublicationDate)
                        .withEnabled(true));

        assertThat("ID column", harvestersTable.getColumn(0).getValue(config),
                is(config.getContent().getId()));
        assertThat("Schedule column", harvestersTable.getColumn(1).getValue(config),
                is(config.getContent().getSchedule()));
        assertThat("Description column", harvestersTable.getColumn(2).getValue(config),
                is(config.getContent().getDescription()));
        assertThat("Destination column", harvestersTable.getColumn(3).getValue(config),
                is(config.getContent().getDestination()));
        assertThat("Format column", harvestersTable.getColumn(4).getValue(config),
                is(config.getContent().getFormat()));
        assertThat("TimeOfLastHarvest column", harvestersTable.getColumn(5).getValue(config),
                is(Format.formatLongDate(config.getContent().getTimeOfLastHarvest())));
        assertThat("NextPublicationDate column", harvestersTable.getColumn(6).getValue(config),
                is(Format.formatLongDate(nextPublicationDate)));
        assertThat("Enabled column", harvestersTable.getColumn(7).getValue(config),
                is(texts.value_Enabled()));
        assertThat("Enabled column", harvestersTable.getColumn(8).getValue(config),
                is(texts.button_Edit()));

        assertThat("Number of columns tested", harvestersTable.getColumnCount(), is(9));
    }
}

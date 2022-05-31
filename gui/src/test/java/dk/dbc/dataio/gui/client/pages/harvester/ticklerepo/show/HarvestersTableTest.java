package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test of HarvestersTable
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class HarvestersTableTest {

    @Mock
    Presenter mockedPresenter;
    @Mock
    ListDataProvider<TickleRepoHarvesterConfig> mockedDataProvider;
    @Mock
    List<TickleRepoHarvesterConfig> mockedHarvesterList;
    @Mock
    Texts mockedTexts;
    @Mock
    DoubleClickEvent mockedDoubleClickEvent;
    @Mock
    SingleSelectionModel<TickleRepoHarvesterConfig> mockedSelectionModel;

    // Test Data
    private List<TickleRepoHarvesterConfig> testHarvesterConfig = new ArrayList<>();
    private TickleRepoHarvesterConfig testHarvesterConfigEntry1 = new TickleRepoHarvesterConfig(1, 2, new TickleRepoHarvesterConfig.Content()
            .withId("ID1")
            .withDatasetName("DatasetName1")
            .withDescription("Description1")
            .withDestination("Destination1")
            .withFormat("Format1")
            .withType(JobSpecification.Type.TRANSIENT)
            .withEnabled(false)
    );
    private TickleRepoHarvesterConfig testHarvesterConfigEntry2 = new TickleRepoHarvesterConfig(2, 3, new TickleRepoHarvesterConfig.Content().withId("ID2"));

    @Before
    public void setupTestHarvesterConfig() {
        testHarvesterConfig.add(testHarvesterConfigEntry2);
        testHarvesterConfig.add(testHarvesterConfigEntry1);
    }

    @Before
    public void setupTexts() {
        when(mockedTexts.value_Enabled()).thenReturn("enabled");
        when(mockedTexts.value_Disabled()).thenReturn("disabled");
        when(mockedTexts.button_Edit()).thenReturn("editButton");
    }


    // Subject Under Test
    private HarvestersTable harvestersTable;


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        harvestersTable = new HarvestersTable();

        // Verify Test
        assertThat(harvestersTable.getRowCount(), is(0));
    }

    @Test(expected = NullPointerException.class)
    public void setHarvesters_nullData_exception() {
        // Test Preparation
        harvestersTable = new HarvestersTable();

        // Subject Under Test
        harvestersTable.setHarvesters(mockedPresenter, null);
    }

    @Test
    public void setHarvesters_empty_dataOk() {
        // Test Preparation
        harvestersTable = new HarvestersTable();
        harvestersTable.dataProvider = mockedDataProvider;
        when(mockedDataProvider.getList()).thenReturn(mockedHarvesterList);

        // Subject Under Test
        harvestersTable.setHarvesters(mockedPresenter, testHarvesterConfig);

        // Verify Test
        verify(mockedDataProvider, times(4)).getList();
        verifyNoMoreInteractions(mockedDataProvider);
        verify(mockedHarvesterList).clear();
        verify(mockedHarvesterList).add(testHarvesterConfigEntry1);
        verify(mockedHarvesterList).add(testHarvesterConfigEntry2);
    }

    @Test
    public void constructor_data_checkGetValueCallbacks() {
        // Subject Under Test
        harvestersTable = new HarvestersTable();
        harvestersTable.texts = mockedTexts;

        // Verify Test
        assertThat(harvestersTable.getColumnCount(), is(10));
        int i = 0;
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("ID1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("DatasetName1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("Description1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("Destination1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("Format1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("TRANSIENT"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("na, 0"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("disabled"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("disabled"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("editButton"));
    }

    @Test
    public void getDoubleClickHandler__ok() {
        // Test preparation
        harvestersTable = new HarvestersTable();
        harvestersTable.presenter = mockedPresenter;
        harvestersTable.setSelectionModel(mockedSelectionModel);
        harvestersTable.selectionModel = mockedSelectionModel;
        when(mockedSelectionModel.getSelectedObject()).thenReturn(testHarvesterConfigEntry1);
        DoubleClickHandler handler = harvestersTable.getDoubleClickHandler();

        // Subject Under Test
        handler.onDoubleClick(mockedDoubleClickEvent);

        // Verify Test
        verify(mockedSelectionModel).getSelectedObject();
        verify(mockedPresenter).editTickleRepoHarvesterConfig("1");
    }
}

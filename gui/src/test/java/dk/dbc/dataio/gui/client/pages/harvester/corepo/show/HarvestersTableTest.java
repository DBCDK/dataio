package dk.dbc.dataio.gui.client.pages.harvester.corepo.show;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
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
    ListDataProvider<CoRepoHarvesterConfig> mockedDataProvider;
    @Mock
    List<CoRepoHarvesterConfig> mockedHarvesterList;
    @Mock
    Texts mockedTexts;
    @Mock
    DoubleClickEvent mockedDoubleClickEvent;
    @Mock
    SingleSelectionModel<CoRepoHarvesterConfig> mockedSelectionModel;
    @Mock
    View mockedView;

    // Test Data
    private List<CoRepoHarvesterConfig> testHarvesterConfig = new ArrayList<>();
    private CoRepoHarvesterConfig testHarvesterConfigEntry1 = new CoRepoHarvesterConfig(1, 1,
            new CoRepoHarvesterConfig.Content()
                    .withName("nami1")
                    .withDescription("descri1")
                    .withResource("resi")
                    .withTimeOfLastHarvest(new Date(7654))
                    .withEnabled(true)
                    .withRrHarvester(234)
    );
    private CoRepoHarvesterConfig testHarvesterConfigEntry2 = new CoRepoHarvesterConfig(2, 2, new CoRepoHarvesterConfig.Content().withName("nami2"));

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

    @After
    public void commonVerification() {
        verifyNoMoreInteractions(mockedPresenter);
        verifyNoMoreInteractions(mockedDataProvider);
        verifyNoMoreInteractions(mockedHarvesterList);
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedDoubleClickEvent);
        verifyNoMoreInteractions(mockedSelectionModel);
        verifyNoMoreInteractions(mockedView);
    }


    // Subject Under Test
    private HarvestersTable harvestersTable;


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        harvestersTable = new HarvestersTable(mockedView);

        // Verify Test
        assertThat(harvestersTable.getRowCount(), is(0));
    }

    @Test(expected = NullPointerException.class)
    public void setHarvesters_nullData_exception() {
        // Test Preparation
        harvestersTable = new HarvestersTable(mockedView);

        // Subject Under Test
        harvestersTable.setHarvesters(mockedPresenter, null);
    }

    @Test
    public void setHarvesters_empty_dataOk() {
        // Test Preparation
        harvestersTable = new HarvestersTable(mockedView);
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
        verify(mockedHarvesterList).sort(any());
    }

    @Test
    public void constructor_data_checkGetValueCallbacks() {
        // Subject Under Test
        harvestersTable = new HarvestersTable(mockedView);
        harvestersTable.texts = mockedTexts;
        HarvestersTable.FetchAvailableRRHarvesterConfigsCallback callback = harvestersTable.new FetchAvailableRRHarvesterConfigsCallback();
        List<RRHarvesterConfig> rrConfigs = new ArrayList<>();
        rrConfigs.add(new RRHarvesterConfig(233L, 1L, new RRHarvesterConfig.Content().withId("RR1")));
        rrConfigs.add(new RRHarvesterConfig(234L, 2L, new RRHarvesterConfig.Content().withId("RR2")));
        rrConfigs.add(new RRHarvesterConfig(235L, 3L, new RRHarvesterConfig.Content().withId("RR3")));
        callback.onSuccess(rrConfigs);


        // Verify Test
        assertThat(harvestersTable.getColumnCount(), is(7));
        int i = 0;
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("nami1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("descri1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("resi"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("RR2"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("1970-01-01 01:00:07"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("enabled"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("editButton"));
        verify(mockedTexts).value_Enabled();
        verify(mockedTexts).button_Edit();
    }

    @Test
    public void onDoubleClick_editCoRepoConfig_ok() {
        // Test preparation
        harvestersTable = new HarvestersTable(mockedView);
        harvestersTable.presenter = mockedPresenter;
        harvestersTable.setSelectionModel(mockedSelectionModel);
        harvestersTable.selectionModel = mockedSelectionModel;
        when(mockedSelectionModel.getSelectedObject()).thenReturn(testHarvesterConfigEntry1);
        DoubleClickHandler handler = harvestersTable.getDoubleClickHandler();

        // Subject Under Test
        handler.onDoubleClick(mockedDoubleClickEvent);

        // Verify Test
        verify(mockedSelectionModel).addSelectionChangeHandler(any());
        verify(mockedSelectionModel).getSelectedObject();
        verify(mockedPresenter).editCoRepoHarvesterConfig("1");
    }
}

package dk.dbc.dataio.gui.client.pages.sink.status;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test of SinkStatusTable
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class SinkStatusTableTest {

    @Mock
    Presenter mockedPresenter;
    @Mock
    ListDataProvider<SinkStatusTable.SinkStatusModel> mockedDataProvider;
    @Mock
    List<SinkStatusTable.SinkStatusModel> mockedSinkStatusList;
    @Mock
    Texts mockedTexts;
    @Mock
    DoubleClickEvent mockedDoubleClickEvent;
    @Mock
    SingleSelectionModel<SinkStatusTable.SinkStatusModel> mockedSelectionModel;

    // Test Data
    private List<SinkStatusTable.SinkStatusModel> testData = Arrays.asList(
            new SinkStatusTable.SinkStatusModel(54, "Dummy sink", "Dummy sink", 0, 0, null),
            new SinkStatusTable.SinkStatusModel(6601, "Dummy sink", "Tracer bullit sink", 0, 0, null),
            new SinkStatusTable.SinkStatusModel(1551, "ES sink", "Basis22", 2, 4, null),
            new SinkStatusTable.SinkStatusModel(5701, "ES sink", "Danbib3", 0, 0, null),
            new SinkStatusTable.SinkStatusModel(752, "Hive sink", "Cisterne sink", 34, 56, null),
            new SinkStatusTable.SinkStatusModel(8, "Hive sink", "Boblebad sink", 32, 54, null),
            new SinkStatusTable.SinkStatusModel(1651, "Update sink", "Cisterne Update sink", 1, 56023, null),
            new SinkStatusTable.SinkStatusModel(5401, "IMS sink", "IMS cisterne sink", 7, 8, null)
    );
    private SinkStatusTable.SinkStatusModel testSinkStatus = new SinkStatusTable.SinkStatusModel(3333, "Test sink", "Sink name", 111, 222, null);


    // Subject Under Test
    private SinkStatusTable sinkStatusTable;


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        sinkStatusTable = new SinkStatusTable();

        // Verify Test
        assertThat(sinkStatusTable.getRowCount(), is(0));
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void setSinkStatusData_null_noDataOk() {
        // Test Preparation
        sinkStatusTable = new SinkStatusTable();
        sinkStatusTable.dataProvider = mockedDataProvider;
        when(mockedDataProvider.getList()).thenReturn(mockedSinkStatusList);

        // Subject Under Test
        sinkStatusTable.setSinkStatusData(mockedPresenter, null);

        // Verify Test
        verify(mockedDataProvider).getList();
        verify(mockedSinkStatusList).clear();
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void setSinkStatusData_empty_noDataOk() {
        // Test Preparation
        sinkStatusTable = new SinkStatusTable();
        sinkStatusTable.dataProvider = mockedDataProvider;
        when(mockedDataProvider.getList()).thenReturn(mockedSinkStatusList);

        // Subject Under Test
        sinkStatusTable.setSinkStatusData(mockedPresenter, new ArrayList<>());

        // Verify Test
        verify(mockedDataProvider).getList();
        verify(mockedSinkStatusList).clear();
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void setSinkStatusData_validData_dataOk() {
        // Test Preparation
        sinkStatusTable = new SinkStatusTable();
        sinkStatusTable.dataProvider = mockedDataProvider;
        when(mockedDataProvider.getList()).thenReturn(mockedSinkStatusList);

        // Subject Under Test
        sinkStatusTable.setSinkStatusData(mockedPresenter, testData);

        // Verify Test
        verify(mockedDataProvider).getList();
        verify(mockedSinkStatusList).clear();
        verify(mockedSinkStatusList, times(8)).add(any(SinkStatusTable.SinkStatusModel.class));
        verifyNoMoreInteractionsOnMocks();
    }

    @Test
    public void constructor_data_checkGetValueCallbacks() {
        // Subject Under Test
        sinkStatusTable = new SinkStatusTable();
        sinkStatusTable.texts = mockedTexts;

        // Verify Test
        assertThat(sinkStatusTable.getColumnCount(), is(5));
        int i = 0;
        assertThat(sinkStatusTable.getColumn(i++).getValue(testSinkStatus), is("Test sink"));
        assertThat(sinkStatusTable.getColumn(i++).getValue(testSinkStatus), is("Sink name"));
        assertThat(sinkStatusTable.getColumn(i++).getValue(testSinkStatus), is("111"));
        assertThat(sinkStatusTable.getColumn(i++).getValue(testSinkStatus), is("222"));
        assertThat(sinkStatusTable.getColumn(i++).getValue(testSinkStatus), is("NA"));
    }


    private void verifyNoMoreInteractionsOnMocks() {
        verifyNoMoreInteractions(mockedPresenter);
        verifyNoMoreInteractions(mockedDataProvider);
        verifyNoMoreInteractions(mockedSinkStatusList);
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedDoubleClickEvent);
        verifyNoMoreInteractions(mockedSelectionModel);

    }
}

package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test of GatekeepersTable
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class GatekeepersTableTest {

    @Mock
    ListDataProvider<GatekeeperDestination> mockedDataProvider;
    @Mock
    List<GatekeeperDestination> mockedGatekeeperList;
    @Mock
    Texts mockedTexts;
    @Mock
    View mockedView;
    @Mock
    Column mockedColumn;

    @Before
    public void setupTexts() {
        when(mockedTexts.button_Delete()).thenReturn("ButtonDelete");
    }


    @Test
    public void constructor_noData_emptyOk() {
        // Subject under test
        GatekeepersTable gatekeepersTable = new GatekeepersTable(mockedView);

        // Verify Test
        assertThat(gatekeepersTable.getRowCount(), is(0));
    }

    @Test
    public void constructor_data_dataOk() {
        // Prepare test
        GatekeepersTable gatekeepersTable = new GatekeepersTable(mockedView);
        gatekeepersTable.dataProvider = mockedDataProvider;
        when(mockedDataProvider.getList()).thenReturn(mockedGatekeeperList);
        List<GatekeeperDestination> gatekeeperDestinationList = new ArrayList<>();
        gatekeeperDestinationList.add(new GatekeeperDestinationBuilder().build());
        gatekeeperDestinationList.add(new GatekeeperDestinationBuilder().build());

        // Subject under test
        gatekeepersTable.setGatekeepers(gatekeeperDestinationList);

        // Verify Test
        verify(mockedDataProvider, times(2)).getList();
        verify(mockedGatekeeperList).clear();
        verify(mockedGatekeeperList).addAll(gatekeeperDestinationList);
        verifyNoMoreInteractions(mockedDataProvider);
        verifyNoMoreInteractions(mockedGatekeeperList);
    }

    @Test
    public void constructor_data_checkGetValueCallbacks() {
        // Prepare test
        GatekeeperDestination gatekeeper = new GatekeeperDestinationBuilder().
                setSubmitterNumber("11").
                setDestination("de").
                setPackaging("pa").
                setFormat("fo")
                .build();

        // Subject Under Test
        GatekeepersTable gatekeepersTable = new GatekeepersTable(mockedView);
        gatekeepersTable.texts = mockedTexts;

        // Verify Test
        assertThat(gatekeepersTable.getColumnCount(), is(5));
        assertThat(gatekeepersTable.getColumn(0).getValue(gatekeeper), is("11"));
        assertThat(gatekeepersTable.getColumn(1).getValue(gatekeeper), is("pa"));
        assertThat(gatekeepersTable.getColumn(2).getValue(gatekeeper), is("fo"));
        assertThat(gatekeepersTable.getColumn(3).getValue(gatekeeper), is("de"));
        assertThat(gatekeepersTable.getColumn(4).getValue(gatekeeper), is("ButtonDelete"));
    }

    @Test
    public void constructSubmitterSortHandler_callMethod_sortIsOk() {
        // Prepare test
        GatekeepersTable gatekeepersTable = new GatekeepersTable(mockedView);
        GatekeeperDestination d1 = new GatekeeperDestinationBuilder().setSubmitterNumber("1").build();
        GatekeeperDestination d2 = new GatekeeperDestinationBuilder().setSubmitterNumber("2").build();

        // Subject Under Test
        ColumnSortEvent.ListHandler handler = gatekeepersTable.constructSubmitterSortHandler(mockedColumn);

        // Verify Test
        Comparator comparator = handler.getComparator(mockedColumn);

        assertThat(comparator.compare(d1, d1), is(0));
        assertThat(comparator.compare(d1, d2), is(-1));
        assertThat(comparator.compare(d2, d1), is(1));

        verify(mockedColumn).setSortable(true);
        verify(mockedColumn).isDefaultSortAscending();
        verifyNoMoreInteractions(mockedColumn);
    }

    @Test
    public void constructCopySortHandler_callMethod_sortIsOk() {
        // Prepare test
        GatekeepersTable gatekeepersTable = new GatekeepersTable(mockedView);

        // Subject Under Test
        ColumnSortEvent.ListHandler handler = gatekeepersTable.constructCopySortHandler(mockedColumn);

        // Verify Test
        Comparator comparator = handler.getComparator(mockedColumn);

        verify(mockedColumn).setSortable(true);
        verifyNoMoreInteractions(mockedColumn);
    }

}

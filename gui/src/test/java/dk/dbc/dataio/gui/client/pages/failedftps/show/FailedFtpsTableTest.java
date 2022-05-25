package dk.dbc.dataio.gui.client.pages.failedftps.show;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.Notification;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


/**
 * FailedFtpsTableunit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class FailedFtpsTableTest {

    @Mock
    ListDataProvider<Notification> mockedDataProvider;
    @Mock
    List<Notification> mockedNotificationList;
    @Mock
    Texts mockedTexts;
    @Mock
    View mockedView;
    @Mock
    Presenter mockedPresenter;
    @Mock
    Column mockedColumn;
    @Mock
    SingleSelectionModel mockedSelectionModel;


    @Before
    public void setupTexts() {
        when(mockedTexts.label_PageTitle()).thenReturn("PageTitle");
        when(mockedTexts.label_HeaderDate()).thenReturn("HeaderDate");
        when(mockedTexts.label_HeaderTransFile()).thenReturn("HeaderTransFile");
        when(mockedTexts.label_HeaderMail()).thenReturn("HeaderMail");
        when(mockedTexts.error_CannotFetchNotifications()).thenReturn("CannotFetchNotifications");
    }

    @Test
    public void constructor_noData_emptyOk() {
        // Subject under test
        FailedFtpsTable failedFtpsTable = new FailedFtpsTable(mockedView);

        // Verify Test
        assertThat(failedFtpsTable.getRowCount(), is(0));
    }

    @Test
    public void constructor_data_dataOk() {
        // Prepare test
        FailedFtpsTable failedFtpsTable = new FailedFtpsTable(mockedView);
        failedFtpsTable.dataProvider = mockedDataProvider;
        when(mockedDataProvider.getList()).thenReturn(mockedNotificationList);
        List<Notification> testNotifications = new ArrayList<>();
        testNotifications.add(new Notification().withContent("Content 1").withId(11).withJobId(111));
        testNotifications.add(new Notification().withContent("Content 2").withId(22).withJobId(222));

        // Subject under test
        failedFtpsTable.setNotifications(testNotifications);

        // Verify Test
        verify(mockedDataProvider, times(2)).getList();
        verify(mockedNotificationList).clear();
        verify(mockedNotificationList).addAll(testNotifications);
        verifyNoMoreInteractions(mockedDataProvider);
        verifyNoMoreInteractions(mockedNotificationList);
    }

    @Test
    public void constructor_data_checkGetValueCallbacks() {
        // Prepare test
        FailedFtpsTable failedFtpsTable = new FailedFtpsTable(mockedView);
        failedFtpsTable.texts = mockedTexts;


        InvalidTransfileNotificationContext testNotificationContext =
                new InvalidTransfileNotificationContext("TransFileNameX", "TransFileContentX", "CauseX");
        Notification testNotification =
                new Notification()
                        .withTimeOfCreation(new Date(1234567890123L))
                        .withStatus(Notification.Status.COMPLETED)
                        .withContext(testNotificationContext);

        // Subject Under Test
        assertThat(failedFtpsTable.getColumnCount(), is(3));
        assertThat(failedFtpsTable.getColumn(0).getValue(testNotification), is("2009-02-14 00:31:30"));
        assertThat(failedFtpsTable.getColumn(1).getValue(testNotification), is("TransFileNameX"));
        assertThat(failedFtpsTable.getColumn(2).getValue(testNotification), is("Completed"));
    }

}

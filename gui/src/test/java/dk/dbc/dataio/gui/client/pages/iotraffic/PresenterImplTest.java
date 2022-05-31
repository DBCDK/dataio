package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
import dk.dbc.dataio.gui.client.components.EnterButton;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {

    @Mock
    View mockedView;
    @Mock
    Texts mockedTexts;
    @Mock
    Widget mockedViewWidget;
    @Mock
    PromptedTextBox mockedSubmitter;
    @Mock
    PromptedTextBox mockedPackaging;
    @Mock
    PromptedTextBox mockedFormat;
    @Mock
    PromptedTextBox mockedDestination;
    @Mock
    EnterButton mockedAddButton;
    @Mock
    GatekeepersTable mockedGatekeepersTable;
    @Mock
    Throwable mockedThrowable;


    // Setup data
    @Before
    public void setupData() {
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        mockedView.submitter = mockedSubmitter;
        mockedView.packaging = mockedPackaging;
        mockedView.format = mockedFormat;
        mockedView.destination = mockedDestination;
        mockedView.addButton = mockedAddButton;
        mockedView.gatekeepersTable = mockedGatekeepersTable;
    }


    // Subject Under Test
    private PresenterImpl presenterImpl;


    @Test
    public void start_callStart_verifyAllCalls() {
        // Prepare test
        presenterImpl = setupPresenter();

        // Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedView).asWidget();
        verify(mockedView).setHeader(header);
        verify(mockedView).setPresenter(presenterImpl);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedView);
        verifyInitializeDataMethodCall();
    }

    @Test(expected = NullPointerException.class)
    public void addButtonPressed_submitterNull_exception() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.submitter = null;

        // Subject Under Test
        presenterImpl.addButtonPressed();
    }

    @Test
    public void addButtonPressed_submitterEmpty_displayWarning() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.submitter = "";

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verify Test
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).displayWarning(isNull());
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test(expected = NullPointerException.class)
    public void addButtonPressed_packagingNull_exception() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.packaging = null;

        // Subject Under Test
        presenterImpl.addButtonPressed();
    }

    @Test
    public void addButtonPressed_packagingEmpty_displayWarning() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.packaging = "";

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verify Test
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).displayWarning(isNull());
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test(expected = NullPointerException.class)
    public void addButtonPressed_formatNull_exception() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.format = null;

        // Subject Under Test
        presenterImpl.addButtonPressed();
    }

    @Test
    public void addButtonPressed_formatEmpty_displayWarning() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.format = "";

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verify Test
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).displayWarning(isNull());
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test(expected = NullPointerException.class)
    public void addButtonPressed_destinationNull_exception() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.destination = null;

        // Subject Under Test
        presenterImpl.addButtonPressed();
    }

    @Test
    public void addButtonPressed_destinationEmpty_displayWarning() {
        // Prepare Test
        presenterImpl = setupPresenter();
        presenterImpl.destination = "";

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verify Test
        verify(mockedTexts).error_InputFieldValidationError();
        verify(mockedView).displayWarning(isNull());
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void addButtonPressed_noEmptyData_callFlowStore() {
        // Prepare Test
        presenterImpl = setupPresenter();

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verify Test
        verify(presenterImpl.flowStoreProxy).createGatekeeperDestination(
                any(GatekeeperDestination.class),
                any(PresenterImpl.CreateGatekeeperDestinationCallback.class)
        );
        verifyNoMoreInteractions(presenterImpl.flowStoreProxy);
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void deleteButtonPressed_validData_callFlowStore() {
        // Prepare Test
        presenterImpl = setupPresenter();

        // Subject Under Test
        presenterImpl.deleteButtonPressed(123L);

        // Verify Test
        verify(presenterImpl.flowStoreProxy).deleteGatekeeperDestination(
                eq(123L),
                any(PresenterImpl.DeleteGatekeeperDestinationCallback.class)
        );
        verifyNoMoreInteractions(presenterImpl.flowStoreProxy);
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void createGatekeeperDestinationCallback_callOnFailure_displayWarning() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        PresenterImpl.CreateGatekeeperDestinationCallback callback = presenterImpl.new CreateGatekeeperDestinationCallback();
        when(presenterImpl.viewInjector.getView()).thenReturn(mockedView);
        when(presenterImpl.viewInjector.getTexts()).thenReturn(mockedTexts);

        // Subject Under Test
        callback.onFailure(mockedThrowable);

        // Verify Test
        verify(mockedTexts).error_CannotCreateGatekeeperDestination();
        verify(mockedView).displayWarning(isNull());
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void createGatekeeperDestinationCallback_callOnSucces_initializeData() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        PresenterImpl.CreateGatekeeperDestinationCallback callback = presenterImpl.new CreateGatekeeperDestinationCallback();
        when(presenterImpl.viewInjector.getView()).thenReturn(mockedView);
        when(presenterImpl.viewInjector.getTexts()).thenReturn(mockedTexts);

        // Subject Under Test
        callback.onSuccess(new GatekeeperDestinationBuilder().build());

        // Verify Test
        verifyInitializeDataMethodCall();
    }

    @Test
    public void findAllGateKeeperDestinationsCallback_callOnFailure_displayWarning() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        PresenterImpl.FindAllGateKeeperDestinationsCallback callback = presenterImpl.new FindAllGateKeeperDestinationsCallback();
        when(presenterImpl.viewInjector.getView()).thenReturn(mockedView);
        when(presenterImpl.viewInjector.getTexts()).thenReturn(mockedTexts);

        // Subject Under Test
        callback.onFailure(mockedThrowable);

        // Verify Test
        verify(mockedTexts).error_CannotFetchGatekeeperDestinations();
        verify(mockedView).displayWarning(isNull());
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void findAllGateKeeperDestinationsCallback_callOnSucces_initializeData() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        PresenterImpl.FindAllGateKeeperDestinationsCallback callback = presenterImpl.new FindAllGateKeeperDestinationsCallback();
        when(presenterImpl.viewInjector.getView()).thenReturn(mockedView);
        when(presenterImpl.viewInjector.getTexts()).thenReturn(mockedTexts);
        List<GatekeeperDestination> gatekeeperDestinations = Collections.singletonList(new GatekeeperDestinationBuilder().build());

        // Subject Under Test
        callback.onSuccess(gatekeeperDestinations);

        // Verify Test
        verify(mockedView).setGatekeepers(gatekeeperDestinations);
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void deleteGatekeeperDestinationCallback_callOnFailure_displayWarning() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        PresenterImpl.DeleteGatekeeperDestinationCallback callback = presenterImpl.new DeleteGatekeeperDestinationCallback();
        when(presenterImpl.viewInjector.getView()).thenReturn(mockedView);
        when(presenterImpl.viewInjector.getTexts()).thenReturn(mockedTexts);

        // Subject Under Test
        callback.onFailure(mockedThrowable);

        // Verify Test
        verify(mockedTexts).error_CannotDeleteGatekeeperDestination();
        verify(mockedView).displayWarning(isNull());
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void deleteGatekeeperDestinationCallback_callOnSucces_initializeData() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        PresenterImpl.DeleteGatekeeperDestinationCallback callback = presenterImpl.new DeleteGatekeeperDestinationCallback();
        when(presenterImpl.viewInjector.getView()).thenReturn(mockedView);
        when(presenterImpl.viewInjector.getTexts()).thenReturn(mockedTexts);

        // Subject Under Test
        callback.onSuccess(null);

        // Verify Test
        verifyInitializeDataMethodCall();
    }

    @Test(expected = NullPointerException.class)
    public void doSort_nullList_exception() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");

        // Subject Under Test
        presenterImpl.doSort(null);
    }

    @Test
    public void doSort_emptyList_noSortNoException() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");

        // Subject Under Test
        List result = presenterImpl.doSort(new ArrayList<>());

        // Verify Test
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void doSort_oneEntryList_ok() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        List<GatekeeperDestination> inputList = new ArrayList<>();
        inputList.add(new GatekeeperDestinationBuilder().setId(111).build());

        // Subject Under Test
        List<GatekeeperDestination> result = presenterImpl.doSort(inputList);

        // Verify Test
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getId(), is(111L));
    }

    @Test
    public void doSort_nineEntriesList_ok() {
        // Prepare Test
        presenterImpl = new PresenterImpl("Header Text");
        List<GatekeeperDestination> inputList = new ArrayList<>();
        /*
           Setup a matrix to be sorted like this:
           Submitter:   Packaging:  Format:
           2            aaa         hhh
           2            aaa         iii
           2            aaa         jjj
           10           bbb         iii
           10           bbb         jjj
           10           ccc         hhh
           30           ddd         jjj
           30           eee         iii
           30           fff         hhh
         */
        inputList.add(new GatekeeperDestinationBuilder().setSubmitterNumber("30").setPackaging("fff").setFormat("hhh").build()); // Position 8
        inputList.add(new GatekeeperDestinationBuilder().setSubmitterNumber("30").setPackaging("eee").setFormat("iii").build()); // Position 7
        inputList.add(new GatekeeperDestinationBuilder().setSubmitterNumber("30").setPackaging("ddd").setFormat("jjj").build()); // Position 6
        inputList.add(new GatekeeperDestinationBuilder().setSubmitterNumber("10").setPackaging("ccc").setFormat("hhh").build()); // Position 5
        inputList.add(new GatekeeperDestinationBuilder().setSubmitterNumber("10").setPackaging("bbb").setFormat("jjj").build()); // Position 4
        inputList.add(new GatekeeperDestinationBuilder().setSubmitterNumber("10").setPackaging("bbb").setFormat("iii").build()); // Position 3
        inputList.add(new GatekeeperDestinationBuilder().setSubmitterNumber("2").setPackaging("aaa").setFormat("jjj").build()); // Position 2
        inputList.add(new GatekeeperDestinationBuilder().setSubmitterNumber("2").setPackaging("aaa").setFormat("iii").build()); // Position 1
        inputList.add(new GatekeeperDestinationBuilder().setSubmitterNumber("2").setPackaging("aaa").setFormat("hhh").build()); // Position 0

        // Subject Under Test
        List<GatekeeperDestination> result = presenterImpl.doSort(inputList);

        // Verify Test
        assertThat(result.size(), is(9));
        int i = 0;
        assertThat(result.get(i).getSubmitterNumber(), is("2"));
        assertThat(result.get(i).getPackaging(), is("aaa"));
        assertThat(result.get(i).getFormat(), is("hhh"));
        i++;
        assertThat(result.get(i).getSubmitterNumber(), is("2"));
        assertThat(result.get(i).getPackaging(), is("aaa"));
        assertThat(result.get(i).getFormat(), is("iii"));
        i++;
        assertThat(result.get(i).getSubmitterNumber(), is("2"));
        assertThat(result.get(i).getPackaging(), is("aaa"));
        assertThat(result.get(i).getFormat(), is("jjj"));
        i++;
        assertThat(result.get(i).getSubmitterNumber(), is("10"));
        assertThat(result.get(i).getPackaging(), is("bbb"));
        assertThat(result.get(i).getFormat(), is("iii"));
        i++;
        assertThat(result.get(i).getSubmitterNumber(), is("10"));
        assertThat(result.get(i).getPackaging(), is("bbb"));
        assertThat(result.get(i).getFormat(), is("jjj"));
        i++;
        assertThat(result.get(i).getSubmitterNumber(), is("10"));
        assertThat(result.get(i).getPackaging(), is("ccc"));
        assertThat(result.get(i).getFormat(), is("hhh"));
        i++;
        assertThat(result.get(i).getSubmitterNumber(), is("30"));
        assertThat(result.get(i).getPackaging(), is("ddd"));
        assertThat(result.get(i).getFormat(), is("jjj"));
        i++;
        assertThat(result.get(i).getSubmitterNumber(), is("30"));
        assertThat(result.get(i).getPackaging(), is("eee"));
        assertThat(result.get(i).getFormat(), is("iii"));
        i++;
        assertThat(result.get(i).getSubmitterNumber(), is("30"));
        assertThat(result.get(i).getPackaging(), is("fff"));
        assertThat(result.get(i).getFormat(), is("hhh"));

    }


    /*
     * Private methods
     */
    private PresenterImpl setupPresenter() {
        PresenterImpl presenter = new PresenterImpl("Header Text");
        when(presenter.viewInjector.getView()).thenReturn(mockedView);
        when(presenter.viewInjector.getTexts()).thenReturn(mockedTexts);
        presenter.submitter = "submitter";
        presenter.packaging = "packaging";
        presenter.format = "format";
        presenter.destination = "destination";
        return presenter;
    }

    private void verifyInitializeDataMethodCall() {
        verify(mockedSubmitter).clearText();
        verify(mockedPackaging).clearText();
        verify(mockedFormat).clearText();
        verify(mockedDestination).clearText();
        verify(presenterImpl.flowStoreProxy).findAllGatekeeperDestinations(any(PresenterImpl.FindAllGateKeeperDestinationsCallback.class));
        verifyNoMoreInteractions(mockedSubmitter);
        verifyNoMoreInteractions(mockedPackaging);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(presenterImpl.flowStoreProxy);
    }
}

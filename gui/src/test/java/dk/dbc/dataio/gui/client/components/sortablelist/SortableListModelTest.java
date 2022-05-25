package dk.dbc.dataio.gui.client.components.sortablelist;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.gquery.GQueryWrapper;
import gwtquery.plugins.draggable.client.DraggableOptions;
import gwtquery.plugins.draggable.client.gwt.DraggableWidget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class SortableListModelTest {
    final String TEXT1 = "b-text";
    final String KEY1 = "-key-1-";
    final String TEXT2 = "a-text";
    final String KEY2 = "-key-2-";
    final String TEXT3 = "c-text";
    final String KEY3 = "-key-3-";
    private final String SELECTED = "sortable-widget-entry-selected";
    private final String NOT_SELECTED = "sortable-widget-entry-deselected";

    @Mock
    FlowPanel mockedFlowPanel;
    @Mock
    FlowPanel mockedParentPanel;
    @Mock
    GQueryWrapper mockedGQueryWrapper;
    @Mock
    GQuery mockedGQuery;
    @SuppressWarnings("deprecation")
    @Mock
    com.google.gwt.user.client.Element element1;
    @SuppressWarnings("deprecation")
    @Mock
    com.google.gwt.user.client.Element element2;
    @SuppressWarnings("deprecation")
    @Mock
    com.google.gwt.user.client.Element element3;
    @Mock
    DraggableWidget draggableWidget1;
    @Mock
    DraggableWidget draggableWidget2;
    @Mock
    DraggableWidget draggableWidget3;
    @Mock
    ValueChangeHandler<Map<String, String>> mockedEventHandler;


    @Before
    public void setupMockedObjects() {
        when(mockedFlowPanel.getParent()).thenReturn(mockedParentPanel);
        when(mockedGQueryWrapper.$(any(Widget.class))).thenReturn(mockedGQuery);
    }


    @Test
    public void create_instantiateSortableListModel_clearList() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel);

        assertThat(sortableListModel.get().size(), is(0));
    }

    @Test
    public void clear_callClear_listAndModelCleared() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel);

        sortableListModel.clear();

        verify(mockedFlowPanel).clear();
        assertThat(sortableListModel.get().size(), is(0));
    }

    @Test
    public void enableEmptyModel_callSetEnabled_listAndModelEnabledOrDisabled() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel);

        sortableListModel.setEnabled(false);
        assertThat(sortableListModel.getEnabled(), is(false));

        sortableListModel.setEnabled(true);
        assertThat(sortableListModel.getEnabled(), is(true));
    }

    @Test
    public void add_addOneItemInList_oneItemAddedCorrectly() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.add(TEXT1, KEY1);

        verify(mockedFlowPanel).add(any(Widget.class));
        assertThat(sortableListModel.modelWidgets.size(), is(1));
        DraggableOptions options = sortableListModel.modelWidgets.get(0).draggableWidget.getDraggableOptions();
        assertThat(options.getAxis(), is(DraggableOptions.AxisOption.Y_AXIS));
        assertThat(options.getContainmentAsGQuery(), is(mockedGQuery));
    }

    @Test
    public void enableNotEmptyModel_callSetEnabled_listAndModelEnabledOrDisabled() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.add(TEXT1, KEY1);

        sortableListModel.setEnabled(false);

        DraggableOptions options = sortableListModel.modelWidgets.get(0).draggableWidget.getDraggableOptions();
        assertThat(sortableListModel.getEnabled(), is(false));
        assertThat(options.isDisabled(), is(true));

        sortableListModel.setEnabled(true);
        assertThat(sortableListModel.getEnabled(), is(true));
        assertThat(options.isDisabled(), is(false));
    }

    @Test
    public void putEmptyMap_callPut_isEmpty() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);

        sortableListModel.put(new HashMap<>());

        assertThat(sortableListModel.modelWidgets.size(), is(0));
        verify(sortableListModel.list, times(0)).add(any(Widget.class));
    }

    @Test
    public void putNotEmptyMap_callPut_isNotEmpty() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);

        sortableListModel.put(constructTestData());

        assertThat(sortableListModel.modelWidgets.size(), is(3));
        assertThat(sortableListModel.modelWidgets.get(0).key, is(KEY1));
        assertThat(sortableListModel.modelWidgets.get(0).value, is(TEXT1));
        assertThat(sortableListModel.modelWidgets.get(1).key, is(KEY2));
        assertThat(sortableListModel.modelWidgets.get(1).value, is(TEXT2));
        assertThat(sortableListModel.modelWidgets.get(2).key, is(KEY3));
        assertThat(sortableListModel.modelWidgets.get(2).value, is(TEXT3));
        verify(sortableListModel.list, times(3)).add(any(Widget.class));
    }

    @Test
    public void put_emptyMapFireEvent_eventHandlerCalled() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.addValueChangeHandler(mockedEventHandler);

        sortableListModel.put(new HashMap<>(), true);

        ArgumentCaptor<ValueChangeEvent> argument = ArgumentCaptor.forClass(ValueChangeEvent.class);
        verify(mockedEventHandler).onValueChange(argument.capture());
        ValueChangeEvent<Map<String, String>> event = (ValueChangeEvent<Map<String, String>>) argument.getValue();
        assertThat(event.getValue().size(), is(0));
        verifyNoMoreInteractions(mockedEventHandler);
    }

    @Test
    public void put_notEmptyMapFireEvent_eventHandlerCalled() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.addValueChangeHandler(mockedEventHandler);

        sortableListModel.put(constructTestData(), true);

        ArgumentCaptor<ValueChangeEvent> argument = ArgumentCaptor.forClass(ValueChangeEvent.class);
        verify(mockedEventHandler).onValueChange(argument.capture());
        ValueChangeEvent<Map<String, String>> event = (ValueChangeEvent<Map<String, String>>) argument.getValue();
        assertThat(event.getValue().size(), is(3));
        verifyNoMoreInteractions(mockedEventHandler);
    }

    @Test
    public void getEmptyMap_callGet_isEmpty() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);

        Map<String, String> items = sortableListModel.get();

        assertThat(items.size(), is(0));
    }

    @Test
    public void getNotEmptyMap_callGet_isNotEmpty() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());

        Map<String, String> fetchedItems = sortableListModel.get();

        assertThat(fetchedItems.size(), is(3));
        assertThat(fetchedItems.get(KEY1), is(TEXT1));
        assertThat(fetchedItems.get(KEY2), is(TEXT2));
        assertThat(fetchedItems.get(KEY3), is(TEXT3));
    }

    @Test
    public void refresh_manualSortingEnabled_allWidgetsOnDisplayIsRefreshed() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());
        sortableListModel.setEnabled(true);
        sortableListModel.setManualSorting(true);

        sortableListModel.refresh();

        assertThat(sortableListModel.modelWidgets.get(0).draggableWidget.isDragDisabled(), is(false));
        assertThat(sortableListModel.modelWidgets.get(1).draggableWidget.isDragDisabled(), is(false));
        assertThat(sortableListModel.modelWidgets.get(2).draggableWidget.isDragDisabled(), is(false));
        verify(sortableListModel.list, times(6)).add(any(Widget.class));
    }

    @Test
    public void refresh_manualSortingDisabled_allWidgetsOnDisplayIsRefreshed() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());
        sortableListModel.setEnabled(true);
        sortableListModel.setManualSorting(false);

        sortableListModel.refresh();

        assertThat(sortableListModel.modelWidgets.get(0).draggableWidget.isDragDisabled(), is(true));
        assertThat(sortableListModel.modelWidgets.get(1).draggableWidget.isDragDisabled(), is(true));
        assertThat(sortableListModel.modelWidgets.get(2).draggableWidget.isDragDisabled(), is(true));
        verify(sortableListModel.list, times(9)).add(any(Widget.class));
    }

    @Test
    public void reOrder_manualSortingEnabled_widgetsAreReorderedAccordingToTheirTopPosition() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());
        sortableListModel.setEnabled(true);
        sortableListModel.setManualSorting(true);

        sortableListModel.reOrder();

        // NB: It is not possible to test the re-ordering part of the reOrder() method, because it is not
        // possible to set the pixel values for AbsoluteTop (it is only possible to read it).
        // Therefore, we only check, that the refresh() method is called (is done by reOrder() )

        assertThat(sortableListModel.modelWidgets.get(0).draggableWidget.isDragDisabled(), is(false));
        assertThat(sortableListModel.modelWidgets.get(1).draggableWidget.isDragDisabled(), is(false));
        assertThat(sortableListModel.modelWidgets.get(2).draggableWidget.isDragDisabled(), is(false));
        verify(sortableListModel.list, times(6)).add(any(Widget.class));
        assertThat(sortableListModel.modelWidgets.get(0).key, is(KEY1));
        assertThat(sortableListModel.modelWidgets.get(1).key, is(KEY2));
        assertThat(sortableListModel.modelWidgets.get(2).key, is(KEY3));
    }

    @Test
    public void reOrder_manualSortingDisabled_widgetsAreReorderedAccordingToText() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());
        sortableListModel.setEnabled(true);
        sortableListModel.setManualSorting(false);

        sortableListModel.reOrder();

        assertThat(sortableListModel.modelWidgets.get(0).draggableWidget.isDragDisabled(), is(true));
        assertThat(sortableListModel.modelWidgets.get(1).draggableWidget.isDragDisabled(), is(true));
        assertThat(sortableListModel.modelWidgets.get(2).draggableWidget.isDragDisabled(), is(true));
        verify(sortableListModel.list, times(9)).add(any(Widget.class));
        // Test reordering according to list item text
        assertThat(sortableListModel.modelWidgets.get(0).key, is(KEY2));
        assertThat(sortableListModel.modelWidgets.get(1).key, is(KEY1));
        assertThat(sortableListModel.modelWidgets.get(2).key, is(KEY3));
    }

    @Test
    public void setOneSelected_callSetOneSelected_checkThatOnlyOneIsSelected() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());
        when(mockedGQuery.size()).thenReturn(3);
        when(mockedGQuery.get(0)).thenReturn(element2);
        sortableListModel.modelWidgets.get(0).draggableWidget = draggableWidget1;
        sortableListModel.modelWidgets.get(1).draggableWidget = draggableWidget2;
        sortableListModel.modelWidgets.get(2).draggableWidget = draggableWidget3;
        when(draggableWidget1.getElement()).thenReturn(element1);
        when(draggableWidget2.getElement()).thenReturn(element2);
        when(draggableWidget3.getElement()).thenReturn(element3);

        sortableListModel.setOneSelected(mockedGQuery);

        verify(mockedGQuery).size();
        verify(mockedGQuery).get(0);

        verify(draggableWidget1).addStyleName(NOT_SELECTED);
        verify(draggableWidget1).removeStyleName(SELECTED);
        verify(draggableWidget2).addStyleName(SELECTED);
        verify(draggableWidget2).removeStyleName(NOT_SELECTED);
        verify(draggableWidget3).addStyleName(NOT_SELECTED);
        verify(draggableWidget3).removeStyleName(SELECTED);

        assertThat(sortableListModel.modelWidgets.get(0).selected, is(false));
        assertThat(sortableListModel.modelWidgets.get(1).selected, is(true));
        assertThat(sortableListModel.modelWidgets.get(2).selected, is(false));
    }

    @Test
    public void sortIfNeeded_manualSortEnabled_noAction() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());
        sortableListModel.setManualSorting(true);

        sortableListModel.sortIfNeeded();

        assertThat(sortableListModel.modelWidgets.get(0).key, is(KEY1));
        assertThat(sortableListModel.modelWidgets.get(1).key, is(KEY2));
        assertThat(sortableListModel.modelWidgets.get(2).key, is(KEY3));
        verify(mockedFlowPanel).clear();  // To make sure, that refresh has been called
    }

    @Test
    public void sortIfNeeded_manualSortDisabled_noAction() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());
        sortableListModel.setManualSorting(false);

        sortableListModel.sortIfNeeded();

        assertThat(sortableListModel.modelWidgets.get(0).key, is(KEY2));
        assertThat(sortableListModel.modelWidgets.get(1).key, is(KEY1));
        assertThat(sortableListModel.modelWidgets.get(2).key, is(KEY3));
        verify(mockedFlowPanel, times(3)).clear();  // To make sure, that refresh has been called
    }

    @Test
    public void getSelected_noSelection_returnNone() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());

        assertThat(sortableListModel.getSelectedItem(), is((String) null));
    }

    @Test
    public void getSelected_oneSelected_returnSelectedItem() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());
        sortableListModel.modelWidgets.get(1).selected = true;  // Set selection to KEY2 (index 1)

        assertThat(sortableListModel.getSelectedItem(), is(KEY2));
    }

    @Test
    public void getSelected_twoSelected_returnFirstSelectedItem() {
        SortableListModel sortableListModel = new SortableListModel(mockedFlowPanel, mockedGQueryWrapper);
        sortableListModel.put(constructTestData());
        sortableListModel.modelWidgets.get(0).selected = true;  // Set selection to KEY1 (index 0)
        sortableListModel.modelWidgets.get(2).selected = true;  // AND set selection to KEY3 (index 2)

        assertThat(sortableListModel.getSelectedItem(), is(KEY1));
    }


    /*
     * Private methods
     */

    private Map<String, String> constructTestData() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put(KEY1, TEXT1);
        items.put(KEY2, TEXT2);
        items.put(KEY3, TEXT3);
        return items;
    }

}

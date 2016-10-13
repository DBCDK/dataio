/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test of SinkStatusTable
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class SinkStatusTableTest {

    @Mock Presenter mockedPresenter;
    @Mock ListDataProvider<SinkStatusTable.SinkStatusModel> mockedDataProvider;
    @Mock List<SinkStatusTable.SinkStatusModel> mockedSinkStatusList;
    @Mock Texts mockedTexts;
    @Mock DoubleClickEvent mockedDoubleClickEvent;
    @Mock SingleSelectionModel<SinkStatusTable.SinkStatusModel> mockedSelectionModel;

    // Test Data
    List<SinkStatusTable.SinkStatusModel> testData = Arrays.asList(
            new SinkStatusTable.SinkStatusModel("Dummy sink",  "Dummy sink",            0,     0, 1234567890L),
            new SinkStatusTable.SinkStatusModel("Dummy sink",  "Tracer bullit sink",    0,     0, 1234567890L),
            new SinkStatusTable.SinkStatusModel("ES sink",     "Basis22",               2,     4, 1234567890L),
            new SinkStatusTable.SinkStatusModel("ES sink",     "Danbib3",               0,     0, 1234567890L),
            new SinkStatusTable.SinkStatusModel("Hive sink",   "Cisterne sink",        34,    56, 1234567890L),
            new SinkStatusTable.SinkStatusModel("Hive sink",   "Boblebad sink",        32,    54, 1234567890L),
            new SinkStatusTable.SinkStatusModel("Update sink", "Cisterne Update sink",  1, 56023, 1234567890L),
            new SinkStatusTable.SinkStatusModel("IMS sink",    "IMS cisterne sink",     7,     8, 1234567890L)
    );
    SinkStatusTable.SinkStatusModel testSinkStatus = new SinkStatusTable.SinkStatusModel("Test sink", "Sink name", 111, 222, 1234567890L);


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
        assertThat(sinkStatusTable.getColumn(i++).getValue(testSinkStatus), is("1970-01-15 07:56:07"));
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

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

package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.view.client.ListDataProvider;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
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
 * Unit test of GatekeepersTable
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class GatekeepersTableTest {

    @Mock ListDataProvider<GatekeeperDestination> mockedDataProvider;
    @Mock List<GatekeeperDestination> mockedGatekeeperList;
    @Mock Texts mockedTexts;

    @Before
    public void setupTexts() {
        when(mockedTexts.label_DoNotCopy()).thenReturn("DoNotCopy");
    }


    @Test
    public void constructor_noData_emptyOk() {
        // Subject under test
        GatekeepersTable gatekeepersTable = new GatekeepersTable();

        // Verify Test
        assertThat(gatekeepersTable.getRowCount(), is(0));
    }

    @Test
    public void constructor_data_dataOk() {
        // Prepare test
        GatekeepersTable gatekeepersTable = new GatekeepersTable();
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
                setFormat("fo").
                setCopyToPosthus(false).build();

        // Subject Under Test
        GatekeepersTable gatekeepersTable = new GatekeepersTable();
        gatekeepersTable.texts = mockedTexts;

        // Verify Test
        assertThat(gatekeepersTable.getColumnCount(), is(5));
        assertThat(gatekeepersTable.getColumn(0).getValue(gatekeeper), is("11"));
        assertThat(gatekeepersTable.getColumn(1).getValue(gatekeeper), is("pa"));
        assertThat(gatekeepersTable.getColumn(2).getValue(gatekeeper), is("fo"));
        assertThat(gatekeepersTable.getColumn(3).getValue(gatekeeper), is("de"));
        assertThat(gatekeepersTable.getColumn(4).getValue(gatekeeper), is("DoNotCopy"));
    }

}

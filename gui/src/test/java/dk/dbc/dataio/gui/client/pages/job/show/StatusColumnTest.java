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

package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Event;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.modelBuilders.JobModelBuilder;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;


/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class StatusColumnTest {
    // Mocked data
    @Mock Resources mockedResources;
    @Mock Cell<ImageResource> mockedCell;
    @Mock static Event mockedBrowserClickEvent;
    @Mock ImageResource gray;
    @Mock ImageResource green;
    @Mock ImageResource red;
    @Mock ImageResource yellow;

    @Before
    public void setupMockedEvents() {
        when(mockedBrowserClickEvent.getType()).thenReturn("click");
        when(mockedResources.gray()).thenReturn(gray);
        when(mockedResources.green()).thenReturn(green);
        when(mockedResources.red()).thenReturn(red);
        when(mockedResources.yellow()).thenReturn(yellow);
    }


    // Test data
    private JobModel doneWithoutErrorModel = new JobModelBuilder()
            .setNumberOfItems(10)
            .setFailedCounter(0)
            .setIgnoredCounter(0)
            .setPartitionedCounter(41)
            .setProcessedCounter(42)
            .setDeliveredCounter(43)
            .setDiagnosticModels(null)
            .setJobCompletionTime(new Date().toString())
            .build();

    private JobModel doneWithErrorModel = new JobModelBuilder()
            .setNumberOfItems(10)
            .setFailedCounter(5)
            .setIgnoredCounter(5)
            .setPartitionedCounter(44)
            .setProcessedCounter(45)
            .setDeliveredCounter(46)
            .setJobCompletionTime(new Date().toString())
            .build();

    private JobModel notDoneModel = new JobModelBuilder()
            .setIsJobDone(false)
            .setFailedCounter(0)
            .setIgnoredCounter(0)
            .setPartitionedCounter(47)
            .setProcessedCounter(48)
            .setDeliveredCounter(49)
            .setDiagnosticModels(null)
            .build();

    private JobModel previewModel = new JobModelBuilder()
            .setNumberOfItems(10)
            .setNumberOfChunks(0)
            .setFailedCounter(0)
            .setIgnoredCounter(0)
            .setPartitionedCounter(0)
            .setProcessedCounter(0)
            .setDeliveredCounter(0)
            .setDiagnosticModels(null)
            .setJobCompletionTime(new Date().toString())
            .build();

    // Subject Under Test
    StatusColumn statusColumn;


    // Test Constructor
    @Test
    public void statusColumn_constructor_correctlySetup() {
        assertThat(true, is(true));

        // Test Subject Under Test
        statusColumn = new StatusColumn(mockedResources, mockedCell);
    }

    // Test getValue(...)
    @Test
    public void getValueAndGetJobStatus_doneWithoutErrorModel_returnGreen() {
        statusColumn = new StatusColumn(mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(doneWithoutErrorModel), is(mockedResources.green()));
    }

    @Test
    public void getValueAndGetJobStatus_doneWithErrorModel_returnRed() {
        statusColumn = new StatusColumn(mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(doneWithErrorModel), is(mockedResources.red()));
    }

    @Test
    public void getValueAndGetJobStatus_notDoneModel_returnGray() {
        statusColumn = new StatusColumn(mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(notDoneModel), is(mockedResources.gray()));
    }

    @Test
    public void getValueAndGetJobStatus_previewModel_returnYellow() {
        statusColumn = new StatusColumn(mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(previewModel), is(mockedResources.yellow()));
    }

}

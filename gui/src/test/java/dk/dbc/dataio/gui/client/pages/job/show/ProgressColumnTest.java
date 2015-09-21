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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Event;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.components.MultiProgressBar;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.modelBuilders.JobModelBuilder;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
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
@SuppressWarnings("deprecation")  // Due to the fact, that com.google.gwt.user.client.Element is deprecated, and MultiProgressBar.getElement() returns this element, so I am forced to use it!
@RunWith(GwtMockitoTestRunner.class)
public class ProgressColumnTest {
    // Mocked data
    @Mock EventBus mockedEventBus;
    @Mock Resources mockedResources;
    @Mock Cell<ImageResource> mockedCell;
    @Mock Cell.Context mockedContext;
    @Mock com.google.gwt.user.client.Element mockedElement;
    @Mock static Event mockedBrowserClickEvent;
    @Mock MultiProgressBar mockedMultiProgressBar;
    @Mock SafeHtmlBuilder mockedHtmlBuilder;

    @Before
    public void setupMockedEvents() {
        when(mockedBrowserClickEvent.getType()).thenReturn("click");
    }


    // Test data
    private JobModel legalTestModel = new JobModelBuilder()
            .setItemCounter(100)
            .setFailedCounter(0)
            .setIgnoredCounter(0)
            .setPartitionedCounter(41)
            .setProcessedCounter(23)
            .setDeliveredCounter(12)
            .build();

    private JobModel illegalTestModel1 = new JobModelBuilder()
            .setItemCounter(100)
            .setFailedCounter(0)
            .setIgnoredCounter(0)
            .setPartitionedCounter(41)
            .setProcessedCounter(23)
            .setDeliveredCounter(43)
            .build();

    private JobModel illegalTestModel2 = new JobModelBuilder()
            .setItemCounter(20)
            .setFailedCounter(0)
            .setIgnoredCounter(0)
            .setPartitionedCounter(41)
            .setProcessedCounter(23)
            .setDeliveredCounter(12)
            .build();

    // Subject Under Test
    ProgressColumn progressColumn;


    // Test Constructor
    @Test
    public void progressColumn_constructor_correctlySetup() {

        // Test Subject Under Test
        progressColumn = new ProgressColumn();
        assertThat(progressColumn.getCell() instanceof ProgressColumn.ProgressCell, is(true));
    }

    @Test
    public void progressCell_constructor_correctlySetup() {
        ProgressColumn.ProgressCell progressCell = new ProgressColumn.ProgressCell();
    }

    @Test
    public void progressCell_renderWithNullProgressBar_noAction() {
        ProgressColumn.ProgressCell progressCell = new ProgressColumn.ProgressCell();

        // Test Subject Under Test
        progressCell.render(mockedContext, null, mockedHtmlBuilder);

        // Verification
        verifyNoMoreInteractions(mockedHtmlBuilder);
    }

    @Test
    public void progressCell_renderWithProgressBar_renderHtml() {
        // Setup Test
        final String INNERHTML = "<inner html>";
        ProgressColumn.ProgressCell progressCell = new ProgressColumn.ProgressCell();
        com.google.gwt.user.client.Element mockedElement = mock(com.google.gwt.user.client.Element.class);
        when(mockedMultiProgressBar.getElement()).thenReturn(mockedElement);
        when(mockedElement.getInnerHTML()).thenReturn(INNERHTML);

        // Test Subject Under Test
        progressCell.render(mockedContext, mockedMultiProgressBar, mockedHtmlBuilder);
        // Verification
        verify(mockedHtmlBuilder).appendHtmlConstant(INNERHTML);
    }

    // Test getValue(...)
    @Test
    public void getValue_doneWithoutErrorModel_returnOK() {
        // Setup Test
        progressColumn = new ProgressColumn();

        // Test Subject Under Test
        MultiProgressBar progressBar = progressColumn.getValue(legalTestModel);

        // Verification
        verify(progressBar.textProgress).setText("12/11/77"); // 12=12, 11=max(0,23-12), 77=100-23
        verify(progressBar.firstProgress).setAttribute("value", "12");
        verify(progressBar.secondProgress).setAttribute("value", "23");
    }

    @Test
    public void getValue_doneWithErrorModel1_returnOK() {
        // Setup Test
        progressColumn = new ProgressColumn();

        // Test Subject Under Test
        MultiProgressBar progressBar = progressColumn.getValue(illegalTestModel1);

        // Verification
        verify(progressBar.textProgress).setText("43/0/77"); // 43=43, 0=max(0,12-43), 77=100-23
        verify(progressBar.firstProgress).setAttribute("value", "43");
        verify(progressBar.secondProgress).setAttribute("value", "23");
    }

    @Test
    public void getValue_doneWithErrorModel2_returnOK() {
        // Setup Test
        progressColumn = new ProgressColumn();

        // Test Subject Under Test
        MultiProgressBar progressBar = progressColumn.getValue(illegalTestModel2);

        // Verification
        verify(progressBar.textProgress).setText("12/11/0"); // 12=12, 11=max(0,23-12), 77=max(0,20-23)
        verify(progressBar.firstProgress).setAttribute("value", "12");
        verify(progressBar.secondProgress).setAttribute("value", "23");
    }

}

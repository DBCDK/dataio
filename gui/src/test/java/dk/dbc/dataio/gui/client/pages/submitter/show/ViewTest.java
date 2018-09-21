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

package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class ViewTest {
    @Mock Presenter mockedPresenter;
    @Mock dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock ViewGinjector mockedViewInjector;
    @Mock CommonGinjector mockedCommonInjector;
    @Mock Texts mockedTexts;


    // Test Data
    private SubmitterModel testModel1 = new SubmitterModelBuilder().setEnabled(true).setNumber("564738").setName("Submitter Name 1").setDescription("Submitter Description 1").build();
    private SubmitterModel testModel2 = new SubmitterModelBuilder().setNumber("564739").setName("Submitter Name 2").setDescription("Submitter Description 2").setEnabled(false).build();
    private List<SubmitterModel> testModels = new ArrayList<>(Arrays.asList(testModel1, testModel2));

    // Subject Under Test
    private View view;

    // Mocked Texts
    final static String MOCKED_LABEL_SUBMITTERS = "Mocked Text: Submittere";
    final static String MOCKED_BUTTON_EDIT = "Mocked Text: Rediger";
    final static String MOCKED_BUTTON_SHOWFLOWBINDERS = "Mocked Text: button_ShowFlowBinders";
    final static String MOCKED_COLUMNHEADER_NUMBER = "Mocked Text: Nummer";
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: Navn";
    final static String MOCKED_COLUMNHEADER_DESCRIPTION = "Mocked Text: Beskrivelse";
    final static String MOCKED_COLUMNHEADER_FLOWBINDERS = "Mocked Text: columnHeader_FlowBinders";
    final static String MOCKED_COLUMNHEADER_ACTION = "Mocked Text: Handling";
    final static String MOCKED_COLUMNHEADER_STATUS = "Mocked Text: Tilstand";

    class ViewConcrete extends View {
        public ViewConcrete() {
            super();
        }
        @Override
        public Texts getTexts() {
            return mockedTexts;
        }
    }

    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonInjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_Submitters()).thenReturn("Header Text");
        when(mockedTexts.label_Submitters()).thenReturn(MOCKED_LABEL_SUBMITTERS);
        when(mockedTexts.button_Edit()).thenReturn(MOCKED_BUTTON_EDIT);
        when(mockedTexts.button_ShowFlowBinders()).thenReturn(MOCKED_BUTTON_SHOWFLOWBINDERS);
        when(mockedTexts.columnHeader_Number()).thenReturn(MOCKED_COLUMNHEADER_NUMBER);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_Description()).thenReturn(MOCKED_COLUMNHEADER_DESCRIPTION);
        when(mockedTexts.columnHeader_FlowBinders()).thenReturn(MOCKED_COLUMNHEADER_FLOWBINDERS);
        when(mockedTexts.columnHeader_Action()).thenReturn(MOCKED_COLUMNHEADER_ACTION);
        when(mockedTexts.columnHeader_Status()).thenReturn(MOCKED_COLUMNHEADER_STATUS);
        when(mockedTexts.value_Disabled()).thenReturn("disabled");
    }


    /*
     * Testing starts here...
     */
    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        setupView();

        // Verify invocations
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NUMBER));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESCRIPTION));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_STATUS));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FLOWBINDERS));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION));
    }


    @Test
    public void constructor_setupData_dataSetupCorrect() {
        setupView();

        List<SubmitterModel> models = view.dataProvider.getList();

        assertThat(models.isEmpty(), is(true));

        // Subject Under Test
        view.setSubmitters(testModels);

        assertThat(models.isEmpty(), is(false));
        assertThat(models.size(), is(2));
        assertThat(models.get(0).getName(), is(testModel1.getName()));
        assertThat(models.get(1).getName(), is(testModel2.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSubmitterNumberColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructSubmitterNumberColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getNumber()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructNameColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDescriptionColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructDescriptionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getDescription()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructStatusColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructStatusColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(""));
        assertThat(column.getValue(testModel2), is("disabled"));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowBindersColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructFlowBindersColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(mockedTexts.button_ShowFlowBinders()));

        // Test that the right action is activated upon click
        view.setPresenter(mockedPresenter);
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(37, testModel1, "Show FlowBinders Button Text");  // Simulate a click on the column
        verify(mockedPresenter).showFlowBinders(testModel1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructActionColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructActionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(mockedTexts.button_Edit()));

        // Test that the right action is activated upon click
        view.setPresenter(mockedPresenter);
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(3, testModel1, "Updated Button Text");  // Simulate a click on the column
        verify(mockedPresenter).editSubmitter(testModel1);
    }

    private void setupView() {
        view = new ViewConcrete();
        view.commonInjector = mockedCommonInjector;
        view.viewInjector = mockedViewInjector;
    }

}
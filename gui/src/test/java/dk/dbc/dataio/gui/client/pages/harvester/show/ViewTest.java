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

package dk.dbc.dataio.gui.client.pages.harvester.show;


import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.test.types.RawRepoHarvesterConfigEntryBuilder;
import dk.dbc.dataio.harvester.types.OpenAgencyTarget;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * View unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class ViewTest {

    @Mock Presenter mockedPresenter;
    @Mock dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock CommonGinjector mockedCommonGinjector;
    @Mock ViewGinjector mockedViewGinjector;

    // Test Data
    private RawRepoHarvesterConfig testHarvesterConfig = new RawRepoHarvesterConfig();
    private RawRepoHarvesterConfig testHarvesterConfigExtra = new RawRepoHarvesterConfig();
    private OpenAgencyTarget testOpenAgencyTarget = new OpenAgencyTarget();
    private RawRepoHarvesterConfig.Entry testHarvesterConfigEntry1 = new RawRepoHarvesterConfigEntryBuilder().
            setId("ID1").
            setResource("Resource1").
            setConsumerId("ConsumerId1").
            setDestination("Destination1").
            setType(JobSpecification.Type.TRANSIENT).
            setFormat("Format1").
            setFormatOverrides(234567, "FormatOverride2").
            setFormatOverrides(123456, "FormatOverride1").
            setIncludeRelations(true).
            setBatchSize(321).
            setOpenAgencyTarget(testOpenAgencyTarget).
            build();
    private RawRepoHarvesterConfig.Entry testHarvesterConfigEntry2 = new RawRepoHarvesterConfigEntryBuilder().setId("ID2").build();
    private RawRepoHarvesterConfig.Entry testHarvesterConfigEntry3 = new RawRepoHarvesterConfigEntryBuilder().setId("ID3").build();

    @Before
    public void setupTestHarvesterConfig() {
        testOpenAgencyTarget.setUrl("Url1");
        testOpenAgencyTarget.setGroup("Group1");
        testOpenAgencyTarget.setUser("User1");
        testOpenAgencyTarget.setPassword("Password1");
        testHarvesterConfig.addEntry(testHarvesterConfigEntry2);
        testHarvesterConfig.addEntry(testHarvesterConfigEntry1);
        testHarvesterConfigExtra.addEntry(testHarvesterConfigEntry3);
    }

    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: Navn";
    final static String MOCKED_COLUMNHEADER_RESOURCE = "Mocked Text: Resource";
    final static String MOCKED_COLUMNHEADER_TARGET = "Mocked Text: OpenAgencyTarget";
    final static String MOCKED_COLUMNHEADER_ID = "Mocked Text: ConsumerId";
    final static String MOCKED_COLUMNHEADER_SIZE = "Mocked Text: BatchSize";
    final static String MOCKED_COLUMNHEADER_FORMATOVERRIDES = "Mocked Text: FormatOverrides";
    final static String MOCKED_COLUMNHEADER_RELATIONS = "Mocked Text: IncludeRelations";
    final static String MOCKED_COLUMNHEADER_DESTINATION = "Mocked Text: Destination";
    final static String MOCKED_COLUMNHEADER_FORMAT = "Mocked Text: Format";
    final static String MOCKED_COLUMNHEADER_TYPE = "Mocked Text: Type";
    final static String MOCKED_HELP_NAME = "Mocked Text: Navn på høsterkonfiguration";
    final static String MOCKED_HELP_RESOURCE = "Mocked Text: Databaseressourcen i Glassfish – Glassfishnavnet på det RR, der høstes fra";
    final static String MOCKED_HELP_TARGET = "Mocked Text: Den openAgency-instans som RR skal kalde mhp. fastsættelse af postmodel (påhængsposter eller lokalposter)";
    final static String MOCKED_HELP_ID = "Mocked Text: Navn på den RR-kø, der høstes fra";
    final static String MOCKED_HELP_SIZE = "Mocked Text: Det maksimale antal poster der kan høstes pr job";
    final static String MOCKED_HELP_FORMATOVERRIDES = "Mocked Text: Hvis poster med den angivne submitter høstes, får disse det i \"FormatOverrides\" angivne format, i stedet for det, der er angivet i \"Format\"";
    final static String MOCKED_HELP_RELATIONS = "Mocked Text: true = alle relaterede poster høstes med henblik på postsammenskrivning, false = kun den post der er lagt på kø høstes";
    final static String MOCKED_HELP_DESTINATION = "Mocked Text: Bruges til match med indholdet i Destination i en IO-flowbinder";
    final static String MOCKED_HELP_FORMAT = "Mocked Text: Bruges til match med indholdet i Format i en IO-flowbinder";
    final static String MOCKED_HELP_TYPE = "Mocked Text: Angivelse af jobtype for de jobs, der dannes ud fra en høstning";
    final static String MOCKED_INCLUDERELATIONSTRUE = "Mocked Text: true";
    final static String MOCKED_INCLUDERELATIONSFALSE = "Mocked Text: false";

    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_Flows()).thenReturn("Header Text");

        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_Resource()).thenReturn(MOCKED_COLUMNHEADER_RESOURCE);
        when(mockedTexts.columnHeader_Target()).thenReturn(MOCKED_COLUMNHEADER_TARGET);
        when(mockedTexts.columnHeader_Id()).thenReturn(MOCKED_COLUMNHEADER_ID);
        when(mockedTexts.columnHeader_Size()).thenReturn(MOCKED_COLUMNHEADER_SIZE);
        when(mockedTexts.columnHeader_FormatOverrides()).thenReturn(MOCKED_COLUMNHEADER_FORMATOVERRIDES);
        when(mockedTexts.columnHeader_Relations()).thenReturn(MOCKED_COLUMNHEADER_RELATIONS);
        when(mockedTexts.columnHeader_Destination()).thenReturn(MOCKED_COLUMNHEADER_DESTINATION);
        when(mockedTexts.columnHeader_Format()).thenReturn(MOCKED_COLUMNHEADER_FORMAT);
        when(mockedTexts.columnHeader_Type()).thenReturn(MOCKED_COLUMNHEADER_TYPE);
        when(mockedTexts.help_Name()).thenReturn(MOCKED_HELP_NAME);
        when(mockedTexts.help_Resource()).thenReturn(MOCKED_HELP_RESOURCE);
        when(mockedTexts.help_Target()).thenReturn(MOCKED_HELP_TARGET);
        when(mockedTexts.help_Id()).thenReturn(MOCKED_HELP_ID);
        when(mockedTexts.help_Size()).thenReturn(MOCKED_HELP_SIZE);
        when(mockedTexts.help_FormatOverrides()).thenReturn(MOCKED_HELP_FORMATOVERRIDES);
        when(mockedTexts.help_Relations()).thenReturn(MOCKED_HELP_RELATIONS);
        when(mockedTexts.help_Destination()).thenReturn(MOCKED_HELP_DESTINATION);
        when(mockedTexts.help_Format()).thenReturn(MOCKED_HELP_FORMAT);
        when(mockedTexts.help_Type()).thenReturn(MOCKED_HELP_TYPE);
        when(mockedTexts.includeRelationsTrue()).thenReturn(MOCKED_INCLUDERELATIONSTRUE);
        when(mockedTexts.includeRelationsFalse()).thenReturn(MOCKED_INCLUDERELATIONSFALSE);
    }

    class ViewConcrete extends View {

        public ViewConcrete() {
            super();
            viewInjector = mockedViewGinjector;
        }
        @Override
       public Texts getTexts() {
           return mockedTexts;
       }

    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        view = new ViewConcrete();

        // Verify invocations
        verify(view.harvestersTable).addRangeChangeHandler(any(RangeChangeEvent.Handler.class));
        verify(view.harvestersTable).setRowCount(0, true);
        verifyColumnContent(MOCKED_COLUMNHEADER_NAME, MOCKED_HELP_NAME);
        verifyColumnContent(MOCKED_COLUMNHEADER_RESOURCE, MOCKED_HELP_RESOURCE);
        verifyColumnContent(MOCKED_COLUMNHEADER_TARGET, MOCKED_HELP_TARGET);
        verifyColumnContent(MOCKED_COLUMNHEADER_ID, MOCKED_HELP_ID);
        verifyColumnContent(MOCKED_COLUMNHEADER_SIZE, MOCKED_HELP_SIZE);
        verifyColumnContent(MOCKED_COLUMNHEADER_FORMATOVERRIDES, MOCKED_HELP_FORMATOVERRIDES);
        verifyColumnContent(MOCKED_COLUMNHEADER_RELATIONS, MOCKED_HELP_RELATIONS);
        verifyColumnContent(MOCKED_COLUMNHEADER_DESTINATION, MOCKED_HELP_DESTINATION);
        verifyColumnContent(MOCKED_COLUMNHEADER_FORMAT, MOCKED_HELP_FORMAT);
        verifyColumnContent(MOCKED_COLUMNHEADER_TYPE, MOCKED_HELP_TYPE);
        verify(view.harvestersTable).setSelectionModel(view.selectionModel);
        verifyNoMoreInteractions(view.harvestersTable);
    }

    @SuppressWarnings("unchecked")
    private void verifyColumnContent(String columnHeader, String help) {
        SafeHtml safeHtmlText = SafeHtmlUtils.fromSafeConstant("<span title='" + help + "'>" + columnHeader + "</span>");
        verify(view.harvestersTable).addColumn(isA(Column.class), eq(safeHtmlText));
    }


    @Test
    public void setHarvesters_callSetupHarvesters_dataSetupCorrect() {
        view = new ViewConcrete();

        List<RawRepoHarvesterConfig.Entry> harvesters = view.dataProvider.getList();
        assertThat(harvesters.isEmpty(), is(true));

        // Subject Under Test
        view.setHarvesters(testHarvesterConfig);

        assertThat(harvesters.isEmpty(), is(false));
        assertThat(harvesters.size(), is(2));
        assertThat(harvesters.get(0).getId(), is("ID1"));
        assertThat(harvesters.get(1).getId(), is("ID2"));
    }

    @Test
    public void setHarvesters_callSetupHarvestersTwice_dataSetupCorrect() {
        view = new ViewConcrete();

        List<RawRepoHarvesterConfig.Entry> harvesters = view.dataProvider.getList();

        // Subject Under Test
        view.setHarvesters(testHarvesterConfig);
        view.setHarvesters(testHarvesterConfigExtra);  // The second call clears the existing harvesters, and puts ID3 only

        assertThat(harvesters.isEmpty(), is(false));
        assertThat(harvesters.size(), is(1));
        assertThat(harvesters.get(0).getId(), is("ID3"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructNameColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testHarvesterConfigEntry1), is("ID1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructResourceColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructResourceColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testHarvesterConfigEntry1), is("Resource1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructTargetColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructTargetColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testHarvesterConfigEntry1), is("Url1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructConsumerIdColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructConsumerIdColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testHarvesterConfigEntry1), is("ConsumerId1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSizeColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructSizeColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testHarvesterConfigEntry1), is("321"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFormatOverridesColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructFormatOverridesColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testHarvesterConfigEntry1), is("123456 - FormatOverride1, 234567 - FormatOverride2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructRelationsColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructRelationsColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testHarvesterConfigEntry1), is("Mocked Text: true"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDestinationColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructDestinationColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testHarvesterConfigEntry1), is("Destination1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFormatColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructFormatColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testHarvesterConfigEntry1), is("Format1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructTypeColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructTypeColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testHarvesterConfigEntry1), is("TRANSIENT"));
    }


}
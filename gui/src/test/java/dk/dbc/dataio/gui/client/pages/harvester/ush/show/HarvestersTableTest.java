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

package dk.dbc.dataio.gui.client.pages.harvester.ush.show;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
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
 * Unit test of HarvestersTable
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class HarvestersTableTest {

    @Mock Presenter mockedPresenter;
    @Mock ListDataProvider<UshSolrHarvesterConfig> mockedDataProvider;
    @Mock List<UshSolrHarvesterConfig> mockedHarvesterList;
    @Mock Texts mockedTexts;
    @Mock DoubleClickEvent mockedDoubleClickEvent;
    @Mock SingleSelectionModel<UshSolrHarvesterConfig> mockedSelectionModel;

    // Test Data
    private List<UshSolrHarvesterConfig> testHarvesterConfig = new ArrayList<>();
    private UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties().
            withAmountHarvested(555).
            withCurrentStatus("UshPropStatus").
            withEnabled(true).
            withId(22).
            withJobClass("UshPropJobClass").
            withLastHarvestFinishedDate(new Date(1000L)).
            withLastHarvestStartedDate(new Date(2000L)).
            withLastUpdatedDate(new Date(3000L)).
            withMessage("UshPropMessage").
            withName("UshPropName").
            withNextHarvestSchedule(new Date(4000L)).
            withStorageUrl("UshPropStorageUrl");
    private UshSolrHarvesterConfig testHarvesterConfigEntry1 = new UshSolrHarvesterConfig(1,2, new UshSolrHarvesterConfig.Content().
            withName("UshName").
            withDescription("UshDescritpion").
            withFormat("UshFormat").
            withDestination("UshDestination").
            withSubmitterNumber(432).
            withUshHarvesterJobId(22).
            withUshHarvesterProperties(ushHarvesterProperties).
            withTimeOfLastHarvest(new Date(5000L)).
            withEnabled(true)
    );

    @Before
    public void setupTexts() {
        when(mockedTexts.value_Enabled()).thenReturn("UshEnabled");
        when(mockedTexts.value_Disabled()).thenReturn("UshDisabled");
        when(mockedTexts.button_Edit()).thenReturn("UshEditButton");
    }


    // Subject Under Test
    private HarvestersTable harvestersTable;



    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        harvestersTable = new HarvestersTable();

        // Verify Test
        assertThat(harvestersTable.getRowCount(), is(0));
    }

    @Test(expected = NullPointerException.class)
    public void setHarvesters_nullData_exception() {
        // Test Preparation
        harvestersTable = new HarvestersTable();

        // Subject Under Test
        harvestersTable.setHarvesters(mockedPresenter, null);
    }

    @Test
    public void setHarvesters_empty_dataOk() {
        // Test Preparation
        harvestersTable = new HarvestersTable();
        harvestersTable.dataProvider = mockedDataProvider;
        when(mockedDataProvider.getList()).thenReturn(mockedHarvesterList);

        // Subject Under Test
        harvestersTable.setHarvesters(mockedPresenter, testHarvesterConfig);

        // Verify Test
        verify(mockedDataProvider, times(2)).getList();
        verifyNoMoreInteractions(mockedDataProvider);
        verify(mockedHarvesterList).clear();
    }

    @Test
    public void constructor_data_checkGetValueCallbacks() {
        // Subject Under Test
        harvestersTable = new HarvestersTable();
        harvestersTable.texts = mockedTexts;

        // Verify Test
        assertThat(harvestersTable.getColumnCount(), is(9));
        int i = 0;
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("22"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("UshName"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("UshPropStatus"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("1970-01-01 01:00:01"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("555"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("1970-01-01 01:00:04"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("UshPropMessage"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("UshEnabled"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("UshEditButton"));
    }

    @Test
    public void textWithToolTip_nullInput_nullValuedOutput() {
        // Test preparation
        harvestersTable = new HarvestersTable();

        // Subject Under Test
        SafeHtml result = harvestersTable.textWithToolTip(null, null);

        // Verify Test
        assertThat(result.toString(), is("safe: \"<span title='null'>null</span>\""));
    }

    @Test
    public void textWithToolTip_emptyInput_emptyOutput() {
        // Test preparation
        harvestersTable = new HarvestersTable();

        // Subject Under Test
        SafeHtml result = harvestersTable.textWithToolTip("", "");

        // Verify Test
        assertThat(result.toString(), is("safe: \"<span title=''></span>\""));
    }

    @Test
    public void textWithToolTip_nonEmptyInput_nonEmptyOutput() {
        // Test preparation
        harvestersTable = new HarvestersTable();

        // Subject Under Test
        SafeHtml result = harvestersTable.textWithToolTip("monkey", "elephant");

        // Verify Test
        assertThat(result.toString(), is("safe: \"<span title='elephant'>monkey</span>\""));
    }

    @Test
    public void getDoubleClickHandler__ok() {
        // Test preparation
        harvestersTable = new HarvestersTable();
        harvestersTable.presenter = mockedPresenter;
        harvestersTable.setSelectionModel(mockedSelectionModel);
        harvestersTable.selectionModel = mockedSelectionModel;
        when(mockedSelectionModel.getSelectedObject()).thenReturn(testHarvesterConfigEntry1);
        DoubleClickHandler handler = harvestersTable.getDoubleClickHandler();

        // Subject Under Test
        handler.onDoubleClick(mockedDoubleClickEvent);

        // Verify Test
        verify(mockedSelectionModel).getSelectedObject();
        verify(mockedPresenter).editHarvesterConfig("1");
    }

}

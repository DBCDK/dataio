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
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.JobSpecification;
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

    @Mock ListDataProvider<RawRepoHarvesterConfig.Entry> mockedDataProvider;
    @Mock List<RawRepoHarvesterConfig.Entry> mockedHarvesterList;
    @Mock Texts mockedTexts;

    // Test Data
    private RawRepoHarvesterConfig testHarvesterConfig = new RawRepoHarvesterConfig();
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

    @Before
    public void setupTestHarvesterConfig() {
        testOpenAgencyTarget.setUrl("Url1");
        testOpenAgencyTarget.setGroup("Group1");
        testOpenAgencyTarget.setUser("User1");
        testOpenAgencyTarget.setPassword("Password1");
        testHarvesterConfig.addEntry(testHarvesterConfigEntry2);
        testHarvesterConfig.addEntry(testHarvesterConfigEntry1);
    }

    @Before
    public void setupTexts() {
        when(mockedTexts.includeRelationsTrue()).thenReturn("includeRelationsTrue");
        when(mockedTexts.includeRelationsFalse()).thenReturn("includeRelationsFalse");
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
        harvestersTable.setHarvesters(null);
    }

    @Test
    public void setHarvesters_empty_dataOk() {
        // Test Preparation
        harvestersTable = new HarvestersTable();
        harvestersTable.dataProvider = mockedDataProvider;
        when(mockedDataProvider.getList()).thenReturn(mockedHarvesterList);

        // Subject Under Test
        harvestersTable.setHarvesters(testHarvesterConfig);

        // Verify Test
        verify(mockedDataProvider, times(4)).getList();
        verifyNoMoreInteractions(mockedDataProvider);
        verify(mockedHarvesterList).clear();
        verify(mockedHarvesterList).add(testHarvesterConfigEntry1);
        verify(mockedHarvesterList).add(testHarvesterConfigEntry2);
    }

    @Test
    public void constructor_data_checkGetValueCallbacks() {
        // Subject Under Test
        harvestersTable = new HarvestersTable();
        harvestersTable.texts = mockedTexts;

        // Verify Test
        assertThat(harvestersTable.getColumnCount(), is(10));
        int i = 0;
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("ID1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("Resource1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("Url1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("ConsumerId1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("321"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("123456 - FormatOverride1, 234567 - FormatOverride2"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("includeRelationsTrue"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("Destination1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("Format1"));
        assertThat(harvestersTable.getColumn(i++).getValue(testHarvesterConfigEntry1), is("TRANSIENT"));
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

}

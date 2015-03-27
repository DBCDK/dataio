
package dk.dbc.dataio.gui.client.pages.item.show;


import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * PresenterImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class PlaceTest {


    @Test
    public void instantiateWithUrl_instantiateWithOKUrl_idSubmitterAndSinkOK() {
        Place place = new Place("id:submitter:sink");

        assertThat(place.getJobId(), is("id"));
        assertThat(place.getSubmitterNumber(), is("submitter"));
        assertThat(place.getSinkName(), is("sink"));
    }

    @Test
    public void instantiateWithUrl_instantiateWithoutId_idEmpty() {
        Place place = new Place(":submitter:sink");

        assertThat(place.getJobId(), is(""));
        assertThat(place.getSubmitterNumber(), is("submitter"));
        assertThat(place.getSinkName(), is("sink"));
    }

    @Test
    public void instantiateWithUrl_instantiateWithoutSubmitter_submitterEmpty() {
        Place place = new Place("id::sink");

        assertThat(place.getJobId(), is("id"));
        assertThat(place.getSubmitterNumber(), is(""));
        assertThat(place.getSinkName(), is("sink"));
    }

    @Test
    public void instantiateWithUrl_instantiateWithoutSink_sinkEmpty() {
        Place place = new Place("id:submitter:");

        assertThat(place.getJobId(), is("id"));
        assertThat(place.getSubmitterNumber(), is("submitter"));
        assertThat(place.getSinkName(), is(""));
    }

    @Test
    public void instantiateWithUrl_instantiateWithOnlyTwoPars_sinkEmpty() {
        Place place = new Place("id:submitter");

        assertThat(place.getJobId(), is("id"));
        assertThat(place.getSubmitterNumber(), is("submitter"));
        assertThat(place.getSinkName(), is(""));
    }

    @Test
    public void instantiateWithUrl_instantiateWithOnlyOnePar_submitterAndSinkEmpty() {
        Place place = new Place("id");

        assertThat(place.getJobId(), is("id"));
        assertThat(place.getSubmitterNumber(), is(""));
        assertThat(place.getSinkName(), is(""));
    }

    @Test
    public void instantiate_instantiateLegalPars_ok() {
        Place place = new Place("iD", "sUbmitter", "sInk", "1" , "0", "0");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk"));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateEmptyId_emptyId() {
        Place place = new Place("", "sUbmitter", "sInk", "1" , "0", "0");

        assertThat(place.getJobId(), is(""));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk"));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateEmptySubmitter_emptySubmitter() {
        Place place = new Place("iD", "", "sInk", "1" , "0", "0");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is(""));
        assertThat(place.getSinkName(), is("sInk"));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateEmptySink_emptySink() {
        Place place = new Place("iD", "sUbmitter", "", "1" , "0", "0");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is(""));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateEmptyItemCounter_emptyItemCounter() {
        Place place = new Place("iD", "sUbmitter", "", "" , "0", "0");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is(""));
        assertThat(place.getItemCounter(), is(""));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateEmptyFailedItemCounter_emptyFailedItemCounter() {
        Place place = new Place("iD", "sUbmitter", "", "1" , "", "0");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is(""));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is(""));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateEmptyIgnoredItemCounter_emptyIgnoredItemCounter() {
        Place place = new Place("iD", "sUbmitter", "", "1" , "0", "");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is(""));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is(""));
    }

    @Test
    public void instantiate_instantiateIdContainsColon_idContainsColon() {
        Place place = new Place("i:D", "sUbmitter", "sInk", "1" , "0", "0");

        assertThat(place.getJobId(), is("i:D"));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk"));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateSubmitterContainsColon_submitterContainsColon() {
        Place place = new Place("iD", "sUbmi:tter", "sInk", "1" , "0", "0");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is("sUbmi:tter"));
        assertThat(place.getSinkName(), is("sInk"));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateSinkContainsColon_sinkContainsColon() {
        Place place = new Place("iD", "sUbmitter", "sInk:","1" , "0", "0");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk:"));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateItemCounterContainsColon_itemCounterContainsColon() {
        Place place = new Place("iD", "sUbmitter", "sInk","1:" , "0", "0");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk"));
        assertThat(place.getItemCounter(), is("1:"));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateFailedItemCounterContainsColon_failedItemCounterContainsColon() {
        Place place = new Place("iD", "sUbmitter", "sInk","1" , "0:", "0");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk"));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is("0:"));
        assertThat(place.getIgnoredItemCounter(), is("0"));
    }

    @Test
    public void instantiate_instantiateIgnoredItemCounterContainsColon_ignoredItemCounterContainsColon() {
        Place place = new Place("iD", "sUbmitter", "sInk","1" , "0", "0:");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterNumber(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk"));
        assertThat(place.getItemCounter(), is("1"));
        assertThat(place.getFailedItemCounter(), is("0"));
        assertThat(place.getIgnoredItemCounter(), is("0:"));
    }


}

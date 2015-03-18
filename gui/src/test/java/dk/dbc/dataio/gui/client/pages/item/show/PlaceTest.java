
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
        assertThat(place.getSubmitterName(), is("submitter"));
        assertThat(place.getSinkName(), is("sink"));
    }

    @Test
    public void instantiateWithUrl_instantiateWithoutId_idEmpty() {
        Place place = new Place(":submitter:sink");

        assertThat(place.getJobId(), is(""));
        assertThat(place.getSubmitterName(), is("submitter"));
        assertThat(place.getSinkName(), is("sink"));
    }

    @Test
    public void instantiateWithUrl_instantiateWithoutSubmitter_submitterEmpty() {
        Place place = new Place("id::sink");

        assertThat(place.getJobId(), is("id"));
        assertThat(place.getSubmitterName(), is(""));
        assertThat(place.getSinkName(), is("sink"));
    }

    @Test
    public void instantiateWithUrl_instantiateWithoutSink_sinkEmpty() {
        Place place = new Place("id:submitter:");

        assertThat(place.getJobId(), is("id"));
        assertThat(place.getSubmitterName(), is("submitter"));
        assertThat(place.getSinkName(), is(""));
    }

    @Test
    public void instantiateWithUrl_instantiateWithOnlyTwoPars_sinkEmpty() {
        Place place = new Place("id:submitter");

        assertThat(place.getJobId(), is("id"));
        assertThat(place.getSubmitterName(), is("submitter"));
        assertThat(place.getSinkName(), is(""));
    }

    @Test
    public void instantiateWithUrl_instantiateWithOnlyOnePar_submitterAndSinkEmpty() {
        Place place = new Place("id");

        assertThat(place.getJobId(), is("id"));
        assertThat(place.getSubmitterName(), is(""));
        assertThat(place.getSinkName(), is(""));
    }

    @Test
    public void instantiateWithIdSubmitterAndSink_instantiateLegalPars_ok() {
        Place place = new Place("iD", "sUbmitter", "sInk");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterName(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk"));
    }

    @Test
    public void instantiateWithIdSubmitterAndSink_instantiateEmptyId_emptyId() {
        Place place = new Place("", "sUbmitter", "sInk");

        assertThat(place.getJobId(), is(""));
        assertThat(place.getSubmitterName(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk"));
    }

    @Test
    public void instantiateWithIdSubmitterAndSink_instantiateEmptySubmitter_emptySubmitter() {
        Place place = new Place("iD", "", "sInk");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterName(), is(""));
        assertThat(place.getSinkName(), is("sInk"));
    }

    @Test
    public void instantiateWithIdSubmitterAndSink_instantiateEmptySink_emptySink() {
        Place place = new Place("iD", "sUbmitter", "");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterName(), is("sUbmitter"));
        assertThat(place.getSinkName(), is(""));
    }

    @Test
    public void instantiateWithIdSubmitterAndSink_instantiateIdContainsColon_idContainsColon() {
        Place place = new Place("i:D", "sUbmitter", "sInk");

        assertThat(place.getJobId(), is("i:D"));
        assertThat(place.getSubmitterName(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk"));
    }

    @Test
    public void instantiateWithIdSubmitterAndSink_instantiateSubmitterContainsColon_submitterContainsColon() {
        Place place = new Place("iD", "sUbmi:tter", "sInk");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterName(), is("sUbmi:tter"));
        assertThat(place.getSinkName(), is("sInk"));
    }

    @Test
    public void instantiateWithIdSubmitterAndSink_instantiateSinkContainsColon_sinkContainsColon() {
        Place place = new Place("iD", "sUbmitter", "sInk:");

        assertThat(place.getJobId(), is("iD"));
        assertThat(place.getSubmitterName(), is("sUbmitter"));
        assertThat(place.getSinkName(), is("sInk:"));
    }

}

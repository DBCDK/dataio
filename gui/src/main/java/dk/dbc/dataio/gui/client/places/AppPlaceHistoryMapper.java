package dk.dbc.dataio.gui.client.places;

import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowPlace;
import dk.dbc.dataio.gui.client.pages.sinkcreate.SinkCreatePlace;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowPlace;
import dk.dbc.dataio.gui.client.pages.submittercreate.SubmitterCreatePlace;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreate.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowPlace;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

@WithTokenizers({
    FlowCreatePlace.Tokenizer.class,
    FlowComponentCreatePlace.Tokenizer.class,
    SubmitterCreatePlace.Tokenizer.class,
    FlowbinderCreatePlace.Tokenizer.class,
    SinkCreatePlace.Tokenizer.class,
    FlowComponentsShowPlace.Tokenizer.class,
    FlowsShowPlace.Tokenizer.class,
    SubmittersShowPlace.Tokenizer.class,
    JobsShowPlace.Tokenizer.class,
    SinksShowPlace.Tokenizer.class,
})

public interface AppPlaceHistoryMapper extends PlaceHistoryMapper
{
}


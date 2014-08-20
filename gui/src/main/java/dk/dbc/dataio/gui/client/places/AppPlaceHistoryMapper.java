package dk.dbc.dataio.gui.client.places;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentEditPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowPlace;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowPlace;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkCreatePlace;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkEditPlace;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowPlace;
import dk.dbc.dataio.gui.client.pages.submittermodify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowPlace;

@WithTokenizers({
    FlowCreatePlace.Tokenizer.class,
    FlowComponentCreatePlace.Tokenizer.class,
    FlowComponentEditPlace.Tokenizer.class,
    CreatePlace.Tokenizer.class,
    FlowbinderCreatePlace.Tokenizer.class,
    SinkCreatePlace.Tokenizer.class,
    SinkEditPlace.Tokenizer.class,
    FlowComponentsShowPlace.Tokenizer.class,
    FlowsShowPlace.Tokenizer.class,
    SubmittersShowPlace.Tokenizer.class,
    JobsShowPlace.Tokenizer.class,
    SinksShowPlace.Tokenizer.class,
    FlowBindersShowPlace.Tokenizer.class
})

public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {
}


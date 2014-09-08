package dk.dbc.dataio.gui.client.places;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import dk.dbc.dataio.gui.client.pages.flowbinder.flowbindercreate.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowbinder.flowbindersshow.FlowBindersShowPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.flowcomponentcreateedit.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.flowcomponentcreateedit.FlowComponentEditPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.flowcomponentsshow.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.pages.flow.flowcreate.FlowCreatePlace;
import dk.dbc.dataio.gui.client.pages.flow.flowsshow.FlowsShowPlace;
import dk.dbc.dataio.gui.client.pages.job.jobsshow.JobsShowPlace;
import dk.dbc.dataio.gui.client.pages.sink.sinksshow.SinksShowPlace;
import dk.dbc.dataio.gui.client.pages.submitter.submittermodify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.submitter.submittermodify.EditPlace;
import dk.dbc.dataio.gui.client.pages.submitter.submittersshow.SubmittersShowPlace;

@WithTokenizers({
    FlowCreatePlace.Tokenizer.class,
    FlowComponentCreatePlace.Tokenizer.class,
    FlowComponentEditPlace.Tokenizer.class,
    CreatePlace.Tokenizer.class,
    EditPlace.Tokenizer.class,
    FlowbinderCreatePlace.Tokenizer.class,
    dk.dbc.dataio.gui.client.pages.sink.sinkmodify.CreatePlace.Tokenizer.class,
    dk.dbc.dataio.gui.client.pages.sink.sinkmodify.EditPlace.Tokenizer.class,
    FlowComponentsShowPlace.Tokenizer.class,
    FlowsShowPlace.Tokenizer.class,
    SubmittersShowPlace.Tokenizer.class,
    JobsShowPlace.Tokenizer.class,
    SinksShowPlace.Tokenizer.class,
    FlowBindersShowPlace.Tokenizer.class
})

public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {
}


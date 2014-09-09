package dk.dbc.dataio.gui.client.places;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowbinder.show.FlowBindersShowPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentEditPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.show.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowCreatePlace;
import dk.dbc.dataio.gui.client.pages.flow.show.FlowsShowPlace;
import dk.dbc.dataio.gui.client.pages.job.show.JobsShowPlace;
import dk.dbc.dataio.gui.client.pages.sink.show.SinksShowPlace;
import dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace;
import dk.dbc.dataio.gui.client.pages.submitter.show.SubmittersShowPlace;

@WithTokenizers({
    FlowCreatePlace.Tokenizer.class,
    FlowComponentCreatePlace.Tokenizer.class,
    FlowComponentEditPlace.Tokenizer.class,
    CreatePlace.Tokenizer.class,
    EditPlace.Tokenizer.class,
    FlowbinderCreatePlace.Tokenizer.class,
    dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace.Tokenizer.class,
    dk.dbc.dataio.gui.client.pages.sink.modify.EditPlace.Tokenizer.class,
    FlowComponentsShowPlace.Tokenizer.class,
    FlowsShowPlace.Tokenizer.class,
    SubmittersShowPlace.Tokenizer.class,
    JobsShowPlace.Tokenizer.class,
    SinksShowPlace.Tokenizer.class,
    FlowBindersShowPlace.Tokenizer.class
})

public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {
}


package dk.dbc.dataio.gui.client.places;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

@WithTokenizers({
        dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flow.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentCreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentEditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowbinder.modify.FlowbinderCreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.sink.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.show.FlowComponentsShowPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flow.show.FlowsShowPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.submitter.show.SubmittersShowPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.show.JobsShowPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.sink.show.SinksShowPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowbinder.show.FlowBindersShowPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.faileditems.ShowPlace.Tokenizer.class
})

public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {
}


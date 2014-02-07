package dk.dbc.dataio.gui.client.places;

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


package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class EditPlace extends AbstractBasePlace {
    private Long flowId;

    public EditPlace(String url) {
        this.flowId = Long.valueOf(url);
    }

    public EditPlace(FlowModel model) {
        this.flowId = model.getId();
    }

    public Long getFlowId() {
        return flowId;
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterEditImpl(this, clientFactory.getPlaceController(), commonInjector.getMenuTexts().menu_FlowEdit());
    }

    @Prefix("EditFlow")
    public static class Tokenizer implements PlaceTokenizer<EditPlace> {
        @Override
        public String getToken(EditPlace place) {
            return String.valueOf(place.getFlowId());
        }

        @Override
        public EditPlace getPlace(String token) {
            return new EditPlace(token);
        }
    }
}

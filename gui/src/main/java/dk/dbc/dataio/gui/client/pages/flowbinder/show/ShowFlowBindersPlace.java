package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class ShowFlowBindersPlace extends AbstractBasePlace {
    public ShowFlowBindersPlace() { super(); }
    public ShowFlowBindersPlace(String token) { super((token));}

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        presenter = new PresenterImpl(
                clientFactory.getPlaceController(),
                clientFactory.getGlobalViewsFactory().getFlowBindersView(),
                commonInjector.getMenuTexts().menu_FlowBinders());
        return presenter;
    }

    @Prefix("ShowFlowBinders")
    public static class Tokenizer implements PlaceTokenizer<ShowFlowBindersPlace> {

        @Override
        public String getToken(ShowFlowBindersPlace place) { return place.getToken(); }

        @Override
        public ShowFlowBindersPlace getPlace(String token) {return new ShowFlowBindersPlace(token); }
    }
}

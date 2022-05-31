package dk.dbc.dataio.gui.client.places;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.LegacyHandlerWrapper;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.user.client.History;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import java.util.logging.Logger;

/**
 * DataIO version of the GWT PlaceHistoryHandler class.
 * This version assures, that upon initialization, the correct token is extracted from the DefaultPlace, and stored in the Historian class.
 * The purpose of this is to be sure to display the complete URL upons startup, thereby allowing direct changes (from eg. job filters) to be reflected in the URL.
 */

/**
 * Monitors {@link PlaceChangeEvent}s and
 * {@link com.google.gwt.user.client.History} events and keep them in sync.
 */
public class DioPlaceHistoryHandler {
    private static final Logger log = Logger.getLogger(DioPlaceHistoryHandler.class.getName());

    /**
     * Default implementation of {@link DioPlaceHistoryHandler.Historian}, based on {@link History}.
     */
    public static class DefaultHistorian implements DioPlaceHistoryHandler.Historian {
        public com.google.gwt.event.shared.HandlerRegistration addValueChangeHandler(
                ValueChangeHandler<String> valueChangeHandler) {
            return History.addValueChangeHandler(valueChangeHandler);
        }

        public String getToken() {
            return History.getToken();
        }

        public void newItem(String token, boolean issueEvent) {
            History.newItem(token, issueEvent);
        }
    }

    /**
     * Optional delegate in charge of History related events. Provides nice
     * isolation for unit testing, and allows pre- or post-processing of tokens.
     * Methods correspond to the like named methods on {@link History}.
     */
    public interface Historian {
        /**
         * Adds a {@link com.google.gwt.event.logical.shared.ValueChangeEvent}
         * handler to be informed of changes to the browser's history stack.
         *
         * @param valueChangeHandler the handler
         * @return the registration used to remove this value change handler
         */
        com.google.gwt.event.shared.HandlerRegistration addValueChangeHandler(
                ValueChangeHandler<String> valueChangeHandler);

        /**
         * @return the current history token.
         */
        String getToken();

        /**
         * Adds a new browser history entry. Calling this method will cause
         * {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
         * to be called as well.
         *
         * @param token      The token
         * @param issueEvent A boolean determining whether to issue an event
         */
        void newItem(String token, boolean issueEvent);
    }

    private final DioPlaceHistoryHandler.Historian historian;

    private final PlaceHistoryMapper mapper;

    private PlaceController placeController;

    private Place defaultPlace = Place.NOWHERE;

    /**
     * Create a new PlaceHistoryHandler with a {@link DioPlaceHistoryHandler.DefaultHistorian}. The
     * DefaultHistorian is created via a call to GWT.create(), so an alternative
     * default implementation can be provided through &lt;replace-with&gt; rules
     * in a {@code gwt.xml} file.
     *
     * @param mapper a {@link PlaceHistoryMapper} instance
     */
    public DioPlaceHistoryHandler(PlaceHistoryMapper mapper) {
        this(mapper, (DioPlaceHistoryHandler.Historian) GWT.create(DioPlaceHistoryHandler.DefaultHistorian.class));
    }

    /**
     * Create a new PlaceHistoryHandler.
     *
     * @param mapper    a {@link PlaceHistoryMapper} instance
     * @param historian a {@link DioPlaceHistoryHandler.Historian} instance
     */
    public DioPlaceHistoryHandler(PlaceHistoryMapper mapper, DioPlaceHistoryHandler.Historian historian) {
        this.mapper = mapper;
        this.historian = historian;
    }

    /**
     * Handle the current history token. Typically called at application start, to
     * ensure bookmark launches work.
     */
    public void handleCurrentHistory() {
        handleHistoryToken(historian.getToken());
    }

    /**
     * Legacy method tied to the old location for {@link EventBus}.
     *
     * @param placeController The Placecontroller
     * @param eventBus        The Eventbus
     * @param defaultPlace    The default place
     * @return A Handler Registration Object
     * @deprecated use {@link #register(PlaceController, EventBus, Place)}
     */
    @Deprecated
    public com.google.gwt.event.shared.HandlerRegistration register(PlaceController placeController,
                                                                    com.google.gwt.event.shared.EventBus eventBus, Place defaultPlace) {
        return new LegacyHandlerWrapper(register(placeController, (EventBus) eventBus, defaultPlace));
    }

    /**
     * Initialize this place history handler.
     * <p>
     * Returns a registration object to de-register the handler
     *
     * @param placeController The Placecontroller
     * @param eventBus        The Eventbus
     * @param defaultPlace    The default place
     * @return A Handler Registration Object
     */
    public HandlerRegistration register(PlaceController placeController, EventBus eventBus,
                                        Place defaultPlace) {
        this.placeController = placeController;
        this.defaultPlace = defaultPlace;

        final HandlerRegistration placeReg =
                eventBus.addHandler(PlaceChangeEvent.TYPE, event -> {
                    AbstractBasePlace newPlace = (AbstractBasePlace) event.getNewPlace();
                    historian.newItem(tokenForPlace(newPlace), false);
                });

        final HandlerRegistration historyReg =
                historian.addValueChangeHandler(event -> {
                    String token = event.getValue();
                    handleHistoryToken(token);
                });

        return () -> {
            this.defaultPlace = Place.NOWHERE;
            this.placeController = null;
            placeReg.removeHandler();
            historyReg.removeHandler();
        };
    }

    /**
     * Visible for testing.
     */
    Logger log() {
        return log;
    }

    private void handleHistoryToken(String token) {
        Place newPlace = null;

        if ("".equals(token)) {
            newPlace = defaultPlace;
        }

        if (newPlace == null) {
            newPlace = mapper.getPlace(token);
        }

        if (newPlace == null) {
            log().warning("Unrecognized history token: " + token);
            newPlace = defaultPlace;
        }

        placeController.goTo(newPlace);
    }

    private String tokenForPlace(AbstractBasePlace newPlace) {
        String token = mapper.getToken(newPlace);
        if (token != null) {
            return token;
        }

        log().warning("Place not mapped to a token: " + newPlace);
        return "";
    }

}

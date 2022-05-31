package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.FlowBinderIdent;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.util.Utilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class represents the show submitters presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    private final PlaceController placeController;
    private final String header;

    View view;

    /**
     * Default constructor
     *
     * @param placeController PlaceController for navigation
     * @param view            Global submitters View, necessary for keeping filter state, etc.
     * @param header          Breadcrumb header text
     */
    public PresenterImpl(PlaceController placeController, View view, String header) {
        this.placeController = placeController;
        this.view = view;
        this.header = header;
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        final AbstractBasePlace place = (AbstractBasePlace) placeController.getWhere();
        view.setPresenter(this);
        view.setHeader(this.header);
        view.submitterFilter.setPlace(place);
        containerWidget.setWidget(view.asWidget());
        fetchSubmitters();
    }

    /**
     * This method shows a popup window containing the list of attached Flowbinders
     *
     * @param model The model for the submitter to edit
     */
    @Override
    public void showFlowBinders(SubmitterModel model) {
        if (model != null) {
            commonInjector.getFlowStoreProxyAsync().getFlowBindersForSubmitter(model.getId(), new GetFlowBindersForSubmitterCallback());
        }
    }

    /**
     * This method opens a new view, for editing the submitter in question
     *
     * @param model The model for the submitter to edit
     */
    @Override
    public void editSubmitter(SubmitterModel model) {
        if (model != null) {
            placeController.goTo(new EditPlace(model));
        }
    }

    /**
     * This method opens a new view, for creating a new submitter
     */
    @Override
    public void createSubmitter() {
        view.selectionModel.clear();
        placeController.goTo(new CreatePlace());
    }

    /**
     * Copies the list of flowbinders to the Clipboard
     *
     * @param flowBinders The list of flowbinders
     */
    @Override
    public void copyFlowBinderListToClipboard(Map<String, String> flowBinders) {
        String clipboardContent = "";
        if (flowBinders != null && !flowBinders.isEmpty()) {
            for (String flowBinder : flowBinders.values()) {
                clipboardContent += clipboardContent.isEmpty() ? flowBinder : "\n" + flowBinder;
            }
        }
        Utilities.copyTextToClipboard(clipboardContent);
    }

    /**
     * This method fetches submitters and sends them to the view
     */
    private void fetchSubmitters() {
        view.dataProvider.getList().clear();
        final List<GwtQueryClause> clauses = view.submitterFilter.getValue();
        if (clauses.isEmpty()) {
            commonInjector.getFlowStoreProxyAsync().findAllSubmitters(new FetchSubmittersCallback());
        } else {
            commonInjector.getFlowStoreProxyAsync().querySubmitters(clauses, new FetchSubmittersCallback());
        }
    }

    /**
     * This method deciphers if a submitter has been added, updated or deleted.
     * The view and selection model are updated accordingly
     *
     * @param models the list of submitters returned from flow store proxy
     */
    private void setSubmittersAndDecipherSelection(Set<SubmitterModel> dataProviderSet, List<SubmitterModel> models) {
        if (dataProviderSet.size() > models.size() || dataProviderSet.size() == 0) {
            view.selectionModel.clear();
            view.setSubmitters(models);
        } else {
            for (SubmitterModel current : models) {
                if (!dataProviderSet.contains(current)) {
                    view.setSubmitters(models);
                    view.selectionModel.setSelected(current, true);
                    break;
                }
            }
        }
    }

    /**
     * This class is the callback class for the findAllSubmitters method in the Flow Store
     */
    protected class FetchSubmittersCallback extends FilteredAsyncCallback<List<SubmitterModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<SubmitterModel> models) {
            setSubmittersAndDecipherSelection(new HashSet<>(view.dataProvider.getList()), models);
        }
    }

    /**
     * This class is the callback class for the getFlowBindersForSubmitter method in the Flow Store
     */
    protected class GetFlowBindersForSubmitterCallback implements AsyncCallback<List<FlowBinderIdent>> {
        @Override
        public void onFailure(Throwable caught) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(caught, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<FlowBinderIdent> flowBinderIdents) {
            List<FlowBinderModel> flowBinderModels = new ArrayList<>();
            if (flowBinderIdents != null) {
                if (flowBinderIdents.isEmpty()) {
                    Window.alert(viewInjector.getTexts().error_NoFlowBinders());
                } else {
                    for (FlowBinderIdent flowBinderIdent : flowBinderIdents) {
                        FlowBinderModel flowBinderModel = new FlowBinderModel();
                        flowBinderModel.setId(flowBinderIdent.getFlowBinderId());
                        flowBinderModel.setName(flowBinderIdent.getFlowBinderName());
                        flowBinderModels.add(flowBinderModel);
                    }
                    view.showFlowBinders(flowBinderModels);
                }
            }
        }
    }
}

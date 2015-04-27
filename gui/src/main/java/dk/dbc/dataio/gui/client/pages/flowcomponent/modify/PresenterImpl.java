package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents the modify flow component activity encompassing saving/updating
 * an existing flow component in the flow store via RPC proxy
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    protected Texts texts;
    protected FlowStoreProxyAsync flowStoreProxy;
    protected JavaScriptProjectFetcherAsync javaScriptProjectFetcher;
    protected View view;
    protected FlowComponentModel model = new FlowComponentModel();
    protected List<String> availableRevisions = new ArrayList<String>();
    protected List<String> availableScripts = new ArrayList<String>();
    protected List<String> availableInvocationMethods = new ArrayList<String>();

    private static boolean isInitialPopulationOfView;

    /**
     * Constructor
     * Please note, that in the constructor, view has NOT been initialized and can therefore not be used
     * Put code, utilizing view in the start method.
     * @param clientFactory, clientFactory
     */
    public PresenterImpl(ClientFactory clientFactory) {
        texts = clientFactory.getFlowComponentModifyTexts();
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        javaScriptProjectFetcher = clientFactory.getJavaScriptProjectFetcherAsync();
        isInitialPopulationOfView = true;
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter.
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        initializeViewFields();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        initializeModel();
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     * @param name, the new name value
     */
    @Override
    public void nameChanged(String name) {
        model.setName(name);
    }

    /**
     * A signal to the presenter, saying that the svn project field has been changed
     * @param projectName, the new svn project name value
     */
    @Override
    public void projectChanged(String projectName) {
        model.setSvnProject(projectName);
        view.busy.setVisible(true);
        view.revision.setEnabled(false);
        view.script.setEnabled(false);
        view.method.setEnabled(false);
        fetchAvailableRevisions(projectName);
    }

    /**
     * A signal to the presenter, saying that the svn revision field has been changed
     * @param selectedRevision, the new svn project name value
     */
    @Override
    public void revisionChanged(String selectedRevision) {
        model.setSvnRevision(selectedRevision == null ? "" : selectedRevision);
        view.busy.setVisible(true);
        view.script.setEnabled(false);
        view.method.setEnabled(false);
        fetchAvailableScripts(model.getSvnProject(), Long.valueOf(model.getSvnRevision()));
    }

    /**
     * A signal to the presenter, saying that the java script name has been changed
     * @param selectedScript, the new java script name value
     */
    @Override
    public void scriptNameChanged(String selectedScript) {
        model.setInvocationJavascript(selectedScript == null ? "" : selectedScript);
        view.busy.setVisible(true);
        view.method.setEnabled(false);
        fetchAvailableInvocationMethods(model.getSvnProject(), Long.valueOf(model.getSvnRevision()), model.getInvocationJavascript());
    }

    /**
     * A signal to the presenter, saying that the invocation method has been changed
     * @param selectedInvocationMethod, the new invocation method value
     */
    @Override
    public void invocationMethodChanged(String selectedInvocationMethod) {
        model.setInvocationMethod(selectedInvocationMethod == null ? "" : selectedInvocationMethod);
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    @Override
    public void keyPressed() {
        view.status.setText("");
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (model.isInputFieldsEmptyModulesExcluded()) {
            view.setErrorText(texts.error_InputFieldValidationError());
        } else {
            view.status.setText(texts.status_SavingFlowComponent());
            saveModel();
        }
    }

    /*
     * Local classes
     */

    /**
     * Local call back class to be instantiated in the call to fetchRevisions in javaScriptProjectFetcherProxy
     */
    class FetchRevisionsFilteredAsyncCallback extends FilteredAsyncCallback<List<RevisionInfo>> {

        @Override
        public void onFilteredFailure(Throwable e) {
            onFailureSendExceptionToView(e);
        }
        @Override
        public void onSuccess(List<RevisionInfo> revisionInfoList) {
            setAvailableRevisions(revisionInfoList);
        }
    }

    /**
     * Local call back class to be instantiated in the call to fetchJavaScriptFileNames in javaScriptProjectFetcherProxy
     */
    class FetchScriptsFilteredAsyncCallback extends FilteredAsyncCallback<List<String>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            onFailureSendExceptionToView(e);
            view.revision.setEnabled(true);
            view.script.setEnabled(true);
            setAvailableScripts(new ArrayList<String>());
            setAvailableInvocationMethods(new ArrayList<String>());
            model.setInvocationJavascript("");
            model.setInvocationMethod("");
        }
        @Override
        public void onSuccess(List<String> scriptNames) {
            setAvailableScripts(scriptNames);
        }
    }

    /**
     * Local call back class to be instantiated in the call to fetchJavaScriptInvocationMethods in javaScriptProjectFetcherProxy
     */
    class FetchInvocationMethodsFilteredAsyncCallback extends FilteredAsyncCallback<List<String>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            onFailureSendExceptionToView(e);
            view.revision.setEnabled(true);
            view.script.setEnabled(true);
            view.method.setEnabled(true);
            setAvailableInvocationMethods(new ArrayList<String>());
            model.setInvocationMethod("");
        }
        @Override
        public void onSuccess(List<String> invocationMethods) {
            setAvailableInvocationMethods(invocationMethods);
        }
    }

    /**
     * Local call back class to be instantiated in the call to createFlowComponent or updateFlowComponent in flow store proxy
     */
    class SaveFlowComponentModelFilteredAsyncCallback extends FilteredAsyncCallback<FlowComponentModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            onFailureSendExceptionToView(e);
        }

        @Override
        public void onSuccess(FlowComponentModel flowComponentModel) {
            view.status.setText(texts.status_FlowComponentSuccessfullySaved());
            setFlowComponentModel(flowComponentModel);
        }
    }

    /*
     * Protected methods
     */

    /**
     * Method setting the list of available revisions.
     *
     * When modifying a flow component, a change event is fired in order to:
     * Load and display the available revisions, scripts and invocation methods as well as the actual
     * revision, script name and invocation method for the flow component.
     * (Default chosen is the first of each available).
     *
     * For CREATE: The event is fired -> when the user has typed in a valid svn project.
     * For EDIT  : The event is fired -> when populating the svn project view field with the existing
     * svn project value.
     *
     * The user should be able to see the existing values before editing.
     * Therefore: The change event will not result in a reset of project dependent model values when
     * the view is populated for the very first time.
     *
     * Any change of the svn project, EXCEPT the initial one, will result in a reset of all project
     * dependent model values.
     *
     * @param revisionInfoList containing the available revisions
     */
    protected void setAvailableRevisions(List<RevisionInfo> revisionInfoList) {
        if(isInitialPopulationOfView) {
            isInitialPopulationOfView = false;
        } else {
            resetProjectDependentModelValues();
        }
        availableRevisions.clear();
        for(RevisionInfo revisionInfo : revisionInfoList) {
            availableRevisions.add(Long.toString(revisionInfo.getRevision()));
        }
        view.revision.setAvailableItems(availableRevisions);
        view.revision.setSelectedItem(model.getSvnRevision());
        view.revision.fireChangeEvent();
    }

    /**
     * Method setting the list of available script names
     * @param scriptNames containing the available script names
     */
    protected void setAvailableScripts(List<String> scriptNames) {
        availableScripts = scriptNames;
        view.script.setAvailableItems(scriptNames);
        view.script.setSelectedItem(model.getInvocationJavascript());
        view.script.fireChangeEvent();
    }

    /**
     * Method setting the list of available invocation methods
     * @param invocationMethods containing the available invocation methods
     */
    protected void setAvailableInvocationMethods(List<String> invocationMethods) {
        availableInvocationMethods = invocationMethods;
        view.method.setAvailableItems(invocationMethods);
        view.method.setSelectedItem(model.getInvocationMethod());
        view.method.fireChangeEvent();
        view.revision.setEnabled(true);
        view.script.setEnabled(true);
        view.method.setEnabled(true);
        view.busy.setVisible(false);
    }

    /**
     * Method used to set the model after a successful update or a save
     * @param model The model to save
     */
    protected void setFlowComponentModel(FlowComponentModel model) {
        this.model = model;
    }

    /**
     * Method populating the view fields.
     * if the action is create, the view will be fully populated once the user
     * has typed in the desired svn project.
     * if the action is edit, a change event is fired through the method: projectChanged
     * in order to fully populate the view.
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        view.name.setText(model.getName());
        view.name.setEnabled(true);
        view.project.setText(model.getSvnProject());
        view.project.setEnabled(true);
        if(model.getId() != 0) {
            projectChanged(model.getSvnProject());
        }
    }

    /*
    * Private methods
    */

    /**
     * Method used to set the initial state of the fields in the view
     */
    private void initializeViewFields() {
        view.name.clearText();
        view.name.setEnabled(false);
        view.project.clearText();
        view.project.setEnabled(false);
        view.revision.clear();
        view.revision.setEnabled(false);
        view.script.clear();
        view.script.setEnabled(false);
        view.method.clear();
        view.method.setEnabled(false);
        view.busy.setVisible(false);
        view.busy.setVisible(false);
        view.status.setText("");
    }

    /**
     * Method used to reset the model values for svn revision, invocation java script
     * and invocation method to default
     */
    private void resetProjectDependentModelValues() {
        model.setSvnRevision("");
        model.setInvocationJavascript("");
        model.setInvocationMethod("");
    }

    /**
     * Method translating a JavaScriptProjectFetcherException to an error string specified in the texts for flow component modify
     * @param e the exception to translate
     */
    private void translateJavaScriptProjectFetcherError(Throwable e) {
        JavaScriptProjectFetcherError errorCode = ((JavaScriptProjectFetcherException) e).getErrorCode();
        switch (errorCode) {
            case SCM_RESOURCE_NOT_FOUND: view.setErrorText(texts.error_ScmProjectNotFoundError());
                break;
            case SCM_ILLEGAL_PROJECT_NAME: view.setErrorText(texts.error_ScmIllegalProjectNameError());
                break;
            case JAVASCRIPT_REFERENCE_ERROR: view.setErrorText(texts.error_JavaScriptReferenceError());
                break;
            default: view.setErrorText(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * Method decoding which error message to display to the user
     * @param e the exception previously thrown
     */
    private void onFailureSendExceptionToView(Throwable e) {
        view.busy.setVisible(false);
        if(e instanceof JavaScriptProjectFetcherException) {
            translateJavaScriptProjectFetcherError(e);
        } else {
            view.setErrorText(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * Method retrieving available revisions for a given svn project.
     * @param projectName the svn project name value
     */
    void fetchAvailableRevisions(String projectName) {
        javaScriptProjectFetcher.fetchRevisions(projectName, new FetchRevisionsFilteredAsyncCallback());
    }

    /**
     * Method retrieving available scripts for a given svn project with a given revision.
     * @param projectName the svn project name value
     * @param revision the given revision
     */
    void fetchAvailableScripts(String projectName, long revision) {
        javaScriptProjectFetcher.fetchJavaScriptFileNames(projectName, revision, new FetchScriptsFilteredAsyncCallback());
    }

    /**
     * Method retrieving all available java script invocation methods for a given svn project with
     * a given revision and a chosen java script.
     * @param projectName the svn project name value
     * @param revision the given revision
     * @param scriptName the chosen script
     */
    void fetchAvailableInvocationMethods(String projectName, long revision, String scriptName) {
        javaScriptProjectFetcher.fetchJavaScriptInvocationMethods(projectName, revision, scriptName, new FetchInvocationMethodsFilteredAsyncCallback());
    }

     /*
     * Abstract methods
     */

    /**
     * getModel
     */
    abstract void initializeModel();

    /**
     * saveModel
     */
    abstract void saveModel();
}

package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents the modify flow component activity encompassing saving/updating
 * an existing flow component in the flow store via RPC proxy
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {

    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    protected FlowComponentModel model = new FlowComponentModel();
    protected List<String> availableRevisions = new ArrayList<String>();
    protected List<String> availableNextRevisions = new ArrayList<String>();
    protected List<String> availableScripts = new ArrayList<String>();
    protected List<String> availableInvocationMethods = new ArrayList<String>();

    private static boolean isInitialPopulationOfView;
    private String header;

    /**
     * Constructor
     * Please note, that in the constructor, view has NOT been initialized and can therefore not be used
     * Put code, utilizing view in the start method.
     *
     * @param header Breadcrumb header text
     */
    public PresenterImpl(String header) {
        isInitialPopulationOfView = true;
        this.header = header;
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter.
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        getView().setPresenter(this);
        getView().setHeader(this.header);
        initializeViewFields();
        containerWidget.setWidget(getView().asWidget());
        initializeModel();
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     *
     * @param name, the new name value
     */
    @Override
    public void nameChanged(String name) {
        model.setName(name);
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     *
     * @param description, the new description value
     */
    public void descriptionChanged(String description) {
        model.setDescription(description);
    }

    /**
     * A signal to the presenter, saying that the svn project field has been changed
     *
     * @param projectName, the new svn project name value
     */
    @Override
    public void projectChanged(String projectName) {
        model.setSvnProject(projectName);
        View view = getView();
        view.busy.setVisible(true);
        view.revision.setEnabled(false);
        view.next.setEnabled(false);
        view.script.setEnabled(false);
        view.method.setEnabled(false);
        fetchAvailableRevisions(projectName);
    }

    /**
     * A signal to the presenter, saying that the svn revision field has been changed
     *
     * @param selectedRevision, the new svn revision value
     */
    @Override
    public void revisionChanged(String selectedRevision) {
        model.setSvnRevision(selectedRevision == null ? "" : selectedRevision);
        View view = getView();
        view.busy.setVisible(true);
        view.script.setEnabled(false);
        view.method.setEnabled(false);
        fetchAvailableScripts(model.getSvnProject(), Long.valueOf(model.getSvnRevision()));
    }

    /**
     * A signal to the presenter, saying that the svn next revision field has been changed
     *
     * @param selectedNext, the new next svn revision value
     */
    @Override
    public void nextChanged(String selectedNext) {
        model.setSvnNext(selectedNext);
    }

    /**
     * A signal to the presenter, saying that the java script name has been changed
     *
     * @param selectedScript, the new java script name value
     */
    @Override
    public void scriptNameChanged(String selectedScript) {
        model.setInvocationJavascript(selectedScript == null ? "" : selectedScript);
        getView().busy.setVisible(true);
        getView().method.setEnabled(false);
        fetchAvailableInvocationMethods(model.getSvnProject(), Long.valueOf(model.getSvnRevision()), model.getInvocationJavascript());
    }

    /**
     * A signal to the presenter, saying that the invocation method has been changed
     *
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
        getView().status.setText("");
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (model.isInputFieldsEmptyModulesExcluded()) {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        } else if (!model.getDataioPatternMatches().isEmpty()) {
            getView().setErrorText(getTexts().error_NameFormatValidationError());
        } else {
            getView().status.setText(getTexts().status_SavingFlowComponent());
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
            getView().revision.setEnabled(true);
            getView().next.setEnabled(true);
            getView().script.setEnabled(true);
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
            View view = getView();
            view.revision.setEnabled(true);
            view.next.setEnabled(true);
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
            getView().status.setText(getTexts().status_FlowComponentSuccessfullySaved());
            setFlowComponentModel(flowComponentModel);
            History.back();
        }
    }

    /*
     * Protected methods
     */

    /**
     * Method setting the list of available revisions.
     * <p>
     * When modifying a flow component, a change event is fired in order to:
     * Load and display the available revisions, scripts and invocation methods as well as the actual
     * revision, script name and invocation method for the flow component.
     * (Default chosen is the first of each available).
     * <p>
     * For CREATE: The event is fired -&gt; when the user has typed in a valid svn project.
     * For EDIT  : The event is fired -&gt; when populating the svn project view field with the existing
     * svn project value.
     * <p>
     * The user should be able to see the existing values before editing.
     * Therefore: The change event will not result in a reset of project dependent model values when
     * the view is populated for the very first time.
     * <p>
     * Any change of the svn project, EXCEPT the initial one, will result in a reset of all project
     * dependent model values.
     *
     * @param revisionInfoList containing the available revisions
     */
    protected void setAvailableRevisions(List<RevisionInfo> revisionInfoList) {
        if (isInitialPopulationOfView) {
            isInitialPopulationOfView = false;
        } else {
            resetProjectDependentModelValues();
        }
        availableRevisions.clear();
        for (RevisionInfo revisionInfo : revisionInfoList) {
            availableRevisions.add(Long.toString(revisionInfo.getRevision()));
        }
        View view = getView();
        view.revision.setAvailableItems(availableRevisions);
        availableNextRevisions = new ArrayList<String>(availableRevisions);
        view.next.setAvailableItems(availableNextRevisions);
        if (!model.getSvnNext().isEmpty()) {
            view.next.setSelectedText(model.getSvnNext());
        }
        view.next.fireChangeEvent();
        view.revision.setSelectedText(model.getSvnRevision());
        view.revision.fireChangeEvent();
    }

    /**
     * Method setting the list of available script names
     *
     * @param scriptNames containing the available script names
     */
    protected void setAvailableScripts(List<String> scriptNames) {
        availableScripts = scriptNames;
        View view = getView();
        view.script.setAvailableItems(scriptNames);
        view.script.setSelectedText(model.getInvocationJavascript());
        view.script.fireChangeEvent();
    }

    /**
     * Method setting the list of available invocation methods
     *
     * @param invocationMethods containing the available invocation methods
     */
    protected void setAvailableInvocationMethods(List<String> invocationMethods) {
        availableInvocationMethods = invocationMethods;
        View view = getView();
        view.method.setAvailableItems(invocationMethods);
        view.method.setSelectedText(model.getInvocationMethod());
        view.method.fireChangeEvent();
        view.revision.setEnabled(true);
        view.next.setEnabled(true);
        view.script.setEnabled(true);
        view.method.setEnabled(true);
        view.busy.setVisible(false);
    }

    /**
     * Method used to set the model after a successful update or a save
     *
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
        View view = getView();
        view.name.setText(model.getName());
        view.name.setEnabled(true);
        view.name.setFocus(true);
        view.description.setText(model.getDescription());
        view.description.setEnabled(true);
        view.project.setText(model.getSvnProject());
        view.project.setEnabled(true);
        if (model.getId() != 0) {
            projectChanged(model.getSvnProject());
        }
    }

    /*
     * Private methods
     */

    View getView() {
        return viewInjector.getView();
    }

    Texts getTexts() {
        return viewInjector.getTexts();
    }

    /**
     * Method used to set the initial state of the fields in the view
     */
    private void initializeViewFields() {
        View view = getView();
        view.name.clearText();
        view.name.setEnabled(false);
        view.description.clearText();
        view.description.setEnabled(false);
        view.project.clearText();
        view.project.setEnabled(false);
        view.revision.clear();
        view.revision.setEnabled(false);
        view.next.clear();
        view.next.setEnabled(false);
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
        model.setSvnNext("");
        model.setInvocationJavascript("");
        model.setInvocationMethod("");
    }

    /**
     * Method translating a JavaScriptProjectFetcherException to an error string specified in the texts for flow component modify
     *
     * @param e the exception to translate
     */
    private void translateJavaScriptProjectFetcherError(Throwable e) {
        JavaScriptProjectFetcherError errorCode = ((JavaScriptProjectFetcherException) e).getErrorCode();
        switch (errorCode) {
            case SCM_RESOURCE_NOT_FOUND:
                getView().setErrorText(getTexts().error_ScmProjectNotFoundError());
                break;
            case SCM_ILLEGAL_PROJECT_NAME:
                getView().setErrorText(getTexts().error_ScmIllegalProjectNameError());
                break;
            case JAVASCRIPT_REFERENCE_ERROR:
                getView().setErrorText(getTexts().error_JavaScriptReferenceError());
                break;
            default:
                getView().setErrorText(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * Method decoding which error message to display to the user
     *
     * @param e the exception previously thrown
     */
    private void onFailureSendExceptionToView(Throwable e) {
        getView().busy.setVisible(false);
        if (e instanceof JavaScriptProjectFetcherException) {
            translateJavaScriptProjectFetcherError(e);
        } else {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), null));
        }
    }

    /**
     * Method retrieving available revisions for a given svn project.
     *
     * @param projectName the svn project name value
     */
    void fetchAvailableRevisions(String projectName) {
        commonInjector.getJavaScriptProjectFetcherAsync().fetchRevisions(projectName, new FetchRevisionsFilteredAsyncCallback());
    }

    /**
     * Method retrieving available scripts for a given svn project with a given revision.
     *
     * @param projectName the svn project name value
     * @param revision    the given revision
     */
    void fetchAvailableScripts(String projectName, long revision) {
        commonInjector.getJavaScriptProjectFetcherAsync().fetchJavaScriptFileNames(projectName, revision, new FetchScriptsFilteredAsyncCallback());
    }

    /**
     * Method retrieving all available java script invocation methods for a given svn project with
     * a given revision and a chosen java script.
     *
     * @param projectName the svn project name value
     * @param revision    the given revision
     * @param scriptName  the chosen script
     */
    void fetchAvailableInvocationMethods(String projectName, long revision, String scriptName) {
        commonInjector.getJavaScriptProjectFetcherAsync().fetchJavaScriptInvocationMethods(projectName, revision, scriptName, new FetchInvocationMethodsFilteredAsyncCallback());
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

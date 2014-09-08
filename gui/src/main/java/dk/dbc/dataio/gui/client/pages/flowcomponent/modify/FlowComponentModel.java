package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import dk.dbc.dataio.gui.client.model.GenericBackendModel;

import java.util.ArrayList;
import java.util.List;

public class FlowComponentModel extends GenericBackendModel {

    private String name;
    private String svnProject;
    private String svnRevision;
    private String invocationJavascript;
    private String invocationMethod;
    private List<String> javascriptModules;

    public FlowComponentModel(long id, long version, String name, String svnProject, String svnRevision, String invocationJavascript, String invocationMethod, List<String> javascriptModules) {
        super(id, version);
        this.name = name;
        this.svnProject = svnProject;
        this.svnRevision = svnRevision;
        this.invocationJavascript = invocationJavascript;
        this.invocationMethod = invocationMethod;
        this.javascriptModules = javascriptModules;
    }

    public FlowComponentModel() {
        super(0L, 0L);
        this.name = "";
        this.svnProject = "";
        this.svnRevision = "";
        this.invocationJavascript = "";
        this.invocationMethod = "";
        this.javascriptModules = new ArrayList<String>();
    }

    /**
     * @return name The name of the flow component
     */
    public String getName() {
        return name;
    }

    /**
     * Set flow component name
     * @param name The name of the flow component
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return name The SVN Project name
     */
    public String getSvnProject() {
        return svnProject;
    }

    /**
     * Set the SVN Project name
     * @param svnProject The SVN Project name
     */
    public void setSvnProject(String svnProject) {
        this.svnProject = svnProject;
    }

    /**
     * @return name The SVN Revision number
     */
    public String getSvnRevision() {
        return svnRevision;
    }

    /**
     * Set the SVN Revision number
     * @param svnRevision The SVN Revision number
     */
    public void setSvnRevision(String svnRevision) {
        this.svnRevision = svnRevision;
    }

    /**
     * @return name The name of the Invocation Javascript
     */
    public String getInvocationJavascript() {
        return invocationJavascript;
    }

    /**
     * Set Invocation Javascript name
     * @param invocationJavascript The name of the Invocation Javascript
     */
    public void setInvocationJavascript(String invocationJavascript) {
        this.invocationJavascript = invocationJavascript;
    }

    /**
     * @return name The name of the Invocation Method in the Javascript
     */
    public String getInvocationMethod() {
        return invocationMethod;
    }

    /**
     * Set name of the Invocation Method in the Javascript
     * @param invocationMethod The name of the Invocation Method in the Javascript
     */
    public void setInvocationMethod(String invocationMethod) {
        this.invocationMethod = invocationMethod;
    }

    /**
     * @return name A list of all Javascript modules
     */
    public List<String> getJavascriptModules() {
        return javascriptModules;
    }

    /**
     * Set the list of all Javascript modules
     * @param javascriptModules The list of all Javascript modules
     */
    public void setJavascriptModules(List<String> javascriptModules) {
        this.javascriptModules = javascriptModules;
    }

    /**
     * Checks for empty String values
     */
    public boolean isInputFieldsEmpty() {
        return name.isEmpty() ||
                svnProject.isEmpty() ||
                svnRevision.isEmpty() ||
                invocationJavascript.isEmpty() ||
                invocationMethod.isEmpty() ||
                javascriptModules.isEmpty();
    }

}

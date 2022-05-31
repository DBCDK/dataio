package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.gui.client.util.Format;

import java.util.ArrayList;
import java.util.List;

public class FlowComponentModel extends GenericBackendModel {

    private String name;
    private String svnProject;
    private String svnRevision;
    private String svnNext;
    private String invocationJavascript;
    private String invocationMethod;
    private List<String> javascriptModules;
    private List<String> nextJavascriptModules;
    private String description;

    public FlowComponentModel(long id, long version, String name, String svnProject, String svnRevision, String svnNext, String invocationJavascript, String invocationMethod, List<String> javascriptModules, List<String> nextJavascriptModules, String description) {
        super(id, version);
        this.name = name;
        this.svnProject = svnProject;
        this.svnRevision = svnRevision;
        this.invocationJavascript = invocationJavascript;
        this.invocationMethod = invocationMethod;
        this.javascriptModules = javascriptModules;
        this.nextJavascriptModules = nextJavascriptModules;
        this.description = description == null ? "" : description;
        this.svnNext = svnNext;
    }

    public FlowComponentModel() {
        this(0L, 0L, "", "", "", "", "", "", new ArrayList<>(), new ArrayList<>(), "");
    }

    /**
     * @return name The name of the flow component
     */
    public String getName() {
        return name;
    }

    /**
     * Set flow component name
     *
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
     *
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
     *
     * @param svnRevision The SVN Revision number
     */
    public void setSvnRevision(String svnRevision) {
        this.svnRevision = svnRevision;
    }

    /**
     * @return name The next SVN Revision number
     */
    public String getSvnNext() {
        return svnNext;
    }

    /**
     * Set the SVN Revision number
     *
     * @param svnNext The next SVN Revision number
     */
    public void setSvnNext(String svnNext) {
        this.svnNext = svnNext;
    }

    /**
     * @return name The name of the Invocation Javascript
     */
    public String getInvocationJavascript() {
        return invocationJavascript;
    }

    /**
     * Set Invocation Javascript name
     *
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
     *
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
     *
     * @param javascriptModules The list of all Javascript modules
     */
    public void setJavascriptModules(List<String> javascriptModules) {
        this.javascriptModules = javascriptModules;
    }

    /**
     * @return name A list of all Next Javascript modules
     */
    public List<String> getNextJavascriptModules() {
        return nextJavascriptModules;
    }

    /**
     * Set the list of all Next Javascript modules
     *
     * @param nextJavascriptModules The list of all Next Javascript modules
     */
    public void setNextJavascriptModules(List<String> nextJavascriptModules) {
        this.nextJavascriptModules = nextJavascriptModules;
    }

    /**
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set description
     *
     * @param description Flow component description
     */
    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * Checks for empty String values
     *
     * @return true if no empty String values were found, otherwise false
     */
    public boolean isInputFieldsEmpty() {
        return name == null ||
                name.isEmpty() ||
                svnProject == null ||
                svnProject.isEmpty() ||
                svnRevision == null ||
                svnRevision.isEmpty() ||
                invocationJavascript == null ||
                invocationJavascript.isEmpty() ||
                invocationMethod == null ||
                invocationMethod.isEmpty() ||
                description == null ||
                description.isEmpty() ||
                javascriptModules == null ||
                javascriptModules.isEmpty() ||
                nextJavascriptModules == null ||
                nextJavascriptModules.isEmpty();
    }

    public boolean isInputFieldsEmptyModulesExcluded() {
        return name == null ||
                name.isEmpty() ||
                svnProject == null ||
                svnProject.isEmpty() ||
                svnRevision == null ||
                svnRevision.isEmpty() ||
                invocationJavascript == null ||
                invocationJavascript.isEmpty() ||
                invocationMethod == null ||
                invocationMethod.isEmpty() ||
                description == null ||
                description.isEmpty();
    }

    /**
     * Checks if the flow component name contains illegal characters.
     * A-Å, 0-9, - (minus), + (plus), _ (underscore) and space is valid
     *
     * @return a list containing illegal characters found. Empty list if none found.
     */
    public List<String> getDataioPatternMatches() {
        return Format.getDataioPatternMatches(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowComponentModel)) return false;

        FlowComponentModel that = (FlowComponentModel) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (svnProject != null ? !svnProject.equals(that.svnProject) : that.svnProject != null) return false;
        if (svnRevision != null ? !svnRevision.equals(that.svnRevision) : that.svnRevision != null) return false;
        if (svnNext != null ? !svnNext.equals(that.svnNext) : that.svnNext != null) return false;
        if (invocationJavascript != null ? !invocationJavascript.equals(that.invocationJavascript) : that.invocationJavascript != null)
            return false;
        if (invocationMethod != null ? !invocationMethod.equals(that.invocationMethod) : that.invocationMethod != null)
            return false;
        if (javascriptModules != null ? !javascriptModules.equals(that.javascriptModules) : that.javascriptModules != null)
            return false;
        if (nextJavascriptModules != null ? !nextJavascriptModules.equals(that.nextJavascriptModules) : that.nextJavascriptModules != null)
            return false;
        return !(description != null ? !description.equals(that.description) : that.description != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (svnProject != null ? svnProject.hashCode() : 0);
        result = 31 * result + (svnRevision != null ? svnRevision.hashCode() : 0);
        result = 31 * result + (svnNext != null ? svnNext.hashCode() : 0);
        result = 31 * result + (invocationJavascript != null ? invocationJavascript.hashCode() : 0);
        result = 31 * result + (invocationMethod != null ? invocationMethod.hashCode() : 0);
        result = 31 * result + (javascriptModules != null ? javascriptModules.hashCode() : 0);
        result = 31 * result + (nextJavascriptModules != null ? nextJavascriptModules.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}

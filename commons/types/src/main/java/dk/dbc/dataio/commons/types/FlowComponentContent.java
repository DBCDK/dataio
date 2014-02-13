package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FlowComponentContent DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class FlowComponentContent implements Serializable {
    private static final long serialVersionUID = -290854497828809813L;

    private /* final */ String name;
    private /* final */ String svnProject;
    private /* final */ long svnRevision;
    private /* final */ String javaScriptName;
    private /* final */ List<JavaScript> javascripts;
    private /* final */ String invocationMethod;

    private FlowComponentContent() { }

    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param name name of flow component
     * @param svnProject name of the SVN Project
     * @param svnRevision the SVN Revision number
     * @param javaScriptName name of the original javascript
     * @param javascripts list of attached JavaScripts (can be empty)
     * @param invocationMethod name of invocation method (can be empty)
     *
     * @throws NullPointerException if given null-valued name, javascripts or invocationMethod argument
     * @throws IllegalArgumentException if given empty-valued name argument
     */
    public FlowComponentContent(String name, String svnProject, long svnRevision, String javaScriptName, List<JavaScript> javascripts, String invocationMethod) {
        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.svnProject = InvariantUtil.checkNotNullNotEmptyOrThrow(svnProject, "svnProject");
        this.svnRevision = InvariantUtil.checkAboveThresholdOrThrow(svnRevision, "svnRevision", 0);
        this.javaScriptName = InvariantUtil.checkNotNullNotEmptyOrThrow(javaScriptName, "javaScriptName");
        this.invocationMethod = InvariantUtil.checkNotNullOrThrow(invocationMethod, "invocationMethod");
        this.javascripts = new ArrayList<JavaScript>(InvariantUtil.checkNotNullOrThrow(javascripts, "javascripts"));
    }

    public String getName() {
        return name;
    }

    public String getSvnProject() {
        return svnProject;
    }

    public long getSvnRevision() {
        return svnRevision;
    }

    public String getJavaScriptName() {
        return javaScriptName;
    }

    public String getInvocationMethod() {
        return invocationMethod;
    }

    public List<JavaScript> getJavascripts() {
        return new ArrayList<JavaScript>(javascripts);
    }
}

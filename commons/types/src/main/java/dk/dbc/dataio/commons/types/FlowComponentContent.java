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
    private /* final */ String svnProjectForInvocationJavascript;
    private /* final */ long svnRevision;
    private /* final */ String invocationJavascriptName;
    private /* final */ List<JavaScript> javascripts;
    private /* final */ String invocationMethod;
    private /* final */ String requireCache;

    private FlowComponentContent() { }



    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param name name of flow component
     * @param svnProjectForInvocationJavascript name of the SVN Project
     * @param svnRevision the SVN Revision number
     * @param invocationJavascriptName name of the original javascript
     * @param javascripts list of attached JavaScripts (can be empty)
     * @param invocationMethod name of invocation method (can be empty)
     *
     * @throws NullPointerException if given null-valued name, javascripts or invocationMethod argument
     * @throws IllegalArgumentException if given empty-valued name argument
     */
    public FlowComponentContent(String name, String svnProjectForInvocationJavascript, long svnRevision, String invocationJavascriptName, List<JavaScript> javascripts, String invocationMethod )
    {
        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.svnProjectForInvocationJavascript = InvariantUtil.checkNotNullNotEmptyOrThrow(svnProjectForInvocationJavascript, "svnProjectForInvocationJavascript");
        this.svnRevision = InvariantUtil.checkLowerBoundOrThrow(svnRevision, "svnRevision", 1);
        this.invocationJavascriptName = InvariantUtil.checkNotNullNotEmptyOrThrow(invocationJavascriptName, "invocationJavascriptName");
        this.invocationMethod = InvariantUtil.checkNotNullOrThrow(invocationMethod, "invocationMethod");
        this.javascripts = new ArrayList<JavaScript>(InvariantUtil.checkNotNullOrThrow(javascripts, "javascripts"));

    }
    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param name name of flow component
     * @param svnProjectForInvocationJavascript name of the SVN Project
     * @param svnRevision the SVN Revision number
     * @param invocationJavascriptName name of the original javascript
     * @param javascripts list of attached JavaScripts (can be empty)
     * @param invocationMethod name of invocation method (can be empty)
     * @param requireCache the JSON string of the RequireCache ( can be empty )
     *
     * @throws NullPointerException if given null-valued name, javascripts or invocationMethod argument
     * @throws IllegalArgumentException if given empty-valued name argument
     */

    public FlowComponentContent(String name, String svnProjectForInvocationJavascript, long svnRevision, String invocationJavascriptName, List<JavaScript> javascripts, String invocationMethod,
                                    String requireCache)
    {
        this(name, svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, javascripts, invocationMethod);
        this.requireCache = requireCache;
    }

    public String getName() {
        return name;
    }

    public String getSvnProjectForInvocationJavascript() {
        return svnProjectForInvocationJavascript;
    }

    public long getSvnRevision() {
        return svnRevision;
    }

    public String getInvocationJavascriptName() {
        return invocationJavascriptName;
    }

    public String getInvocationMethod() {
        return invocationMethod;
    }

    public List<JavaScript> getJavascripts() {
        return new ArrayList<JavaScript>(javascripts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowComponentContent)) return false;

        FlowComponentContent that = (FlowComponentContent) o;

        if (svnRevision != that.svnRevision) return false;
        if (!invocationJavascriptName.equals(that.invocationJavascriptName)) return false;
        if (!invocationMethod.equals(that.invocationMethod)) return false;
        if (!javascripts.equals(that.javascripts)) return false;
        if (!name.equals(that.name)) return false;
        if (!svnProjectForInvocationJavascript.equals(that.svnProjectForInvocationJavascript)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + svnProjectForInvocationJavascript.hashCode();
        result = 31 * result + (int) (svnRevision ^ (svnRevision >>> 32));
        result = 31 * result + invocationJavascriptName.hashCode();
        result = 31 * result + javascripts.hashCode();
        result = 31 * result + invocationMethod.hashCode();
        return result;
    }

    public String getRequireCache() {
        return requireCache;
    }
}

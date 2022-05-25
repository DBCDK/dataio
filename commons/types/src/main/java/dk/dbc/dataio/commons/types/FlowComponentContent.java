package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FlowComponentContent DTO class.
 */
public class FlowComponentContent implements Serializable {
    private static final long serialVersionUID = -290854497828809813L;

    private final String name;
    private final String svnProjectForInvocationJavascript;
    private final long svnRevision;
    private final String invocationJavascriptName;
    private final List<JavaScript> javascripts;
    private final String invocationMethod;
    private String requireCache;
    private String description;


    /**
     * Class constructor
     *
     * @param name                              name of flow component
     * @param svnProjectForInvocationJavascript name of the SVN Project
     * @param svnRevision                       the SVN Revision number
     * @param invocationJavascriptName          name of the original javascript
     * @param javascripts                       list of attached JavaScripts (can be empty)
     * @param invocationMethod                  name of invocation method (can be empty)
     * @param description                       description of flow component
     * @throws NullPointerException     if given null-valued name, javascripts or invocationMethod argument
     * @throws IllegalArgumentException if given empty-valued name argument
     */

    public FlowComponentContent(String name,
                                String svnProjectForInvocationJavascript,
                                long svnRevision,
                                String invocationJavascriptName,
                                List<JavaScript> javascripts,
                                String invocationMethod,
                                String description) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.svnProjectForInvocationJavascript = InvariantUtil.checkNotNullNotEmptyOrThrow(svnProjectForInvocationJavascript, "svnProjectForInvocationJavascript");
        this.svnRevision = InvariantUtil.checkLowerBoundOrThrow(svnRevision, "svnRevision", 1);
        this.invocationJavascriptName = InvariantUtil.checkNotNullNotEmptyOrThrow(invocationJavascriptName, "invocationJavascriptName");
        this.javascripts = new ArrayList<>(InvariantUtil.checkNotNullOrThrow(javascripts, "javascripts"));
        this.invocationMethod = InvariantUtil.checkNotNullOrThrow(invocationMethod, "invocationMethod");
        this.description = description;
    }

    /**
     * Class constructor
     *
     * @param name                              name of flow component
     * @param svnProjectForInvocationJavascript name of the SVN Project
     * @param svnRevision                       the SVN Revision number
     * @param invocationJavascriptName          name of the original javascript
     * @param javascripts                       list of attached JavaScripts (can be empty)
     * @param invocationMethod                  name of invocation method (can be empty)
     * @param description                       description of flow component
     * @param requireCache                      the JSON string of the RequireCache ( can be empty )
     * @throws NullPointerException     if given null-valued name, javascripts or invocationMethod argument
     * @throws IllegalArgumentException if given empty-valued name argument
     */

    @JsonCreator
    public FlowComponentContent(@JsonProperty("name") String name,
                                @JsonProperty("svnProjectForInvocationJavascript") String svnProjectForInvocationJavascript,
                                @JsonProperty("svnRevision") long svnRevision,
                                @JsonProperty("invocationJavascriptName") String invocationJavascriptName,
                                @JsonProperty("javascripts") List<JavaScript> javascripts,
                                @JsonProperty("invocationMethod") String invocationMethod,
                                @JsonProperty("description") String description,
                                @JsonProperty("requireCache") String requireCache) {
        this(name, svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, javascripts, invocationMethod, description);
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
        return new ArrayList<>(javascripts);
    }

    public String getDescription() {
        return description;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowComponentContent)) return false;

        FlowComponentContent that = (FlowComponentContent) o;

        return svnRevision == that.svnRevision
                && name.equals(that.name)
                && svnProjectForInvocationJavascript.equals(that.svnProjectForInvocationJavascript)
                && invocationJavascriptName.equals(that.invocationJavascriptName)
                && javascripts.equals(that.javascripts)
                && invocationMethod.equals(that.invocationMethod)
                && !(requireCache != null ? !requireCache.equals(that.requireCache) : that.requireCache != null)
                && !(description != null ? !description.equals(that.description) : that.description != null);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + svnProjectForInvocationJavascript.hashCode();
        result = 31 * result + (int) (svnRevision ^ (svnRevision >>> 32));
        result = 31 * result + invocationJavascriptName.hashCode();
        result = 31 * result + javascripts.hashCode();
        result = 31 * result + invocationMethod.hashCode();
        result = 31 * result + (requireCache != null ? requireCache.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    public String getRequireCache() {
        return requireCache;
    }
}

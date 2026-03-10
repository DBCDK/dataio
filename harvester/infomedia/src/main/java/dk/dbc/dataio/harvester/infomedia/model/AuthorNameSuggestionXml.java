package dk.dbc.dataio.harvester.infomedia.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * XML representation of an author name suggestion using the auto-nomen legacy format.
 */
public class AuthorNameSuggestionXml {

    @JacksonXmlProperty(localName = "input-name")
    private String inputName;

    @JacksonXmlProperty(localName = "authority")
    private String authority;

    public AuthorNameSuggestionXml() {
    }

    public AuthorNameSuggestionXml(String inputName, String authority) {
        this.inputName = inputName;
        this.authority = authority;
    }

    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}

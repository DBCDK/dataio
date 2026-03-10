package dk.dbc.dataio.harvester.infomedia.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * XML representation of author name suggestions using the auto-nomen legacy format.
 */
public class AuthorNameSuggestionsXml {

    @JacksonXmlProperty(localName = "aut-name")
    @JacksonXmlElementWrapper(localName = "aut-names")
    private List<AuthorNameSuggestionXml> authorNameSuggestions = new ArrayList<>();

    public List<AuthorNameSuggestionXml> getAuthorNameSuggestions() {
        return authorNameSuggestions;
    }

    public void setAuthorNameSuggestions(List<AuthorNameSuggestionXml> authorNameSuggestions) {
        this.authorNameSuggestions = authorNameSuggestions;
    }
}

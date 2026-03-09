package dk.dbc.dataio.harvester.infomedia.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "record")
public class Record {
    private Infomedia infomedia;

    @JacksonXmlProperty(localName = "author-name-suggestion")
    @JacksonXmlElementWrapper(localName = "author-name-suggestions")
    private List<AuthorNameSuggestionsXml> authorNameSuggestionsXml;

    public Infomedia getInfomedia() {
        return infomedia;
    }

    public void setInfomedia(Infomedia infomedia) {
        this.infomedia = infomedia;
    }

    public List<AuthorNameSuggestionsXml> getAuthorNameSuggestionsXml() {
        return authorNameSuggestionsXml;
    }

    public void setAuthorNameSuggestionsXml(List<AuthorNameSuggestionsXml> authorNameSuggestionsXmls) {
        this.authorNameSuggestionsXml = authorNameSuggestionsXmls;
    }
}

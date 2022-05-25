package dk.dbc.dataio.harvester.infomedia.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import dk.dbc.autonomen.AutoNomenSuggestions;

import java.util.List;

@JacksonXmlRootElement(localName="record")
public class Record {
    private Infomedia infomedia;

    @JacksonXmlProperty(localName = "author-name-suggestion")
    @JacksonXmlElementWrapper(localName = "author-name-suggestions")
    private List<AutoNomenSuggestions> autoNomenSuggestions;
    
    public Infomedia getInfomedia() {
        return infomedia;
    }

    public void setInfomedia(Infomedia infomedia) {
        this.infomedia = infomedia;
    }

    public List<AutoNomenSuggestions> getAutoNomenSuggestions() {
        return autoNomenSuggestions;
    }

    public void setAutoNomenSuggestions(List<AutoNomenSuggestions> autoNomenSuggestions) {
        this.autoNomenSuggestions = autoNomenSuggestions;
    }
}

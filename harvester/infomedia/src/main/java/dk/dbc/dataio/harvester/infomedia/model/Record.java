/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import dk.dbc.authornamesuggester.AuthorNameSuggestions;

import java.util.List;

@JacksonXmlRootElement(localName="record")
public class Record {
    private Infomedia infomedia;

    @JacksonXmlProperty(localName = "author-name-suggestion")
    @JacksonXmlElementWrapper(localName = "author-name-suggestions")
    private List<AuthorNameSuggestions> authorNameSuggestions;
    
    public Infomedia getInfomedia() {
        return infomedia;
    }

    public void setInfomedia(Infomedia infomedia) {
        this.infomedia = infomedia;
    }

    public List<AuthorNameSuggestions> getAuthorNameSuggestions() {
        return authorNameSuggestions;
    }

    public void setAuthorNameSuggestions(List<AuthorNameSuggestions> authorNameSuggestions) {
        this.authorNameSuggestions = authorNameSuggestions;
    }
}
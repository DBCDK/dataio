/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import dk.dbc.authornamesuggester.Suggestions;

@JacksonXmlRootElement(localName="record")
public class Record {
    private Infomedia infomedia;
    private Suggestions authorNameSuggestions;
    
    public Infomedia getInfomedia() {
        return infomedia;
    }

    public void setInfomedia(Infomedia infomedia) {
        this.infomedia = infomedia;
    }

    public Suggestions getAuthorNameSuggestions() {
        return authorNameSuggestions;
    }

    public void setAuthorNameSuggestions(Suggestions authorNameSuggestions) {
        this.authorNameSuggestions = authorNameSuggestions;
    }
}
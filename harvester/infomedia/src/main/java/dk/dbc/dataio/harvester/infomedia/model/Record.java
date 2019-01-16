/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName="record")
public class Record {
    private Infomedia infomedia;
    
    public Infomedia getInfomedia() {
        return infomedia;
    }

    public void setInfomedia(Infomedia infomedia) {
        this.infomedia = infomedia;
    }
}
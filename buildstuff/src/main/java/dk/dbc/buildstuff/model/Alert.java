package dk.dbc.buildstuff.model;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Alert extends Deploy {
    @Override
    public String getFilename() {
        return "alert-" + name + ".yml";
    }
}

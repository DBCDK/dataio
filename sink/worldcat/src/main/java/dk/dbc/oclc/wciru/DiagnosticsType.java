package dk.dbc.oclc.wciru;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "diagnosticsType", namespace = "http://www.loc.gov/zing/srw/", propOrder = {
        "diagnostic",
        "extraDiagData"
})
public class DiagnosticsType {

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/diagnostic/", required = true)
    protected List<Diagnostic> diagnostic;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    protected String extraDiagData;

    public List<Diagnostic> getDiagnostic() {
        if (diagnostic == null) {
            diagnostic = new ArrayList<Diagnostic>();
        }
        return this.diagnostic;
    }

    public String getExtraDiagData() {
        return extraDiagData;
    }

    public void setExtraDiagData(String value) {
        this.extraDiagData = value;
    }
}

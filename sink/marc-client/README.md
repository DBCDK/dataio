marc-client
==============

In order to stabilize the tests, xmlns entries have been added to src/main/java/dk/dbc/oss/ns/updatemarcxchange/package-info.java 
to fix the xms prefixes, in an otherwise wsgen generated client.

Furthermore, the wsgen plugin is now wrapped in a profile, which can be activated using mvn -P generate-source.
If you do want to regenerate the source, you should probably re-add the xmlns entries.

```
@XmlSchema(namespace = "http://oss.dbc.dk/ns/updateMarcXchange", elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED, xmlns = {
    @XmlNs(prefix = "", namespaceURI="info:lc/xmlns/marcxchange-v1"),
    @XmlNs(prefix = "ns2", namespaceURI = "http://oss.dbc.dk/ns/updateMarcXchange")
})
```
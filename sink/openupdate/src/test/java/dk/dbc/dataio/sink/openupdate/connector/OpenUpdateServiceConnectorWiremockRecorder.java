package dk.dbc.dataio.sink.openupdate.connector;

public class OpenUpdateServiceConnectorWiremockRecorder {
    /*
        Steps to reproduce wiremock recording:

        * Start standalone runner
            java -jar wiremock-standalone.jar --proxy-all="http://fbstest:20080" --record-mappings --verbose

        * Run the main method of this class

        * Replace content of src/test/resources/{__files|mappings} with that produced by the standalone runner
     */

    public static void main(String[] args) {
        OpenUpdateServiceConnectorIT openUpdateServiceConnectorIT = new OpenUpdateServiceConnectorIT();
        openUpdateServiceConnectorIT.openUpdateServiceConnector = new OpenUpdateServiceConnector("http://localhost:8080/UpdateService/2.0");
        openUpdateServiceConnectorIT.recordUpdateRecordRequests();
    }
}

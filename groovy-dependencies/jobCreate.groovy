import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder
import FlowStore


//def fs = new FlowStore()
//fs.createSink("sinkname", "sinkRes")

def createDefaultFlowComponent(flowStore) {
    def name = "testComponent"
    def svnProject = "datawell-convert"
    def svnRevision = 71958
    def invocationJavascript = "trunk/js/xml_datawell_3.0.js"
    def invocationFunction = "createDatawellXmlFromBibzoom"

    flowStore.createFlowComponent(name, svnProject, svnRevision, invocationJavascript, invocationFunction)
}
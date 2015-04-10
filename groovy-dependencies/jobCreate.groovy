import FlowStore

import dk.dbc.dataio.commons.types.JobSpecification
import dk.dbc.dataio.jobstore.types.JobInputStream
import groovy.json.JsonSlurper

def createDefaultFlowComponent(flowStore) {
  def name = "testComponent"
  def svnProject = "datawell-convert"
  def svnRevision = 71958
  def invocationJavascript = "trunk/js/xml_datawell_3.0.js"
  def invocationFunction = "createDatawellXmlFromBibzoom"
  
  flowStore.createFlowComponent(name, svnProject, svnRevision, invocationJavascript, invocationFunction)
}

def createDefaultSink(flowStore) {
  def name = "dummyTestSink"
  def resource = "dummysink"

  fs.createSink(name, resource)
}

def createDefaultJob(jobConn) {

  def jobSpecPackaging = "xml"
  def jobSpecFormat = "myformat"
  def jobSpecCharset = "utf8"
  def jobSpecDestination = "myDummySink"
  def submitterNumber = 1234567
  def mailAddr = "jda@dbc.dk"
  def initials = "jda"
  /* def dataFile = "/home/damkjaer/dataio-trunk/us140-testdata/150014.albums.alt.godt.xml" */
  def dataFile = "/home/damkjaer/dataio-trunk/us140-testdata/does_not_exist.xml"

  def jobSpec = new JobSpecification(jobSpecPackaging, jobSpecFormat, jobSpecCharset, jobSpecDestination, 
				     submitterNumber, mailAddr, mailAddr, initials, dataFile)
  jobConn.addJob(new JobInputStream(jobSpec, true, 0))
}


def createDefaultSetup(flowStore) {

  def sinkName = "dummyTestSink"
  def sinkResource = "dummySink"

  def submitterName = "mySubmitter"
  def submitterNumber = 1234567
  def submitterDescription = "This is the submitter description"
  
  def flowComponentName = "testComponent"
  def flowComponentSvnProject = "datawell-convert"
  def flowComponentSvnRevision = 71958
  def flowComponentInvocationJavascript = "trunk/js/xml_datawell_3.0.js"
  def flowComponentInvocationFunction = "createDatawellXmlFromBibzoom"
  
  def flowName = "testFlow"
  def flowDescription = "This is the flow description"
  
  def flowBinderName = "testFlowBinder"
  def flowBinderDescription = "This is the flowbinder description"
  def flowBinderPackaging = "xml"
  def flowBinderFormat = "myformat"
  def flowBinderCharset = "utf8"
  def flowBinderDestination = "myDummySink"
  def flowBinderRecordSplitter = "DefaultXMLRecordSplitter"


  def sinkResult = flowStore.createSink(sinkName, sinkResource)
  def submitterResult = flowStore.createSubmitter(submitterName, submitterNumber, submitterDescription)
  def flowComponentResult = flowStore.createFlowComponent(flowComponentName, flowComponentSvnProject, flowComponentSvnRevision, 
							  flowComponentInvocationJavascript, flowComponentInvocationFunction)
  def flowResult = flowStore.createFlow(flowName, flowDescription, flowComponentResult.json)
 
  def submitterId = submitterResult.location.split("/").last().toLong()
  def flowId = flowResult.location.split("/").last().toInteger()
  def slurper = new JsonSlurper()
  def sinkJson = slurper.parseText(sinkResult.json)
  def sinkId = sinkJson.id

  def flowBinderResult = flowStore.createFlowBinder(flowBinderName, flowBinderDescription, flowBinderPackaging, flowBinderFormat, 
  						    flowBinderCharset, flowBinderDestination, flowBinderRecordSplitter, flowId, [submitterId], sinkId)

  true
}
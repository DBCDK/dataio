@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1' )
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST

import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder

import dk.dbc.dataio.gui.server.JavaScriptProjectFetcherImpl
import dk.dbc.dataio.commons.types.FlowComponentContent

import dk.dbc.dataio.commons.utils.json.JsonUtil

class FlowStore {

  def host = "http://localhost:8080"
  def flowStoreUri = "/flow-store"

  def getHost() {
    host
  }

  def postJsonData(endpoint, data) {
    def uriPath = flowStoreUri + "/" + endpoint
    println "posting json to: " + host + uriPath

    def http = new HTTPBuilder( host )
    http.request( POST, JSON ) {req -> 
      uri.path = uriPath
      body = data

      response.success = { resp, json ->
	println "Status: " + resp.getStatus()
	println "Result: " + json
	["location":resp.getHeaders("Location").value, "json":json]
      }

      response.failure = { resp ->
	println "Status: " + resp.getStatus()
      }
    }
  }

  def getJsonData(url) {
    println "Getting json from: " + url

    def http = new HTTPBuilder(url)
    http.request(GET, JSON) { req -> 

      response.success = { resp, json ->
	println "Status: " + resp.getStatus()
	println "Result: " + json
	json
      }

      response.failure = { resp ->
	println "Status: " + resp.getStatus()
      }
    }
  }

  def createSink(name, resource) {
    def sinkContentJson = new SinkContentJsonBuilder().setName(name).setResource(resource).build();

    println "Trying to create sink with: " + sinkContentJson
    postJsonData("sinks", sinkContentJson)
  }

  def createFlowComponent(name, svnProject, svnRevision, invocationJavascript, invocationFunction) {
    def javascriptProjectFetcher = new JavaScriptProjectFetcherImpl("https://svn.dbc.dk/repos")
    def javascripts = javascriptProjectFetcher.fetchRequiredJavaScript(svnProject, svnRevision, invocationJavascript, invocationFunction)
    def fc = new FlowComponentContent(name, svnProject, svnRevision, invocationJavascript, javascripts, invocationFunction)
    def json = JsonUtil.toJson(fc)
    println "Trying to create flowComponent: " + fc.name
    def res = postJsonData("components", json)
    res
  }

  def createSubmitter(name, number, description) {
    def json = new SubmitterContentJsonBuilder().setName(name).setNumber(number).setDescription(description).build();

    println "Trying to create submitter with: " + json
    postJsonData("submitters", json)
  }

  def createFlow() {
  }

  def createFlowBinder() {
  }
  
}


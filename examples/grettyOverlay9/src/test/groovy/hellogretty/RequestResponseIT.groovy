package hellogretty

import geb.spock.GebReportingSpec

class RequestResponseIT extends GebReportingSpec {

  def 'should get expected response from servlet'() {
  when:
    go 'http://localhost:8080/grettyOverlay9/hello'
  then:
    $('h1').text() == 'Dynamic content generated by servlet'
    $('p', 0).text() == 'This is overlayed web-application!'
  }

  def 'should get expected static page'() {
  when:
    go 'http://localhost:8080/grettyOverlay9/index.html'
  then:
    $('h1').text() == 'Hello, world!'
  }
}
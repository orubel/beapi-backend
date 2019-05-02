package net.nosegrind.apiframework

import geb.spock.*
import grails.gorm.transactions.*
import grails.testing.mixin.integration.Integration
import grails.util.Holders
import grails.util.Metadata
import groovy.json.JsonSlurper
import net.nosegrind.apiframework.ApiCacheService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import spock.lang.*

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import net.nosegrind.apiframework.Person

/**
 * See http://www.gebish.org/manual/current/ for more instructions
 */

@Integration
@Rollback
class ApidocFunctionalSpec extends Specification {

    @Autowired
    ApplicationContext applicationContext

    @Shared String token
    @Shared String guestToken
    @Shared List authorities = ['permitAll']
    @Shared String controller = 'apidoc'
    @Shared String testDomain = 'http://localhost:8080'
    @Shared String currentId
    @Shared String guestId
    @Shared String appVersion = "v${Metadata.current.getProperty(Metadata.APPLICATION_VERSION, String.class)}"
    @Shared String guestdata = "{'username': 'apidoctest','password':'testamundo','email':'apidoc@guesttest.com'}"
    @Shared String guestlogin = 'apidoctest'
    @Shared String guestpassword = 'testamundo'

    void "login and get token"(){
        setup:"logging in"
            String METHOD = "POST"
            String login = Holders.grailsApplication.config.root.login
            String password = Holders.grailsApplication.config.root.password
            String loginUri = Holders.grailsApplication.config.grails.plugin.springsecurity.rest.login.endpointUrl

            String url = "curl -H 'Content-Type: application/json' -X ${METHOD} -d '{\"username\":\"${login}\",\"password\":\"${password}\"}' ${this.testDomain}${loginUri}"
            def proc = ['bash','-c',url].execute()
            proc.waitFor()
            def info = new JsonSlurper().parseText(proc.text)

        when:"set token"
            this.token = info.access_token
            info.authorities.each(){ it ->
                this.authorities.add(it)
            }
        then:"has bearer token"
            assert info.token_type == 'Bearer'
    }

    void "CREATE guest id call"() {
        setup:"api is called"
            String METHOD = "POST"
            String controller = 'person'
            String action = 'create'
            def info
            def proc = ["curl", "-H", "Content-Type: application/json", "-H", "Authorization: Bearer ${this.token}", "--request", "${METHOD}", "-d", "${this.guestdata}", "${this.testDomain}/${this.appVersion}/${controller}/${action}"].execute()
            proc.waitFor()
            def outputStream = new StringBuffer()
            proc.waitForProcessOutput(outputStream, System.err)
            String output = outputStream.toString()
            info = new JsonSlurper().parseText(output)
        when:"info is not null"
            this.guestId = info['id']
            assert info!=null
        then:"created user"
            assert info['id'] != null

    }

    // create using mockdata
    void "CREATE guest role call"() {
        setup:"api is called"
            String METHOD = "POST"
            String controller = 'personRole'
            String action = 'create'
            String data = "{'personId': '${this.guestId}','roleId':'1'}"
            def info
            def proc = ["curl", "-H", "Content-Type: application/json", "-H", "Authorization: Bearer ${this.token}", "--request", "${METHOD}", "-d", "${data}", "${this.testDomain}/${this.appVersion}/${controller}/${action}"].execute()
            proc.waitFor()
            def outputStream = new StringBuffer()
            proc.waitForProcessOutput(outputStream, System.err)
            String output = outputStream.toString()
            info = new JsonSlurper().parseText(output)
        when:"info is not null"
            assert info!=null
        then:"created user"
            assert info['roleId'] != null

    }

    void "GUEST login and get token"(){
        setup:"logging in"
            String loginUri = Holders.grailsApplication.config.grails.plugin.springsecurity.rest.login.endpointUrl

            String url = "curl -H 'Content-Type: application/json' -X POST -d '{\"username\":\"${this.guestlogin}\",\"password\":\"${this.guestpassword}\"}' ${this.testDomain}${loginUri}"
            def proc = ['bash','-c',url].execute()
            proc.waitFor()
            def info = new JsonSlurper().parseText(proc.text)

        when:"set token"
            this.guestToken = info.access_token
        then:"has bearer token"
            assert info.token_type == 'Bearer'
    }

    void "GET admin apidoc"() {
        setup:"apidoc is called"
            String METHOD = "GET"
            String action = 'show'
            def info = [:]
            def proc = ["curl","-H","Content-Type: application/json","-H","Authorization: Bearer ${this.token}","--request","${METHOD}","${this.testDomain}/${this.appVersion}/${this.controller}/${action}"].execute()
            proc.waitFor()
            def outputStream = new StringBuffer()
            proc.waitForProcessOutput(outputStream, System.err)
            String output = outputStream.toString()
            def slurper = new JsonSlurper()
            slurper.parseText(output).each(){ k,v ->
                info[k] = v
            }
        when:"info is not null"
            assert info!=[:]
        then:"get user"
            assert info['apidoc']['stat']['show']!=[:]
    }

    void "GET guest apidoc"() {
        setup:"apidoc is called"

            String METHOD = "GET"
            String action = 'show'
            def info = [:]
            def proc = ["curl","-H","Content-Type: application/json","-H","Authorization: Bearer ${this.guestToken}","--request","${METHOD}","${this.testDomain}/${this.appVersion}/${this.controller}/${action}"].execute()
            proc.waitFor()
            def outputStream = new StringBuffer()
            proc.waitForProcessOutput(outputStream, System.err)
            String output = outputStream.toString()

            def slurper = new JsonSlurper()
            slurper.parseText(output).each(){ k,v ->
                info[k] = v
            }
        when:"info is not null"
            assert info!=[:]
        then:"get user"
            assert info['apidoc']['stat']==null
    }


    void "DELETE guest:"() {
        setup:"api is called"
            String METHOD = "DELETE"
            String controller = 'person'
            String action = 'delete'
            LinkedHashMap info = [:]
            def proc = ["curl","-H","Content-Type: application/json","-H","Authorization: Bearer ${this.token}","--request","${METHOD}","${this.testDomain}/${this.appVersion}/${controller}/${action}?id=${this.guestId}"].execute()
            proc.waitFor()
            def outputStream = new StringBuffer()
            proc.waitForProcessOutput(outputStream, System.err)
            String output = outputStream.toString()

            info = new JsonSlurper().parseText(output)
        when:"info is not null"
            assert info!=null
        then:"delete created user"
            assert this.guestId == info.id
    }

}


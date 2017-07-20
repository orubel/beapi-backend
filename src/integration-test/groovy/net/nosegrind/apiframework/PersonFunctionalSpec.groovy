package net.nosegrind.apiframework

import grails.test.mixin.integration.Integration
import grails.transaction.*
import spock.lang.*
import geb.spock.*
import grails.util.Holders
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovy.json.JsonSlurper
import org.springframework.context.ApplicationContext
import net.nosegrind.apiframework.ApiCacheService
import org.grails.web.util.WebUtils

/**
 * See http://www.gebish.org/manual/current/ for more instructions
 */

@Integration
//@Rollback
class PersonFunctionalSpec extends Specification {

    @Shared String token
    @Shared String controller = 'person'
    @Shared String testDomain = 'http://localhost:8080'
    @Shared String currentId
    @Shared ApplicationContext ctx


    void "login and get token"(){
        setup:"logging in"
            this.ctx = Holders.grailsApplication.mainContext
            String login = Holders.grailsApplication.config.root.login
            String password = Holders.grailsApplication.config.root.password
            String loginUri = Holders.grailsApplication.config.grails.plugin.springsecurity.rest.login.endpointUrl

            String url = "curl -H 'Content-Type: application/json' -X POST -d '{\"username\":\"${login}\",\"password\":\"${password}\"}' ${this.testDomain}${loginUri}"
            def proc = ['bash','-c',url].execute();
            proc.waitFor()
            def info = new JsonSlurper().parseText(proc.text)

        when:"set token"
            this.token = info.access_token

        then:"has bearer token"
            assert info.token_type == 'Bearer'
    }

    // create using mockdata
    void "CREATE api call"() {
        setup:"api is called"
            String METHOD = "POST"

            ApiCacheService apiCacheService = ctx.getBean("apiCacheService")
            LinkedHashMap cache = apiCacheService.getApiCache(this.controller)
            Integer version = cache['cacheversion']

            String action = 'create'
            String data = "{"
            cache?."${version}"?."${action}".receives.each(){ k,v ->
                v.each(){
                    data += "'"+it.name+"': '"+it.mockData+"',"
                }
            }
            data += "}"
            def info
            net.nosegrind.apiframework.Person.withTransaction { status ->
                def proc = ["curl", "-H", "Content-Type: application/json", "-H", "Authorization: Bearer ${this.token}", "--request", "${METHOD}", "-d", "${data}", "${this.testDomain}/v0.1/${this.controller}/${action}"].execute();

                proc.waitFor()
                def outputStream = new StringBuffer()
                proc.waitForProcessOutput(outputStream, System.err)
                String output = outputStream.toString()
                info = new JsonSlurper().parseText(output)
            }
        when:"info is not null"
            assert info!=null
        then:"created user"
            cache?."${version}"?."${action}".returns.each(){ k,v ->
                v.each(){
                    if(it.keyType=='PRIMARY'){
                        this.currentId = info."${it.name}"
                    }
                    assert info."${it.name}" != null
                }
            }
    }

    void "GET api call"() {
        setup:"api is called"
            String METHOD = "GET"
            LinkedHashMap info = [:]
            ApiCacheService apiCacheService = this.ctx.getBean("apiCacheService")
            LinkedHashMap cache = apiCacheService.getApiCache(this.controller)

            Integer version = cache['cacheversion']

            String action = 'get'
            String pkey = cache?."${version}"?."${action}".pkey[0]

            def proc = ["curl","-H","Content-Type: application/json","-H","Authorization: Bearer ${this.token}","--request","${METHOD}","${this.testDomain}/v0.1/${this.controller}/${action}/${this.currentId}"].execute();
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
            cache?."${version}"?."${action}".returns.each(){ k,v ->
                v.each(){ it ->
                    assert info."${it.name}" != null
                }
            }
    }

    // create using mockdata
    void "DELETE api call"() {
        setup:"api is called"
            String METHOD = "DELETE"
            LinkedHashMap info = [:]
            ApiCacheService apiCacheService = this.ctx.getBean("apiCacheService")
            LinkedHashMap cache = apiCacheService.getApiCache(this.controller)

            Integer version = cache['cacheversion']

            String action = 'delete'
            def proc = ["curl","-H","Content-Type: application/json","-H","Authorization: Bearer ${this.token}","--request","${METHOD}","${this.testDomain}/v0.1/${this.controller}/${action}?id=${this.currentId}"].execute();
            proc.waitFor()
            def outputStream = new StringBuffer()
            proc.waitForProcessOutput(outputStream, System.err)
            String output = outputStream.toString()
            def slurper = new JsonSlurper()
            slurper.parseText(output).each(){ k,v ->
                info[k] = v
            }
        when:"info is not null"
            assert info!=null
        then:"delete created user"
            def id
            cache?."${version}"?."${action}".returns.each(){ k,v ->
                v.each(){ it ->
                    if(it.keyType=='PRIMARY'){
                        id = info."${it.name}"
                        println(id)
                    }

                }
            }
            assert this.currentId == id
    }
}


package grails.api.framework

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.apache.catalina.connector.Connector;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.jta.*
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean


//import org.grails.io.support.Resource
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource


import org.grails.config.yaml.YamlPropertySourceLoader
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.Resource
import org.grails.config.NavigableMapPropertySource
import org.springframework.core.io.DefaultResourceLoader
import groovy.util.ConfigSlurper

import org.apache.coyote.http2.Http2Protocol

import org.apache.tomcat.jdbc.pool.DataSource

import org.h2.tools.Server
import java.sql.Connection
import java.sql.DriverManager
import groovy.sql.Sql
import grails.util.Environment
import grails.util.Holders

import org.springframework.core.env.*

@EnableAutoConfiguration(exclude = [SecurityFilterAutoConfiguration,JtaAutoConfiguration])
class Application extends GrailsAutoConfiguration implements EnvironmentAware {

    static String cacheUrl
    private ResourceLoader defaultResourceLoader = new DefaultResourceLoader()
    private YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader()

    static void main(String[] args) {
        GrailsApp.run(Application, args)

        startDatabase()

        // Bootstrap database
        String userHome = System.getProperty('user.home')
        String filePath = userHome + "/.beapi/"
        String H2sql = new File(filePath+'beapi_h2.sql').text
        def sql = Sql.newInstance(this.cacheUrl, 'sa', 'sa', 'org.h2.Driver')
        sql.execute(H2sql)
    }

    // Add secondary connector for port 8080
    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory()
        tomcat.addAdditionalTomcatConnectors(createConnector())
        //tomcat.addContextValves(headerEncodingValve)
        return tomcat
    }

    // Add port 8080 and redirect to 8443
    private Connector createConnector() {
        try {
            Connector connector = new Connector()
            connector.setScheme("http")
            connector.setPort(8080)
            connector.setSecure(false)
            connector.setRedirectPort(8443)
            connector.addUpgradeProtocol(new Http2Protocol())
            return connector
        } catch (Throwable ex) {
            throw new IllegalStateException("Failed setting up Connector", ex)
        }
    }

    static void startDatabase(String cacheUrl) {

        Server server = null

        try {
            server = org.h2.tools.Server.createTcpServer("-tcpPort", "9092", "-tcpAllowOthers").start()
            if (server.isRunning(true)) {
                Class.forName("org.h2.Driver")
                Connection conn = DriverManager.getConnection(this.cacheUrl, "sa", "sa");
                println("Connection Established: " + conn.getMetaData().getDatabaseProductName() + "/" + conn.getCatalog())
            } else {
                println("H2 server not running")
            }
        } catch (Exception e) {
            println(e)
        }
    }

    void setEnvironment(org.springframework.core.env.Environment environment) {
        List locations = environment.getProperty('grails.config.locations', ArrayList, [])
        String encoding = environment.getProperty('grails.config.encoding', String, 'UTF-8')

        if (locations) {
            locations.reverse().each { location ->
                String finalLocation = location.toString()
                // Replace ~ with value from system property 'user.home' if set
                String userHome = System.properties.getProperty('user.home')
                if (userHome && finalLocation.startsWith('~/')) {
                    finalLocation = "file:${userHome}${finalLocation[1..-1]}"
                }
                finalLocation = environment.resolvePlaceholders(finalLocation)

                Resource resource = defaultResourceLoader.getResource(finalLocation) as Resource

                if(resource.exists()) {
                    if (finalLocation.endsWith('.groovy')) {
                        String configText = resource.inputStream.getText(encoding)
                        Map properties = configText ? new ConfigSlurper(grails.util.Environment.current.name).parse(configText)?.flatten() : [:]
                        MapPropertySource groovyConfig = new MapPropertySource(resource.filename, properties)
                        environment.propertySources.addFirst(groovyConfig)
                    } else if (finalLocation.endsWith('.yml')) {
                        NavigableMapPropertySource yamlConfig = yamlPropertySourceLoader.load(resource.filename, resource, null) as NavigableMapPropertySource
                        environment.propertySources.addFirst(yamlConfig)
                    } else {
                        // properties
                    }
                }else{
                    println("${finalLocation} does not exist")
                }
            }
        }

        String cacheUrl
        switch (Environment.current) {
            case Environment.DEVELOPMENT:
                this.cacheUrl = environment.getProperty('localCache')
                break
            case Environment.TEST:
                this.cacheUrl = environment.getProperty('localCache')
                break
            case Environment.PRODUCTION:
                println("prod")
                println(environment.getProperty('localCache'))
                this.cacheUrl = environment.getProperty('localCache')
                break
        }

    }
}

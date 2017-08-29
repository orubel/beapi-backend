

//import grails.plugins.GrailsPluginManager
//import grails.plugins.GrailsPlugin
import net.nosegrind.apiframework.Person
import net.nosegrind.apiframework.Role
import net.nosegrind.apiframework.PersonRole

class BootStrap {

    def passwordEncoder
	def grailsApplication
	//ApiObjectService apiObjectService
	//ApiCacheService apiCacheService
    def springSecurityService
	
    def init = { servletContext ->
        println("#### BOOTSTRAP ####")
        if (springSecurityService == null){
            println "springSecurityService is null"
        }else{
            println "springSecurityService EXISTS!!!"
        }
        def apitoolkit = grailsApplication.config.apitoolkit

        apitoolkit.roles.each { it ->
            String currRole = it
            Role role = Role.findByAuthority(currRole)
            if(!role){
                role = new Role(authority:currRole)
                role.save(flush:true,failOnError:true)
            }
        }

        Person user = Person.findByUsername("${grailsApplication.config.root.login}")
//Person user = Person.findByUsername("root")

        PersonRole.withTransaction(){ status ->
            Role adminRole = Role.findByAuthority("ROLE_ADMIN")

            if(!user?.id){
                println(grailsApplication.config.root.login)
                println(grailsApplication.config.root.password)
                println(grailsApplication.config.root.email)
                user = new Person(username:"${grailsApplication.config.root.login}",password:"${grailsApplication.config.root.password}",email:"${grailsApplication.config.root.email}")

                if(!user.save(flush:true)){
                    user.errors.allErrors.each { println it }
                }
            }else{
                if(!passwordEncoder.isPasswordValid(user.password, grailsApplication.config.root.password, null)){
                    log.error "Error: Bootstrapped Root Password was changed in config. Please update"
                }
            }

            if(!user?.authorities?.contains(adminRole)){
                PersonRole pRole = new PersonRole(user,adminRole)
                pRole.save(flush:true,failOnError:true)
            }

            status.isCompleted()
        }


		//apiObjectService.initialize()
		//def test = apiCacheService.getCacheNames()

    }

    def destroy = {}
}

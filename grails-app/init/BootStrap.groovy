

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
	
    def init = { servletContext ->
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
                user = new Person(username:"${grailsApplication.config.root.login}",password:"${grailsApplication.config.root.password}",email:"${grailsApplication.config.root.email}")
                //user = new Person(username:"root",password:"password",email:"orubel@nosegrind.main.scripts.net")
                if(!user.save(flush:true,failOnError:true)){
                    user.errors.allErrors.each { log.error it }
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

        // Bootstrap tools
        // run with 'create-update' first time, then change to 'update' and comment out.
        //apiParseService.parseArchive('http://perf-scidmz-data.cac.washington.edu/esmond/perfsonar/archive/?format=json')

		/*
		def plugins = pluginMngr.getAllPlugins()
		plugins.each{
			println(it)
		}
		*/
		//apiObjectService.initialize()
		//def test = apiCacheService.getCacheNames()

    }

    def destroy = {}
}

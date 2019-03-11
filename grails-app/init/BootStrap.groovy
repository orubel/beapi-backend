

//import grails.plugins.GrailsPluginManager
//import grails.plugins.GrailsPlugin
import net.nosegrind.apiframework.Person
import net.nosegrind.apiframework.Role
import net.nosegrind.apiframework.PersonRole
<<<<<<< HEAD

=======
import groovy.sql.Sql
>>>>>>> cc88b4edd72d6c5698146acc5474c202c81d7558

class BootStrap {

    def passwordEncoder
    def grailsApplication
    def springSecurityService
	
    def init = { servletContext ->
<<<<<<< HEAD
        // Throttle
        // only instantiate if this server is 'master'; check config value


=======
>>>>>>> cc88b4edd72d6c5698146acc5474c202c81d7558
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


        PersonRole.withTransaction(){ status ->
            Role adminRole = Role.findByAuthority("ROLE_ADMIN")

            if(!user?.id){
                //println(grailsApplication.config.root.login)
                //println(grailsApplication.config.root.password)
                //println(grailsApplication.config.root.email)
                user = new Person(username:"${grailsApplication.config.root.login}",password:"${grailsApplication.config.root.password}",email:"${grailsApplication.config.root.email}")

                if(!user.save(flush:true)){
                    user.errors.allErrors.each { println it }
                }
            }else{
                // user exists
                if(!passwordEncoder.isPasswordValid(user.password, grailsApplication.config.root.password, null)){
                    log.error "Error: Bootstrapped Root Password was changed in config. Please update"
                }
            }

            if(!user?.authorities?.contains(adminRole)){
                PersonRole pRole = new PersonRole(user,adminRole)
                pRole.save(flush:true,failOnError:true)
            }else{
                // role exists
            }

            status.isCompleted()
        }

<<<<<<< HEAD
    def destroy = {}
=======

    }

    def destroy = {}


>>>>>>> cc88b4edd72d6c5698146acc5474c202c81d7558
}

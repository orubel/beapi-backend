package net.nosegrind.apiframework

class PersonController{
	
	def springSecurityService

	LinkedHashMap list() {
		if(isSuperuser()){
			def result = Person.list()
			return [person:result]
		}
	}

	java.lang.String value(){}

	LinkedHashMap create(){
		Person user = new Person(username:"${params.username}",password:"${params.password}",email:"${params.email}")
		if(!user.save(flush:true,failOnError:true)){
			user.errors.allErrors.each { log.error it }
		}
		return [person:user]
	}

	LinkedHashMap show(){
		try{
			Person user
			if(isSuperuser()) {
				user = Person.get(params?.id?.toLong())
				return [person: user]
			}else{
				user = Person.get(springSecurityService.principal.id)
				return [person: user]
			}

		}catch(Exception e){
			throw new Exception("[PersonController : get] : Exception - full stack trace follows:",e)
		}
    }

	LinkedHashMap getByUsername(){
		try{
			Person user
			user = Person.get(params?.username)
			return [person: user]
		}catch(Exception e){
			throw new Exception("[PersonController : get] : Exception - full stack trace follows:",e)
		}
	}


	LinkedHashMap delete() {
		try {
			Person person
			if(isSuperuser()) {
				person = Person.get(params?.id?.toLong())
			}else{
				person = Person.get(springSecurityService.principal.id)
			}
			person.delete(flush: true, failOnError: true)
			return [person: [id: params.id.toLong()]]
		}catch(Exception e){
			throw new Exception("#[PersonController : delete] : Exception - full stack trace follows:",e)
		}
	}

	protected boolean isSuperuser() {
		springSecurityService.principal.authorities*.authority.any { grailsApplication.config.apitoolkit.admin.roles.contains(it) }
	}
}

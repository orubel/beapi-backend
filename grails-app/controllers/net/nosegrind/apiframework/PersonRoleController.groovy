package net.nosegrind.apiframework

class PersonRoleController{


	LinkedHashMap create(){
		try{
			PersonRole role = new PersonRole(person:"${params.id}",role:"${params.role}")

			if(role){
				if(!role.save(flush:true,failOnError:true)){
					role.errors.allErrors.each { println(it) }
				}
				return [personrole:role]
			}else{
				render(status: 500,text:"Bad data sent. Could not complete transaction.")
			}
		}catch(Exception e){
			throw new Exception("[PersonRoleController : get] : Exception - full stack trace follows:",e)
		}
	}

	LinkedHashMap showByPerson(){
		try{
			PersonRole role = new PersonRole()
			role = PersonRole.get(params?.id?.toLong())

			if(role){
				return [personrole:role]
			}else{
				render(status: 500,text:"Id does not match record in database.")
			}
		}catch(Exception e){
			throw new Exception("[PersonRoleController : get] : Exception - full stack trace follows:",e)
		}
	}

	LinkedHashMap showByRole(){
		try{
			PersonRole role = new PersonRole()
			role = PersonRole.get(params?.id?.toLong())

			if(role){
				return [personrole:role]
			}else{
				render(status: 500,text:"Id does not match record in database.")
			}
		}catch(Exception e){
			throw new Exception("[PersonRoleController : get] : Exception - full stack trace follows:",e)
		}
	}

	LinkedHashMap delete() {
		PersonRole role
		try {
			role = PersonRole.get(params.id)
			if(role){
				role.delete(flush: true, failOnError: true)
				return [personrole: [id:params.id.toLong()]]
			}else{
				render(status: 500,text:"Id " + params.id + " does not match record in database.")
			}
		}catch(Exception e){
			throw new Exception("[PersonRoleController : delete] : Exception - full stack trace follows:",e)
		}
	}
}
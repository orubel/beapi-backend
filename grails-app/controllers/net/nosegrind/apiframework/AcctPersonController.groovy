package net.nosegrind.apiframework

import grails.plugin.springsecurity.rest.token.AccessToken
import grails.plugin.springsecurity.rest.token.generation.TokenGenerator
import org.springframework.security.core.userdetails.UserDetails

class AcctPersonController {
	
	def springSecurityService
	TokenGenerator tokenGenerator

	def list(){
		println("#### acctperson::list called")
		try{
			Person person
			if(isSuperuser()){
				if(params?.id?.toLong()){
					// TODO : Future functionality
					person = Person.get(params?.id?.toLong())
				}else{
					person = Person.get(springSecurityService.principal.id)
				}
			}else{
				person = Person.get(springSecurityService.principal.id)
			}
			AcctPerson[] acct = AcctPerson.findByPerson(person)

			List accts = []

			acct.each(){
				LinkedHashMap temp = [acct:it.acct.id,acctName:it.acct.acctName]
				accts.add(temp)
			}
			println(accts)
			return [account: accts]
		}catch(Exception e){
			throw new Exception("[AccountController : get] : Exception - full stack trace follows:",e)
		}
	}

	def show(){
		try {
			// double check against person
			AcctPerson acctPerson
			Account acct
			Person person

			if(params?.id) {
				acctPerson = AcctPerson.get(params?.id?.toLong())
			}else{
				acct = Account.get(params.acct.toLong())
				person = Person.get(springSecurityService.principal.id)
				acctPerson = AcctPerson.findByAcctAndPerson(acct,person)
			}

			return [account: acctPerson]

		}catch(Exception e){
			throw new Exception("[AccountController : get] : Exception - full stack trace follows:",e)
		}
	}

	def create(){
		try{
			Account.withTransaction { status ->
				Account acct = Account.get(params.acct.toLong())
				if (acct == null) {
					render(status: 400, text: "NO ACCOUNT BY THAT ID EXISTS")
				}

				Person person = Person.get(params.person.toLong())
				if (person == null) {
					render(status: 400, text: "NO PERSON BY THAT USERNAME EXISTS")
				}

				if (acct.id) {
					AcctPerson acctPerson = new AcctPerson(acct: acct, person: person, owner: false)
					return [account: acct]
				} else {
					status.setRollbackOnly()
				}
			}
		}catch(Exception e){
			throw new Exception("[AccountController : get] : Exception - full stack trace follows:",e)
		}
	}

	def resetToken(){
		try {
			// double check against person
			AcctPerson acctPerson
			Account acct
			Person person
			String token = ""
			if(params?.id) {
				acctPerson = AcctPerson.get(params?.id?.toLong())
			}else{
				acct = Account.get(params.acct.toLong())
				person = Person.get(springSecurityService.principal.id)
				acctPerson = AcctPerson.findByAcctAndPerson(acct,person)
				if(acctPerson.owner==true){
					token = createToken()
				}
			}

			return [account: token]

		}catch(Exception e){
			throw new Exception("[AccountController : get] : Exception - full stack trace follows:",e)
		}
	}


	def delete() {
		try {
			// double check against person
			AcctPerson acct = AcctPerson.get(params.id?.toLong())
			if(acct.person.id==springSecurityService.principal.id || isSuperuser()) {
				acct.delete(flush: true, failOnError: true)
				return [account: [id: params.id.toLong()]]
			}else{
				render( status: 400, text: "ILLEGAL ATTEMPTED ACCESS")
			}
		}catch(Exception e){
			throw new Exception("#[AccountController : delete] : Exception - full stack trace follows:",e)
		}
	}

	// TODO : give account ownership to someone else
	// def changeOwner(){}

	// TODO : add acct user
	// def createAcctUser(){}

	protected boolean isSuperuser() {
		springSecurityService.principal.authorities*.authority.any { grailsApplication.config.apitoolkit.admin.roles.contains(it) }
	}

	private String createToken(){
		UserDetails userDetails = springSecurityService.principal as UserDetails
		AccessToken accessToken = tokenGenerator.generateAccessToken(userDetails)
		//tokenStorageService.storeToken(accessToken.accessToken, userDetails)
		return accessToken.accessToken
	}
}

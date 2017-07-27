package net.nosegrind.apiframework


class AccountController {
	
	def springSecurityService


	def create(){
		try{

			Account.withTransaction { status ->
				Account acct = new Account(acctName: "${params.acctName}", enabled: true)
				if (!acct.save(flush: true, failOnError: true)) {
					acct.errors.allErrors.each { log.error it }
				}

				if (acct.active) {
					AcctPerson acctPerson = new AcctPerson(acct: acct, person: springSecurityService.principal.id, owner: true)
					return [account: acct]
				} else {
					status.setRollbackOnly()
				}

			}
		}catch(Exception e){
			throw new Exception("[AccountController : get] : Exception - full stack trace follows:",e)
		}
	}

    def get(){
		try{
			Account acct = Account.get(params?.id?.toLong())
			return [account: acct]
		}catch(Exception e){
			throw new Exception("[AccountController : get] : Exception - full stack trace follows:",e)
		}
    }

	def delete() {
		try {
			Account acct = Account.get(params.id?.toLong())
			acct.delete(flush: true, failOnError: true)
			return [account: [id: params.id.toLong()]]
		}catch(Exception e){
			throw new Exception("#[AccountController : delete] : Exception - full stack trace follows:",e)
		}
	}

	// TODO : give account ownership to someone else
	// def changeOwner(){}

	// TODO : add acct user
	// def createAcctUser(){}

	protected boolean isSuperuser() {
		springSecurityService.principal.authorities*.authority.any { grailsAccount.config.apitoolkit.admin.roles.contains(it) }
	}

}

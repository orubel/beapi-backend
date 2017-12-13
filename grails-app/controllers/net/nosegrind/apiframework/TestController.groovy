package net.nosegrind.apiframework

class TestController {

    def springSecurityService

    def show(){
        try{
            Test test
            test = Test.get(params?.id?.toLong())
            return [test: test]
        }catch(Exception e){
            throw new Exception("[PersonController : get] : Exception - full stack trace follows:",e)
        }
    }

    def create(){
        Test test = new Test(name:"${params.name}")
        if(!test.save(flush:true,failOnError:true)){
            test.errors.allErrors.each { log.error it }
        }
        return [test:test]
    }

    def delete() {
        try {
            Test test
            test = Test.get(params?.id?.toLong())
            test.delete(flush: true, failOnError: true)
            return [test: [id: params.id.toLong()]]
        }catch(Exception e){
            throw new Exception("#[PersonController : delete] : Exception - full stack trace follows:",e)
        }
    }

    def testHook() {
        Test test = Test.get(params?.id?.toLong())
        return [test:test]
    }

    protected boolean isSuperuser() {
        springSecurityService.principal.authorities*.authority.any { grailsApplication.config.apitoolkit.admin.roles.contains(it) }
    }
}

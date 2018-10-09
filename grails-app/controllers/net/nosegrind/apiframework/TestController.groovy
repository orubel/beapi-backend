package net.nosegrind.apiframework

class TestController {

    def springSecurityService

    LinkedHashMap show(){
        try{
            Test test
            test = Test.get(params?.id?.toLong())
            return [test: test]
        }catch(Exception e){
            throw new Exception("[PersonController : get] : Exception - full stack trace follows:",e)
        }
    }

    LinkedHashMap create(){
        Test test = new Test(name:"${params.name}")
        if(!test.save(flush:true,failOnError:true)){
            test.errors.allErrors.each { log.error it }
        }
        return [test:test]
    }

    LinkedHashMap delete() {
        try {
            Test test
            test = Test.get(params?.id?.toLong())
            test.delete(flush: true, failOnError: true)
            return [test: [id: params.id.toLong()]]
        }catch(Exception e){
            throw new Exception("#[PersonController : delete] : Exception - full stack trace follows:",e)
        }
    }

    LinkedHashMap testHook() {
        Test test = Test.get(params?.id?.toLong())
        return [test:test]
    }

    protected boolean isSuperuser() {
        springSecurityService.principal.authorities*.authority.any { grailsApplication.config.apitoolkit.admin.roles.contains(it) }
    }
}

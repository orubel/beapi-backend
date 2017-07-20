package net.nosegrind.apiframework

class TestController {

    def springSecurityService

    def getPerson() {
        def person = App.get(params.id.toLong())
        return [test:person]
    }

    def testHook() {
        def person = App.get(springSecurityService.principal.id)
        return [test:person]
    }
}

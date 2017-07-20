package net.nosegrind.apiframework

import net.nosegrind.IpData
import net.nosegrind.apiframework.Person
import net.nosegrind.apiframework.Role
import net.nosegrind.apiframework.Hook
import net.nosegrind.apiframework.HookRole

class TestController {

    def springSecurityService

    def getPerson() {
        def person = Person.get(params.id.toLong())
        return [test:person]
    }

    def testHook() {
        def person = Person.get(springSecurityService.principal.id)
        return [test:person]
    }
}

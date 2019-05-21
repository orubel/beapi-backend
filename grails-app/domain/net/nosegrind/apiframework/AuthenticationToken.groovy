package net.nosegrind.apiframework

//@ToString(includeNames = true, includeFields = true)
class AuthenticationToken{

    //static mapWith = "mongo"

    String tokenValue
    String username

    static mapping = {
        cache true
        version false
    }

}

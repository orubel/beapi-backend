package net.nosegrind.apiframework

//@ToString(includeNames = true, includeFields = true)
class Arch{

	static transactional = true

	String url
	Integer cores
	Boolean hasCert = false
	Date certExpiration

	static constraints = {
		url blank: false, unique: true
		cores blank: false
		hasCert blank: false
		certExpiration nullable: true
	}

	static mapping = {
		cache true
	}

}

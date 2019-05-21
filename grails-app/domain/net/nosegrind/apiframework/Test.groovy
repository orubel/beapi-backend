package net.nosegrind.apiframework

class Test{

	String name

	static constraints = {
		name blank: false
	}

	static mapping = {
		cache false
	}

}

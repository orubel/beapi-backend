import grails.util.Metadata


String apiVersion = Metadata.current.getApplicationVersion()
// fix for dots not working with spring security pathing
String entryPoint = "/v${apiVersion}".toString()
String batchEntryPoint = "/b${apiVersion}".toString()
String chainEntryPoint = "/c${apiVersion}".toString()
String metricsEntryPoint = "/p${apiVersion}".toString()
//String domainEntryPoint = "/d${apiVersion}".toString()



// move to RequestMap once stabilized
grails.plugin.springsecurity.securityConfigType = "InterceptUrlMap"
grails.plugin.springsecurity.rejectIfNoRule = false
grails.plugin.springsecurity.fii.rejectPublicInvocations = false


grails.plugin.springsecurity.userLookup.userDomainClassName = 'net.nosegrind.apiframework.Person'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'net.nosegrind.apiframework.PersonRole'
grails.plugin.springsecurity.authority.className = 'net.nosegrind.apiframework.Role'

// grails.plugin.springsecurity.rememberMe.persistent = true
// grails.plugin.springsecurity.rememberMe.persistentToken.domainClassName = 'net.nosegrind.apiframework.PersistentLogin'		  // grails.plugin.springsecurity.rememberMe.persistentToken.domainClassName = 'net.nosegrind.apiframework.PersistentLogin'
grails.server.port.https = 8443

grails.plugin.springsecurity.adh.errorPage = null


grails.plugin.springsecurity.providerNames = ['daoAuthenticationProvider', 'anonymousAuthenticationProvider', 'rememberMeAuthenticationProvider']

grails.plugin.springsecurity.rememberMe.alwaysRemember = true
grails.plugin.springsecurity.rememberMe.cookieName = 'apiTest'
grails.plugin.springsecurity.rememberMe.key = '_grails_'

grails.plugin.springsecurity.logout.postOnly = false
grails.plugin.springsecurity.ui.encodePassword = false
grails.plugin.springsecurity.auth.forceHttps = false
grails.plugin.springsecurity.auth.loginFormUrl = '/login/auth/'
grails.plugin.springsecurity.auth.ajaxLoginFormUrl = '/login/authAjax/'

grails.plugin.springsecurity.successHandler.defaultTargetUrl = '/login/ajaxSuccess'
grails.plugin.springsecurity.failureHandler.defaultFailureUrl = '/login/ajaxDenied'
grails.plugin.springsecurity.failureHandler.ajaxAuthFailUrl = '/login/ajaxDenied'

// DBREVERSEENGINEER
//grails.plugin.reveng.packageName = "io.beapi"
//grails.plugin.reveng.manyToManyBelongsTos = 'none'

grails.plugin.springsecurity.filterChain.chainMap = [
		[pattern: "${entryPoint}/**",filters:'corsSecurityFilter,tokenCacheValidationFilter'],
		[pattern: '/api/login', filters: 'corsSecurityFilter,restAuthenticationFilter'],
		[pattern: '/api/logout', filters: 'corsSecurityFilter,restLogoutFilter'],
		[pattern: "${batchEntryPoint}/**", filters:'corsSecurityFilter,tokenCacheValidationFilter'],
		[pattern: "${chainEntryPoint}/**", filters:'corsSecurityFilter,tokenCacheValidationFilter'],
		[pattern: "${metricsEntryPoint}/**", filters:'corsSecurityFilter,tokenCacheValidationFilter'],
		[pattern: "${domainEntryPoint}/**", filters:'corsSecurityFilter,tokenCacheValidationFilter'],
]

quartz {
	autoStartup = true
	jdbcStore = false
}

grails.plugin.springsecurity.interceptUrlMap = [
		[pattern:"/api/**",            	access:["permitAll && \"{'GET','PUT','POST','DELETE','OPTIONS'}\".contains(request.getMethod())"]],
		[pattern:"/${entryPoint}/**",   access:["permitAll && \"{'GET','PUT','POST','DELETE','OPTIONS'}\".contains(request.getMethod())"]],
		[pattern:"/${batchEntryPoint}/**",   access:["permitAll && \"{'GET','PUT','POST','DELETE','OPTIONS'}\".contains(request.getMethod())"]],
		[pattern:"/${chainEntryPoint}/**",   access:["permitAll && \"{'GET','PUT','POST','DELETE','OPTIONS'}\".contains(request.getMethod())"]],
		[pattern:"/${metricsEntryPoint}/**",   access:["permitAll && \"{'GET','PUT','POST','DELETE','OPTIONS'}\".contains(request.getMethod())"]],
		[pattern:'/',                   access:['permitAll']],
		[pattern:'/error',              access:['permitAll']],
		[pattern:'/error/**',           access:['permitAll']],
		[pattern:'/index',              access:['permitAll']],
		[pattern:'/assets/**',          access:['permitAll']],
		[pattern: '/**', access: ['denyAll'], httpMethod: 'GET'],
		[pattern: '/**', access: ['denyAll'], httpMethod: 'POST'],
		[pattern: '/**', access: ['denyAll'], httpMethod: 'PUT'],
		[pattern: '/**', access: ['denyAll'], httpMethod: 'DELETE']
]

grails.plugin.springsecurity.rest.login.active  = true
grails.plugin.springsecurity.rest.login.endpointUrl = '/api/login'
grails.plugin.springsecurity.rest.logout.endpointUrl = '/api/logout'
grails.plugin.springsecurity.rest.login.failureStatusCode = '401'
//grails.plugin.springsecurity.rest.login.useRequestParamsCredentials  = true
grails.plugin.springsecurity.rest.login.useJsonCredentials  = true
grails.plugin.springsecurity.rest.login.usernamePropertyName =  'username'
grails.plugin.springsecurity.rest.login.passwordPropertyName =  'password'

server.useForwardHeaders = false


grails.plugin.springsecurity.rest.token.generation.useSecureRandom  = true
grails.plugin.springsecurity.rest.token.generation.useUUID  = false

grails.plugin.springsecurity.rest.token.storage.useGorm = true
grails.plugin.springsecurity.rest.token.storage.gorm.tokenDomainClassName   = 'net.nosegrind.apiframework.AuthenticationToken'
grails.plugin.springsecurity.rest.token.storage.gorm.tokenValuePropertyName = 'tokenValue'
grails.plugin.springsecurity.rest.token.storage.gorm.usernamePropertyName   = 'username'

grails.plugin.springsecurity.rest.token.rendering.usernamePropertyName  = 'username'
grails.plugin.springsecurity.rest.token.rendering.authoritiesPropertyName = 'authorities'

// grails.plugin.springsecurity.rest.token.validation.useBearerToken = false
//grails.plugin.springsecurity.rest.token.validation.active   = true
grails.plugin.springsecurity.rest.token.validation.endpointUrl  = '/api/validate'

grails.plugin.springsecurity.rememberMe.alwaysRemember = true
grails.plugin.springsecurity.rememberMe.persistent = false
//grails.plugin.springsecurity.rememberMe.persistentToken.domainClassName = 'net.nosegrind.apiframework.PersistentLogin'

// makes the application easier to work with
grails.plugin.springsecurity.logout.postOnly = false

grails.plugin.springsecurity.useSecurityEventListener = false


// Added by the Reactive API Framework plugin:
apitoolkit.attempts= 5
apitoolkit.roles= ['ROLE_USER','ROLE_ROOT','ROLE_ADMIN','ROLE_ARCH']
apitoolkit.chaining.enabled= true
apitoolkit.batching.enabled= true


grails.server.port.http = 8080
grails.server.port.https = 8443

apitoolkit.encoding= 'UTF-8'
apitoolkit.user.roles= ['ROLE_USER']
apitoolkit.admin.roles= ['ROLE_ROOT','ROLE_ADMIN','ROLE_ARCH']
apitoolkit.serverType= 'master'
apitoolkit.webhook.services= ['iostate']
apitoolkit.iostate.preloadDir= '/home/orubel/.iostate'
apitoolkit.corsInterceptor.includeEnvironments= ['development','test']
apitoolkit.corsInterceptor.excludeEnvironments= ['production']
apitoolkit.corsInterceptor.allowedOrigins= ['localhost:3000']

auth {
	google {
		key = 'AIzaSyDnMuPve6n9UboQnja4VO7Yx2VTlZM36sE'
		secret = ''
	}
	twitter {
		key = ''
		secret = ''
	}
	github {
		key = ''
		secret = ''
	}
}




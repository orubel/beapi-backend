package net.nosegrind.apiframework

class StatsController{
	
	def springSecurityService
	def statsService

	LinkedHashMap show() {
		Integer day = (Integer) System.currentTimeMillis()/((1000*60*60*24)+1)
		switch(params.type){
			case 3:
				// 12 months
				return [stats:statsService.getStatsByYear(day)]
				break
			case 2:
				// 4 weeks / 1 month
				return [stats:statsService.getStatsByMonth(day)]
				break
			case 1:
				// 7 days / 1 week
				return [stats:statsService.getStatsByWeek(day)]
				break
			case 0:
			default:
				// 1 day
				return [stats:statsService.getStats(day)]
				break
		}
	}
}

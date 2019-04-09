import net.nosegrind.apiframework.StatsService

class StatsSchedJob {

    def statsService

    static triggers = {
        cron name: 'myTrigger2', cronExpression: "0 */1 * ? * *"
    }


    def execute() {
	println "Job Run"
        def temp = statsService.getStatsCache()
	println(temp)
	
	//temp.each(){
	//	println(it)
	//}
    }

}

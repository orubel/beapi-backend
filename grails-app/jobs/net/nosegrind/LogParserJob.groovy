package net.nosegrind


import java.nio.charset.StandardCharsets
import groovy.json.JsonSlurper
import grails.core.GrailsApplication
import static groovyx.gpars.GParsPool.withPool
import static groovyx.gpars.GParsPool.withExistingPool
import jsr166y.ForkJoinPool;

class LogParserJob {

    def concurrent = false
    def sessionRequired = true

    GrailsApplication grailsApplication
    def persistenceInterceptor

    static triggers = {
        //waits one minutes and repeats every 10 minutes
        //simple name: 'logTrigger', startDelay: 60000, repeatInterval: 60000*10
        // hourly
        // cron name: 'logTrigger', cronExpression: "0 45 * * * ?"
    }

    def execute() {
        String url = "${grailsApplication.config.perf.url}"
        def conn = url.toURL().openConnection()
        conn.setRequestMethod("GET")
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setFollowRedirects(false)
        conn.setConnectTimeout(15 * 1000)
        conn.setReadTimeout(15*1000)
        conn.setDoOutput(true)
        conn.setDoInput(true)
        conn.connect()

        if (conn.responseCode == 200) {
            StringBuilder sb = new StringBuilder()
            BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
            for (int c; (c = input.read()) != -1;) {
                sb.append((char) c)
            }

            def slurper = new JsonSlurper()
            def json = slurper.parseText(sb.toString())

            withPool(4) { pool ->
                if (json) {
                    json.eachParallel {

                        // PARSE TOOLTYPE
                        String tool = it.get('tool-name')
                        String protocol = it.get('ip-transport-protocol')
                        if (tool != null && !tool.isEmpty()) {
                            Integer ttype = getToolType(tool, protocol)

                            // PARSE LOCATION
                            String locUrl = it.get('url') - it.get('uri')
                            String locUri = it.get('uri')
                            String locProtocol = getProtocol(it.get('url'))
                            Integer measurementLocation = getLocation(locProtocol, locUrl, true, locUri)

                            // PARSE RETURN_SOURCE
                            String locUrl2 = it.get('destination')
                            locProtocol = getProtocol(locUrl2)
                            Integer returnSource = getLocation(locProtocol, locUrl2, false)

                            // PARSE RETURN_DESTINATION
                            String locUrl3 = it.get('source')
                            locProtocol = getProtocol(locUrl3)
                            Integer returnDestination = getLocation(locProtocol, locUrl3, false)

                            // PARSE MEASUREMENT_AGENT
                            String locUrl4 = it.get('measurement-agent')
                            locProtocol = getProtocol(locUrl4)
                            Integer measurementAgent = getLocation(locProtocol, locUrl4, false)

                            // PARSE INPUT_SOURCE
                            String locUrl5 = it.get('input-source')
                            locProtocol = getProtocol(locUrl5)
                            Integer inputSource = getLocation(locProtocol, locUrl5, false)

                            // PARSE INPUT_DESTINATION
                            String locUrl6 = it.get('input-destination')
                            locProtocol = getProtocol(locUrl6)
                            Integer inputDestination = getLocation(locProtocol, locUrl6, false)

                            // PARSE EVENT
                            String mkey = it.get('metadata-key')
                            Integer traceMaxTtl = (it.get('trace-max-ttl') == null) ? 0 : it.get('trace-max-ttl').toInteger()
                            Integer timeInterval = (it.get('time-interval') == null) ? 0 : ((it.get('time-interval') != null)?it.get('time-interval').toInteger():0)
                            Integer ipPacketSize = (it.get('ip-packet-size') == null) ? 0 : ((it.get('time-interval') != null)?it.get('time-interval').toInteger():0)
                            if(!ttype==null) {
                                Integer archive = getArchive(measurementLocation, mkey, ttype, returnSource, returnDestination, measurementAgent, inputSource, inputDestination, traceMaxTtl, timeInterval, ipPacketSize)

                                // PARSE EVENTS/EVENT TYPES
                                def events = it.get('event-types')

                                events.each() { it2 ->
                                    String eventType = it2.get('event-type')
                                    Integer eType = getEventType(eventType)
                                    String baseUri = it2.get('base-uri') - locUri

                                    // PARSE SUMMARIES / SUMMARY TYPE
                                    def summaries = it2.get('summaries')
                                    if (summaries.size() > 0) {
                                        summaries.each() { it3 ->
                                            String summType = it3.get('summary-type')
                                            Integer sType = getSummaryType(summType)

                                            String summUri = it3.get('uri') - locUri
                                            Long summTime = (it3.get('time-updated') == null) ? System.currentTimeMillis() : it3.get('time-updated').toLong()
                                            Integer window = it3.get('summary-window').toInteger()
                                            // if summary window != '1 hour', ignore; this may change or become app property in future
                                            if (window == 3600) {

                                                // locUrl / locUri / summUri
                                                // Integer event = getEvent(archive, baseUri, eType, summUri, sType)

                                                String newUrl = "${locUrl}${locUri}${summUri}?format=json&limit=100000000".toString()
                                                String newBaseUri = "${locUrl}${locUri}${baseUri}?format=json&limit=100000000".toString()
                                                parseReport(archive, baseUri, eType, summUri, sType, newUrl, eventType, summTime, pool)
                                            }
                                        }
                                    } else {
                                        Long summTime = (it2.get('time-updated') == null) ? System.currentTimeMillis() : it2.get('time-updated').toLong()
                                        String newBaseUri = "${locUrl}${locUri}${baseUri}?format=json&limit=100000000".toString()
                                        parseData(archive, baseUri, eType, newBaseUri, eventType, summTime, pool)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (conn.content.text != 'connected') {
                log.info("[main.scripts.net.nosegrind.apiframework.ApiParseService:getApi] : Connection failed for ${url}")
                return null
            }
        }
    }

    private Integer getSummaryType(String type){
        persistenceInterceptor.init()
        def sType = SummaryType.executeQuery('select id from SummaryType where name=?',[type])
        def id = sType[0]
        if(!id){
            sType = new SummaryType(name:type)
            sType.save(flush:true,failOnError:true)
            id = sType.id
        }
        persistenceInterceptor.flush()
        return id
    }

    private Integer getEventType(String type){
        persistenceInterceptor.init()
        def eType = EventType.executeQuery('select id from EventType where name=?',[type])
        def id = eType[0]
        if(!id){
            eType = new EventType(name:type)
            eType.save(flush:true,failOnError:true)
            id = eType.id
        }
        persistenceInterceptor.flush()
        return id
    }

    private Integer getArchive(Integer measurementLocation, String mkey, Integer ttype, Integer returnSource, Integer returnDestination, Integer measurementAgent, Integer inputSource, Integer inputDestination, Integer traceMaxTtl, Integer timeInterval, Integer ipPacketSize){
        persistenceInterceptor.init()
        def archive = Arch.executeQuery('select id from Arch where metadataKey=?',[mkey])
        def id = archive[0]
        if(!id){
            archive = new Arch(
                    inputDestination:inputDestination,
                    inputSource:inputSource,
                    ipPacketSize:ipPacketSize,
                    measurementAgent:measurementAgent,
                    measurementLocation:measurementLocation,
                    metadataKey:mkey,
                    returnDestination:returnDestination,
                    returnSource:returnSource,
                    timeInterval:timeInterval,
                    toolType:ttype,
                    traceMaxTtl:traceMaxTtl
            )
            archive.save(flush:true,failOnError:true)
            id = archive.id
        }
        persistenceInterceptor.flush()
        return id
    }

    private String getProtocol(String url){
        def finder = (url =~ /(http|https).+/)
        if (finder.matches()) {
            return finder[0][1]
        }else{
            return 'ip'
        }
    }

    private Integer getToolType(String tool, String protocol){
        persistenceInterceptor.init()
        def tType = ToolType.executeQuery('select id from ToolType where toolName=?',[tool])
        def id = (tType[0])?tType[0]:null
        if(id==null && (tool!=null && protocol!=null)){
            tType = new ToolType(name:tool, protocol:protocol)
            tType.save(flush:true,failOnError:true)
            id = tType.id
        }
        persistenceInterceptor.flush()
        return id
    }

    private Integer getLocation(String protocol, String url, boolean metadataKey=true, String uri=null) {
        def loc
        def id
        persistenceInterceptor.init()
        if(uri != null) {
            loc = Location.executeQuery("select id from Location where url=? and uri=?", [url,uri])
            id = loc[0]
        }else{
            loc = Location.executeQuery("select id from Location where url=? and uri is null",[url])
            id = loc[0]
        }

        if(!id){
            String insertQuery

            if(uri!=null){
                loc = new Location(url:url, uri:uri, protocol:protocol, hasMetadataKey:metadataKey)
            }else{
                loc = new Location(url:url, protocol:protocol, hasMetadataKey:metadataKey)
            }
            loc.save(flush:true,failOnError:true)
            id = loc.id
        }
        persistenceInterceptor.flush()
        return id
    }

    private void parseReport(Integer archive, String baseUri, Integer type, String summUri, Integer sType, String newUrl, String eType, Long summTime, ForkJoinPool pool) {
        //println("... parsing report for ${archive}")
        def conn = newUrl.toURL().openConnection()
        conn.setRequestMethod("GET")
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setDoOutput(true)
        conn.setDoInput(true)
        conn.connect()
        //def writer = new OutputStreamWriter(conn.outputStream)
        //writer.write(queryString)
        //writer.flush()
        //writer.close()

        switch (eType) {
            case 'packet-loss-rate':
                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder()
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    for (int c; (c = input.read()) != -1;) {
                        sb.append((char) c)
                    }
                    def slurper = new groovy.json.JsonSlurper()
                    def json = slurper.parseText(sb.toString())

                    if (json) {
                        Long packetReportId = createPacketReport(archive, type, baseUri, summUri, sType, summTime)
                        withExistingPool(pool) {
                            json.eachParallel { it2 ->
                                String xAxis = it2['ts']
                                String yAxis = it2['val']
                                if (Float.parseFloat(yAxis) > 0) {
                                    if(packetReportId) {
                                        persistenceInterceptor.init()
                                        def plr = new PacketLossRate(report: packetReportId, xAxis: xAxis, yAxis: yAxis)
                                        plr.save(flush: true, failOnError: true)
                                        persistenceInterceptor.flush()
                                    }
                                }

                            }
                        }
                    }
                }
                break
            case 'histogram-owdelay':
                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder()
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    for (int c; (c = input.read()) != -1;) {
                        sb.append((char) c)
                    }
                    def slurper = new groovy.json.JsonSlurper()
                    def json = slurper.parseText(sb.toString())

                if (json) {
                    Long packetReportId = createPacketReport(archive, type, baseUri, summUri, sType, summTime)

                    withExistingPool(pool) {
                        json.eachParallel { it2 ->

                            if (it2 != null) {
                                String timestamp = it2['ts']
                                def tmp = it2.get('val')
                                List valKeys = tmp.keySet() as List

                                if (valKeys.contains('variance')) {
                                    // get statistics
                                    LinkedHashMap val = it2['val']
                                    String serialVal = val.inspect()

                                    //persistenceInterceptor.init()
                                    if(packetReportId) {
                                        def report = PacketReport.get(packetReportId)
                                        def od = OwDelay.findByTimestampAndReport(timestamp, report)
                                        if (od?.id) {
                                            List mode = val['mode'] as List
                                            def owd = OwDelay.executeUpdate("update OwDelay set maximum=${val['maximum']},mean=${val['mean']},median=${val['median']},minimum=${val['minimum']},mode=${mode[0]},standard_deviation=${val['standard-deviation']},variance=${val['variance']} where id=?", [od.id])

                                        } else {
                                            //Serialize val for storage as one value for retrieval
                                            val = it2['val']
                                            serialVal = val.inspect()
                                            def owd = new OwDelay(report: packetReportId, timestamp: timestamp, vals: serialVal)
                                            owd.save(flush: true, failOnError: true)
                                        }
                                    }
                                    //persistenceInterceptor.flush()
                                }
                            }
                        }
                    }
                    break
                }
            }
        }
    }

    private void parseData(Integer archive, String baseUri, Integer type, String newUrl, String eType, Long summTime, ForkJoinPool pool) {
        def conn = newUrl.toURL().openConnection()
        conn.setRequestMethod("GET")
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setDoOutput(true)
        conn.setDoInput(true)
        conn.connect()
        //def writer = new OutputStreamWriter(conn.outputStream)
        //writer.write(queryString)
        //writer.flush()
        //writer.close()

    }

    private Long createPacketReport(Integer archive, Integer type, String baseUri, String summUri, Integer sType, Long summTime) {
        def prep = PacketReport.executeQuery('select id from PacketReport as PR where PR.arch.id=? and PR.eventType.id=?', [archive.toLong(), type.toLong()])
        def id = prep[0]
        if (!id) {
            if (sType == null && summUri == null) {
                prep = new PacketReport(arch: archive, baseUri: baseUri, eventType: type, timestamp: summTime)
            } else if (sType == null) {
                prep = new PacketReport(arch: archive, baseUri: baseUri, eventType: type, summaryUri: summUri, timestamp: summTime)
            } else if (summUri == null) {
                prep = new PacketReport(arch: archive, baseUri: baseUri, eventType: type, summaryType: sType, timestamp: summTime)
            } else {
                prep = new PacketReport(arch: archive, baseUri: baseUri, eventType: type, summaryType: sType, summaryUri: summUri, timestamp: summTime)
            }
            prep.save(flush: true, failOnError: true)
            id = prep.id
        }
        return id as Long
    }

}

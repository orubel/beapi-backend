#!/usr/bin/env groovy

@Grapes([
        @Grab('mysql:mysql-connector-java:5.1.25')
])
@GrabConfig(systemClassLoader = true)
import groovy.sql.Sql
import java.nio.charset.StandardCharsets
import java.lang.System
import java.io.File
import java.sql.DriverManager

public class ApiParseService {

    static transactional = false
    static limit =1000000000
    static login = 'db_login'
    static password = 'db_pass'
    static jdbc = "jdbc:mysql://localhost/scheduler"

    def sql = null
    /*
    LinkedHashMap events = [
            'time-error-estimates':'Time Error Estimates',
            'packet-duplicates':'Duplicate Packets',
            'histogram-ttl':'OWAMP TTL',
            'packet-count-sent':'Packets Sent',
            'packet-count-lost':'Packets Lost',
            'histogram-owdelay':'Owamp One-way Delay',
            'failures':'Failures',
            'packet-loss-rate':'Querying Packet Loss',
            'packet-trace':'Querying Packet Traces',
            'path-mtu':'MTU Path',
            'throughput-subintervals':'Querying Subinterval Data',
            'packet-retransmits':'Packet Retransmits',
            'throughput':'Querying Throughput',
            'packet-retransmits-subintervals':'Packet Retransmit Intervals'
    ]
    */

    public boolean parseArchive(String url) {

        this.sql = Sql.newInstance(jdbc,"${login}","${password}","com.mysql.jdbc.Driver")

            def conn = url.toURL().openConnection()
            conn.setRequestMethod("GET")
            conn.setRequestProperty("Accept", "*/*")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setDoOutput(true)
            conn.setDoInput(true)
            conn.connect()



            StringBuilder sb = new StringBuilder()
            BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
            for (int c; (c = input.read()) != -1;) {
                sb.append((char) c)
            }

            def slurper = new groovy.json.JsonSlurper()
            def json = slurper.parseText(sb.toString())
            json.each() {

                // PARSE TOOLTYPE
                String tool = it.get('tool-name')
                String protocol = it.get('ip-transport-protocol')
                if (tool != null && protocol != null) {
                    Integer ttype = getToolType(tool, protocol)

                 // PARSE LOCATION
                    String locUrl =  it.get('url') - it.get('uri')
                    String locUri = it.get('uri')
                    String locProtocol = getProtocol(it.get('url'))
                    Integer measurementLocation = getLocation(locProtocol, locUrl, true, locUri)

                 // PARSE RETURN_SOURCE
                    String locUrl2 =  it.get('destination')
                    locProtocol = getProtocol(locUrl2)
                    Integer returnSource = getLocation(locProtocol, locUrl2, false)

                 // PARSE RETURN_DESTINATION
                    String locUrl3 =  it.get('source')
                    locProtocol = getProtocol(locUrl3)
                    Integer returnDestination = getLocation(locProtocol, locUrl3, false)

                 // PARSE MEASUREMENT_AGENT
                    String locUrl4 =  it.get('measurement-agent')
                    locProtocol = getProtocol(locUrl4)
                    Integer measurementAgent = getLocation(locProtocol, locUrl4, false)

                 // PARSE INPUT_SOURCE
                    String locUrl5 =  it.get('input-source')
                    locProtocol = getProtocol(locUrl5)
                    Integer inputSource = getLocation(locProtocol, locUrl5, false)

                 // PARSE INPUT_DESTINATION
                    String locUrl6 =  it.get('input-destination')
                    locProtocol = getProtocol(locUrl6)
                    Integer inputDestination = getLocation(locProtocol, locUrl6, false)

                 // PARSE EVENT
                    String mkey = it.get('metadata-key')
                    Integer traceMaxTtl = (it.get('trace-max-ttl')==null)?0:it.get('trace-max-ttl').toInteger()
                    Integer timeInterval = (it.get('time-interval')==null)?0:it.get('time-interval').toInteger()
                    Integer ipPacketSize = (it.get('ip-packet-size')==null)?0:it.get('time-interval').toInteger()
                    Integer archive = getArchive(measurementLocation, mkey, ttype, returnSource, returnDestination, measurementAgent, inputSource, inputDestination, traceMaxTtl, timeInterval, ipPacketSize)

                 // PARSE EVENTS/EVENT TYPES
                    def events = it.get('event-types')
                    events.each(){ it2 ->
                        String eventType = it2.get('event-type')
                        Integer eType = getEventType(eventType)
                        String baseUri = it2.get('base-uri')-locUri


                 // PARSE SUMMARIES / SUMMARY TYPE
                        def summaries = it2.get('summaries')
                        if(summaries.size()>0) {
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
                                    parseReport(archive, baseUri, eType, summUri, sType, newUrl, eventType, summTime)
                                }
                            }
                        }else{
                            Long summTime = (it2.get('time-updated') == null) ? System.currentTimeMillis() : it2.get('time-updated').toLong()
                            String newBaseUri = "${locUrl}${locUri}${baseUri}?format=json&limit=100000000".toString()
                            parseData(archive, baseUri, eType, newBaseUri, eventType, summTime)
                        }
                    }
                }
            }
            if( conn.content.text != 'connected' ) {
                log.info("[main.scripts.net.nosegrind.apiframework.ApiParseService:getApi] : Connection failed for ${url}")
                return null
            }
    }

    private Integer getSummaryType(String type){
        String selectQuery = "select id from summary_type where name=? limit 1"
        def sType = sql.firstRow(selectQuery,[type])
        if(!sType?.id){
            String insertQuery = "insert into summary_type values(null,0,?)"
            sql.execute(insertQuery,[type])
            sType = sql.firstRow(selectQuery,[type])
        }
        return sType?.id
    }

    private Integer getEventType(String type){
        String selectQuery = "select id from event_type where name=? limit 1"
        def eType = sql.firstRow(selectQuery,[type])
        if(!eType?.id){
            String insertQuery = "insert into event_type values(null,0,?)"
            sql.execute(insertQuery,[type])
            eType = sql.firstRow(selectQuery,[type])
        }
        return eType?.id
    }

    private Integer getArchive(Integer measurementLocation, String mkey, Integer ttype, Integer returnSource, Integer returnDestination, Integer measurementAgent, Integer inputSource, Integer inputDestination, Integer traceMaxTtl, Integer timeInterval, Integer ipPacketSize){
        String selectQuery = "select id from arch where metadata_key=? limit 1"
        def archive = sql.firstRow(selectQuery,[mkey])
        if(!archive?.id){
            String insertQuery = "insert into arch values(null,0,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            sql.execute(insertQuery,[inputDestination, inputSource, ipPacketSize, measurementAgent, measurementLocation, mkey, returnDestination, returnSource, timeInterval, ttype, traceMaxTtl])
            archive = sql.firstRow(selectQuery,[mkey])
        }
        return archive?.id
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
        String ttSelect = 'select id from tool_type where tool_name=? limit 1'
        def tType = sql.firstRow(ttSelect,[tool])
        if(!tType?.id){
            String ttInsert = "insert into tool_type values(null,0,?,?)"
            sql.execute(ttInsert,[protocol, tool])
            tType = sql.firstRow(ttSelect,[tool])
        }
        return tType?.id
    }

    private Integer getLocation(String protocol, String url, boolean metadataKey=true, String uri=null) {
        String selectQuery
        def loc
        if(uri != null) {
            selectQuery = "select id from location where url=? and uri=? limit 1"
            loc = sql.firstRow(selectQuery,[url,uri])
        }else{
            selectQuery = "select id from location where url=? and uri is null limit 1"
            loc = sql.firstRow(selectQuery,[url])
        }

        if(!loc?.id){
            String insertQuery

            if(uri!=null){
                insertQuery = "insert into location values(null,0,?,?,?,?)"
                sql.execute(insertQuery,[metadataKey, protocol, uri, url])
            }else{
                insertQuery = "insert into location values(null,0,?,?,null,?)"
                sql.execute(insertQuery,[metadataKey, protocol, url])
            }
            loc = (uri != null)? sql.firstRow(selectQuery,[url,uri]): sql.firstRow(selectQuery,[url])
        }
        return loc?.id
    }

    private Long createPacketReport(Integer archive, Integer type,String baseUri,String summUri,Integer sType,Long summTime){
        String selectQuery = "select id from packet_report where arch_id=? and event_type_id=? limit 1"
        def plReport = sql.firstRow(selectQuery, [archive, type])
        if (!plReport?.id) {
            String insertQuery = "insert into packet_report values(null,0,?,?,?,"
            if(sType==null){
                insertQuery+="null,"
            }else{
                insertQuery+="?,"
            }

            if(summUri==null){
                insertQuery+="null,?);"
            }else{
                insertQuery+="?,?);"
            }

            if(sType==null && summUri==null) {
                sql.execute(insertQuery, [archive, baseUri, type,summTime])
            }else if(sType==null){
                sql.execute(insertQuery, [archive, baseUri, type, summUri,summTime])
            }else if(summUri==null){
                sql.execute(insertQuery, [archive, baseUri, type, sType,summTime])
            }else{
                sql.execute(insertQuery, [archive, baseUri, type, sType, summUri,summTime])
            }

            plReport = sql.firstRow(selectQuery, [archive, type])
        }
        return plReport.id as Long
    }

    private void parseReport(Integer archive, String baseUri, Integer type, String summUri, Integer sType, String newUrl, String eType, Long summTime) {
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
                if(conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder()
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    for (int c; (c = input.read()) != -1;) {
                        sb.append((char) c)
                    }
                    def slurper = new groovy.json.JsonSlurper()
                    def json = slurper.parseText(sb.toString())

                    if (json) {
                        json.each() { it ->
                            String xAxis = it['ts']
                            String yAxis = it['val']
                            if (Float.parseFloat(yAxis) > 0) {

                                Long packetReportId = createPacketReport(archive, type, baseUri, summUri, sType, summTime)
                                println(eType+"/"+conn.getResponseCode())
                                String insertData = "insert into packet_loss_rate values(null,0,?,?,?)"
                                sql.execute(insertData, [packetReportId, xAxis, yAxis])
                            }

                        }
                    }
                }
                break
            case 'histogram-owdelay':
                println(eType+"/"+conn.getResponseCode())
                if(conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder()
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    for (int c; (c = input.read()) != -1;) {
                        sb.append((char) c)
                    }
                    def slurper = new groovy.json.JsonSlurper()
                    def json = slurper.parseText(sb.toString())

                    Long packetReportId = createPacketReport(archive, type, baseUri, summUri, sType, summTime)

                    if (json) {
                        json.each() { it ->
                            if (it != null) {
                                String timestamp = it['ts']
                                def tmp = it.get('val')
                                List valKeys = tmp.keySet() as List

                                if(valKeys.contains('variance')){
                                    // get statistics
                                    LinkedHashMap val = it['val']
                                    String serialVal = val.inspect()

                                    String selectQuery = "select id from ow_delay where timestamp=? and report_id=? limit 1"
                                    def owDelay = sql.firstRow(selectQuery, [timestamp, packetReportId])
                                    if (owDelay?.id) {
                                        println(eType+"/"+conn.getResponseCode())
                                        List mode = val['mode'] as List
                                        String updateQuery = "update ow_delay set maximum=${val['maximum']},mean=${val['mean']},median=${val['median']},minimum=${val['minimum']},mode=${mode[0]},standard_deviation=${val['standard-deviation']},variance=${val['variance']} where timestamp=? and report_id=?"
                                        sql.execute(updateQuery, [timestamp,packetReportId])
                                    }
                                }else{
                                    println(eType+"/"+conn.getResponseCode())
                                    //Serialize val for storage as one value for retrieval
                                    LinkedHashMap val = it['val']
                                    String serialVal = val.inspect()

                                    String insertQuery = "insert into ow_delay values(null,0,null,null,null,null,null,?,null,?,?,null)"
                                    sql.execute(insertQuery, [packetReportId,timestamp,serialVal])
                                }
                            }
                        }
                    }
                }
                break

        }
    }

    private LinkedHashMap parseIPV4(String ip){
        LinkedHashMap output = [:]
        def response = ["curl", "https://ipapi.co/${ip}/json/"].execute().text
        def slurper = new groovy.json.JsonSlurper()
        def ipData = slurper.parseText(response.toString())
        if (ipData) {
            output.city = ipData['city']
            output.region = ipData['region']
            output.country = ipData['country']
            output.postal = ipData['postal']
            output.latitude = ipData['latitude']
            output.longitude = ipData['longitude']
            output.timezone = ipData['timezone']
            output.reserved = false
            // check for reserved
        }
        return output
    }

    private LinkedHashMap parseIPV6(String ip){
        LinkedHashMap output = [:]
        def response = ["curl", "https://tools.keycdn.com/geo.json?host=${ip}"].execute().text
        def slurper = new groovy.json.JsonSlurper()
        def ipData = slurper.parseText(response.toString())
        if (ipData.status=='success') {
            output.city = ipData['data']['geo']['city']
            output.region = ipData['data']['geo']['region']
            output.country = ipData['data']['geo']['country_code']
            output.postal = ipData['data']['geo']['postal_code']
            output.latitude = ipData['data']['geo']['latitude']
            output.longitude = ipData['data']['geo']['longitude']
            output.timezone = ipData['data']['geo']['timezone']
        }
        return output
    }

    private void parseData(Integer archive, String baseUri, Integer type, String  newUrl, String eType, Long summTime){
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
            case 'packet-count-lost':
                /*
                * no data
                *
                if(conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder()
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    for (int c; (c = input.read()) != -1;) {
                        sb.append((char) c)
                    }
                    def slurper = new groovy.json.JsonSlurper()
                    def json = slurper.parseText(sb.toString())

                    if (json) {
                        json.each() { it ->
                            println("packet-count-lost : " + it)
                        }
                    }
                }
                */
                break
            case 'time-error-estimates':
                // no data
                break
            case 'packet-duplicates':
                // 500 errors
                break
            case 'histogram-ttl':
                // no data
                break
            case 'packet-count-sent':
                // 500 errors
                break
            case 'failures':
                // unimportant; related to perfsonar performance
                break
            case 'packet-trace':
                /*
                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder()
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    for (int c; (c = input.read()) != -1;) {
                        sb.append((char) c)
                    }
                    def slurper = new groovy.json.JsonSlurper()
                    def json = slurper.parseText(sb.toString())

                    if (json) {
                        def lastIP = null
                        def lastTTL = null
                        def lastRTT = 0
                        json.each() { it ->
                            String timestamp = it['ts']
                            List values = it['val'] as List
                            values.each(){ it2 ->

                                LinkedHashMap val = it2 as LinkedHashMap
                                String ip = it2['ip']
                                Integer ttl = it2['ttl'].toInteger()
                                String rtt = it2['rtt']

                                if(rtt) {
                                    def diff = (lastRTT == 0) ? rtt : rtt - lastRTT
                                    lastRTT = rtt
                                    String mtu = it2['mtu']

                                    // Get Location First
                                    String ipQuery = "select id from location where url='${ip}' and protocol='ip' limit 1"
                                    def ipResults = sql.firstRow(ipQuery)
                                    def ipId = null
                                    if (ipResults?.id) {
                                        ipId = ipResults?.id
                                    }

                                    if (ipId == null) {
                                        String insertQuery = "insert into location values(null,0,false,'ip',null,'${ip}')"
                                        def iptrace = sql.executeInsert(insertQuery)
                                        if (iptrace[0]) {
                                            ipId = iptrace[0][0].toInteger()
                                        }
                                    }

                                    // set packet trace data
                                    Long packetReportId = createPacketReport(archive, type, baseUri, null, null, summTime)

                                    String selectQuery = "select id from packet_trace where timestamp=? and report_id=? limit 1"
                                    def ptrace = sql.firstRow(selectQuery, [timestamp, packetReportId])
                                    if (!ptrace?.id && ipId) {
                                        println(eType + "/" + conn.getResponseCode())
                                        String insertQuery2 = "insert into packet_trace values(null,0,?,?,?,?,?)"
                                        sql.execute(insertQuery2, [ipId, packetReportId, rtt, timestamp, ttl])
                                    }
                                }
                            }

                        }
                    }
                }
                */
                break
            case 'path-mtu':
                /*
                * 200's but no data
                *
                println(eType+"/"+conn.getResponseCode())
                if(conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder()
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    for (int c; (c = input.read()) != -1;) {
                        sb.append((char) c)
                    }
                    def slurper = new groovy.json.JsonSlurper()
                    def json = slurper.parseText(sb.toString())

                    if (json) {
                        json.each() { it ->
                            println("path-mtu : " + it)
                        }
                    }
                }
                */
                break
            case 'throughput-subintervals':
                println(eType+"/"+conn.getResponseCode())
                if(conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder()
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    for (int c; (c = input.read()) != -1;) {
                        sb.append((char) c)
                    }
                    def slurper = new groovy.json.JsonSlurper()
                    def json = slurper.parseText(sb.toString())

                    if (json) {
                        json.each() { it ->
                            String timestamp = it['ts']
                            List values = it['val'] as List

                            Integer throughputId = null
                            Long packetReportId = createPacketReport(archive, type, baseUri, null, null, summTime)

                            String selectQuery = "select id from throughput where timestamp=? and report_id=? limit 1"
                            def tput = sql.firstRow(selectQuery, [timestamp, packetReportId])
                            if (!tput?.id) {
                                String insertQuery2 = "insert into throughput values(null,0,?,?)"
                                def tputResult = sql.executeInsert(insertQuery2, [packetReportId, timestamp])
                                if (tputResult[0]) {
                                    throughputId = tputResult[0][0].toInteger()
                                }
                            } else {
                                throughputId = tput.id
                            }

                            values.each() { it2 ->
                                String start = it2['start']
                                String duration = it2['duration']
                                String value = it2['val']

                                String insertQuery2 = "insert into sub_interval values(null,0,?,?,?)"
                                def subResult = sql.executeInsert(insertQuery2,[duration,start,value])

                                if (subResult[0]) {
                                    def subId = subResult[0][0].toInteger()

                                    String insertQuery3 = "insert into throughput_sub values(null,0,?,?)"
                                    sql.execute(insertQuery3, [subId,throughputId])
                                }
                            }
                        }
                    }
                }
                break
            case 'packet-retransmits':
                println(eType+"/"+conn.getResponseCode())
                if(conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder()
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    for (int c; (c = input.read()) != -1;) {
                        sb.append((char) c)
                    }
                    def slurper = new groovy.json.JsonSlurper()
                    def json = slurper.parseText(sb.toString())

                    if (json) {
                        json.each() { it ->
                            String timestamp = it['ts']
                            String value = it['val']

                            Long packetReportId = createPacketReport(archive, type, baseUri, null, null, summTime)

                            String insertQuery2 = "insert into packet_retransmit values(null,0,?,?,?)"
                            sql.execute(insertQuery2, [packetReportId, timestamp,value])

                        }
                    }
                }
                break
            case 'throughput':
                // no data
                break
            case 'packet-retransmits-subintervals':
                println(eType+"/"+conn.getResponseCode())
                if(conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder()
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    for (int c; (c = input.read()) != -1;) {
                        sb.append((char) c)
                    }
                    def slurper = new groovy.json.JsonSlurper()
                    def json = slurper.parseText(sb.toString())

                    if (json) {
                        json.each() { it ->
                            String timestamp = it['ts']
                            List values = it['val'] as List

                            Integer ptransmitId = null
                            Long packetReportId = createPacketReport(archive, type, baseUri, null, null, summTime)

                            String selectQuery = "select id from packet_retransmit where timestamp=? and report_id=? limit 1"
                            def tput = sql.firstRow(selectQuery, [timestamp, packetReportId])
                            if (!tput?.id) {
                                String insertQuery2 = "insert into packet_retransmit values(null,0,?,?,null)"
                                def tputResult = sql.executeInsert(insertQuery2, [packetReportId, timestamp])
                                if (tputResult[0]) {
                                    ptransmitId = tputResult[0][0].toInteger()
                                }
                            } else {
                                ptransmitId = tput.id
                            }

                            values.each() { it2 ->
                                String start = it2['start']
                                String duration = it2['duration']
                                String value = it2['val']

                                String insertQuery2 = "insert into sub_interval values(null,0,?,?,?)"
                                def subResult = sql.executeInsert(insertQuery2,[duration,start,value])

                                if (subResult[0]) {
                                    def subId = subResult[0][0].toInteger()

                                    String insertQuery3 = "insert into packet_retransmit_sub values(null,0,?,?)"
                                    sql.execute(insertQuery3, [ptransmitId, subId])
                                }
                            }
                        }

                    }
                }
                break
        }
    }
}

ApiParseService apiParseService = new ApiParseService()
apiParseService.parseArchive('http://perf-scidmz-data.cac.washington.edu/esmond/perfsonar/archive/?format=json&limit=100000000')

// IPV6
// curl "https://tools.keycdn.com/geo.json?host={IP or hostname}"


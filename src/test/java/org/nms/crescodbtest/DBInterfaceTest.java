package org.nms.crescodbtest;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.cresco.agent.controller.globalscheduler.pNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.json.simple.*;
import org.json.simple.parser.*;
import testhelpers.ControllerEngine;
import testhelpers.CrescoHelpers;
import testhelpers.GDBConf;
import testhelpers.OrientHelpers;



import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DBInterfaceTest {
    private ControllerEngine ce;
    private DBManager dbMan;

    private final GDBConf testingConf = new GDBConf("test_runner","test","localhost","cresco_test");
    //private final GDBConf adminConf = new GDBConf("root","root","localhost","cresco_test");

    //private final String local_db_path = "/home/nima/bin/orientdb-2.2.36/databases";
    //private final String db_export_file_path = "/home/nima/bin/orientdb-2.2.36/backup_for_testing.zip";
    private final String db_export_file_path = "/home/nima/code/IdeaProjects/cresco_db_rewrite/src/test/resources/global_region_agent.gz";
    private final String regional_db_export_file_path = "/home/nima/code/IdeaProjects/cresco_db_rewrite/src/test/resources/cresco_regional.gz";

    private ODatabaseDocumentTx model_db;
    private ODatabaseDocumentTx test_db;

    //Note: The following should match what's in the test database *exactly*
    //At this time the configs are set up so that the agent name will change on a subsequent invocation.
    //If I make changes to the test db, these need to be rechecked!
    private static final Set<String> agents = new HashSet<>(Arrays.asList("agent_smith","gc_agent","rc_agent","",null));
    private static final Set<String> regions = new HashSet<>(Arrays.asList("global_region","different_test_region","",null));
    private static final Set<String> pluginids = new HashSet<>(Arrays.asList("plugin/0","plugin/1","plugin/2","",null));
    private static final Set<String> jarfiles = new HashSet<>(Arrays.asList("repo-1.0-SNAPSHOT.jar","sysinfo-1.0-SNAPSHOT.jar","dashboard-1.0-SNAPSHOT.jar","",null));

    Map<String,Map<String,Map<String,String>>> region_contents = new HashMap<>();

    private static final int REPEAT_COUNT = 5;

    static List<Arguments> getAgentRegionPluginIdTriples(){
        List<Arguments> ret = new ArrayList<>();
        for(String region:regions){
            for(String agent:agents){
                for(String pluginid:pluginids){
                    ret.add(Arguments.of(region,agent,pluginid));
                }
            }
        }
        return ret;
    }

    static Stream<Arguments> getRegionAgentPairs(){
        return regions.stream().flatMap( (r)->
                        agents.stream().map( (a) ->
                                Arguments.of(r,a)
                        )
                );
    }

    Stream<String> getRegionParameterSet(){
        return regions.stream();
    }

    Stream<String> getAgents(){
        return agents.stream();
    }

    Stream<String> getPluginIds(){
        return pluginids.stream();
    }

    Stream<String> getJarfiles(){
        return jarfiles.stream();
    }

    static List<Arguments> getPluginTypeIdValuePairs(){
        /*Values pulled from test db using following query:
          select indexes.name
          from (select indexes from metadata:indexmanager unwind indexes)
          where indexes[name] like 'pNode%'
        */
        List<Arguments> ret  = new ArrayList<>();
        for(String pname : Arrays.asList(new String[]{"io.cresco.dashboard","io.cresco.sysinfo","io.cresco.repo","",null})){
            ret.add(Arguments.of("pluginname",pname));
        }

        for(Arguments argset : getAgentRegionPluginIdTriples()){
            ret.add(Arguments.of("nodePath",String.format("[\"%s\",\"%s\",\"%s\"]",argset.get())));

        }
        for(String jar : jarfiles){
            ret.add(Arguments.of("jarfile",jar));
        }
        ret.add(Arguments.of(null,null));
        return ret;
    }

    @org.junit.jupiter.api.BeforeAll
    void init_model_db() {
        Map<String,String> plugins_reg_agents = new HashMap<>();
        Map<String,Map<String,String>> agents_in_diff_region = new HashMap<>();
        plugins_reg_agents.put("plugin/0","io.cresco.sysinfo");
        agents_in_diff_region.put("agent_smith",plugins_reg_agents);
        agents_in_diff_region.put("rc_agent",plugins_reg_agents);
        region_contents.put("different_test_region",agents_in_diff_region);

        Map<String,String> plugins_gc = new HashMap<>();
        plugins_gc.put("plugin/0","io.cresco.repo");
        plugins_gc.put("plugin/1","io.cresco.dashboard");
        plugins_gc.put("plugin/2","io.cresco.sysinfo");
        Map<String,Map<String,String>> agents_in_global_region = new HashMap<>();
        agents_in_global_region.put("gc_agent",plugins_gc);
        region_contents.put("global_region",agents_in_global_region);

        try {
            model_db = OrientHelpers.getInMemoryTestDB(db_export_file_path).orElseThrow(() -> new Exception("Can't get test db"));
        }
        catch(Exception ex){
            System.out.println("Caught some exception while trying to set up DB for tests");
            ex.printStackTrace();
        }

    }
    @BeforeEach
    void set_up_controller() {
        Map<String,Object> pluginConf = CrescoHelpers.getMockPluginConfig(testingConf.getAsMap());
        test_db = model_db.copy();
        ce = CrescoHelpers.getControllerEngine("test_controller_agent","test_controller_region","DBInterfaceTest",
                pluginConf,test_db);
    }

   /* @AfterEach
    void tear_down_controller(){
        ce.setDBManagerActive(false);
        test_db.close();
    }*/

    @RepeatedTest(REPEAT_COUNT)
    void paramStringToMap() {
        String testParams="param1=value1,param2=value2";
        Map<String,String> resultMap = ce.getGDB().paramStringToMap(testParams);
        assertEquals("value1",resultMap.get("param1"));
        assertEquals("value2",resultMap.get("param2"));
    }


    /**
     * It seems the test DB I created does not have any records for this function to return. It throws an exception
     * because of a null value (see in DBInterface.java in getResourceTotal() the call edgeParams.get("cpu-logical-count")
     * I need to find out why there are no records. Based on the code, there should at least be the keys I check for in
     * the map returned.
     */
    @Test
    void getResourceTotal() {
        Map<String,String> resourceTotals = ce.getGDB().getResourceTotal();
        assertNotNull(resourceTotals.get("regions"));
        assertNotNull(resourceTotals.get("agents"));
        assertNotNull(resourceTotals.get("plugins"));
        assertNotNull(resourceTotals.get("cpu_core_count"));
        assertNotNull(resourceTotals.get("mem_available"));
        assertNotNull(resourceTotals.get("mem_total"));
        assertNotNull(resourceTotals.get("disk_available"));
        assertNotNull(resourceTotals.get("disk_total"));
    }

    @RepeatedTest(REPEAT_COUNT)
    void getRegionList() {
        String desired_output = "{\"regions\":[{\"name\":\"different_test_region\",\"agents\":\"2\"},{\"name\":\"globa"+
                "l_region\",\"agents\":\"1\"}]}";
        assertEquals(desired_output, ce.getGDB().getRegionList());
    }

    /**
     * This may not be needed in the updated version of the api.  We will need *some* mechanism to share databases but
     * that may happen elsewhere. The part of the test code that finds and preps the import file will have to be changed
     *
     */
    @Test
    void submitDBImport_test() {
        String expected_regions = "{\"regions\":[{\"name\":\"different_test_region\",\"agents\":\"2\"},{\"name\":\""+
                "global_region\",\"agents\":\"1\"},{\"name\":\"yet_another_test_region\",\"agents\":\"1\"}]}";
        String expected_agents = "{\"agents\":[{\"environment\":\"environment\",\"plugins\":\"1\",\"name\":\"another_r"+
                "c\",\"location\":\"location\",\"region\":\"yet_another_test_region\",\"platform\":\"platform\"}]}";
        String expected_plugins = "{\"plugins\":[{\"agent\":\"another_rc\",\"name\":\"plugin/0\",\"region\":\"yet_anot"+
                "her_test_region\"}]}";
        try {
            byte[] importData = Files.readAllBytes(Paths.get(regional_db_export_file_path));
            ce.getGDB().submitDBImport(DatatypeConverter.printBase64Binary(importData));
            Thread.sleep(5000);//Import runs in another thread so we need to give it time to work
            String newRegionList = ce.getGDB().getRegionList();
            assertEquals(expected_regions,newRegionList);
            String newAgentList = ce.getGDB().getAgentList("yet_another_test_region");
            assertEquals(expected_agents,newAgentList);
            String newPluginList = ce.getGDB().getPluginList("yet_another_test_region","another_rc");
            assertEquals(expected_plugins,newPluginList);
        }
        catch(FileNotFoundException ex) {
            fail(String.format("Could not find regional db export file at %s",regional_db_export_file_path),ex);
        }
        catch(IOException ex) {
            fail(String.format("Could not read regional db export file at %s",regional_db_export_file_path),ex);
        }
        catch(InterruptedException ex) {
            System.out.println("Threadus Interruptus");
        }
    }

    /**
     * Calling this with a null argument returns a different result from a blank argument or one not in the db. Is that
     * really the desired behavior? This test was written so it would pass if the null, blank, and nonexistent cases all
     * produce the same result.
     * @param region
     */
    @ParameterizedTest
    @MethodSource("getRegionParameterSet")
    void getAgentList_test(String region) {
        Map<String,String> expected_output = new HashMap<>();
        expected_output.put("global_region"
                ,"{\"agents\":[{\"environment\":\"environment\",\"plugins\":\"3\",\"name\":\"gc_agent\""+
                ",\"location\":\"location\",\"region\":\"global_region\",\"platform\":\"platform\"}]}");
        expected_output.put("different_test_region"
                ,"{\"agents\":[{\"environment\":\"environment\",\"plugins\":\"1\",\"name\":\"agent_smith\""+
                ",\"location\":\"location\",\"region\":\"different_test_region\",\""+
                "platform\":\"platform\"},{\"environment\":\"environment\",\"plugins\":\"1\",\"name\":\"rc_agent\",\"l"+
                "ocation\":\"location\",\"region\":\"different_test_region\",\"platform\":\"platform\"}]}");
        if(region == null || region.equals("")){
            assertEquals("{\"agents\":[]}",ce.getGDB().getAgentList(region));
        }
        assertEquals(expected_output.get(region), ce.getGDB().getAgentList(region));

    }

    /**
     * The function 'getPluginListRepoSet' only seems to use the database to figure out where the repo plugin lives (should be global
     * controller). Thus we may need a test that checks the lower-level function the aforementioned one depends on.
     */
    @RepeatedTest(REPEAT_COUNT)
    void getPluginListRepo_test() {
        assertEquals("{\"plugins\":[{\"jarfile\":\"fake.jar\",\"version\":\"NO_VERSION\",\"md5\":\"DefinitelyR"+
                "ealMD5\"}]}",ce.getGDB().getPluginListRepo());
    }

    /**
     * The function 'getPluginListRepoSet' only seems to use the database to figure out where the repo plugin lives (should be global
     * controller). Thus we may need a test that checks the lower-level function the aforementioned one depends on.
     */
    @RepeatedTest(REPEAT_COUNT)
    void getPluginListRepoSet_test() {
        Map<String,List<pNode>> plist = ce.getGDB().getPluginListRepoSet();
        for(pNode aplugin : plist.get("some_plugin_name")){
            assertTrue(
                    aplugin.isEqual("some_plugin_name","some_plugin.jar","65388b8d8bf462df2cd3910bcada4110","9.99.999")
            );
        }

    }

    /**
     * This test is similar to the getPluginListRepoSet_test() because it depends on the same lower-level db function.
     */
    @RepeatedTest(REPEAT_COUNT)
    void getPluginListRepoInventory_test() {
        String expected = "{\"server\":[],\"plugins\":[{\"pluginname\":\"some_plugin_name\",\"jarfile\":\"some_plugin."+
                "jar\",\"version\":\"9.99.999\",\"md5\":\"65388b8d8bf462df2cd3910bcada4110\"}]}";
        List<String> repoList = ce.getGDB().getPluginListRepoInventory();
        for(String res : repoList){
            assertEquals(expected,res);
        }
    }

    /**
     *I intentionally left the test for null args e.g. ("pluginname",null) failing. I think we should add
     * something in the new implementation to avoid the NulLPointerException.
     *We may not want this thing to work the same way after the rewrite. It seems odd that we should
     *get a different result just for null input. Also, this function will never return useful results for
     *this case as written because the query has a hardcoded part like this: '...WHERE key = '" + indexValue + "'".
     *The problem with that is that 'key' may be a collection instead of a single value when the index uses a composite
     *key. We could change the method or rename/annotate it to make it more clear what will work/not work.
     *If nothing else, it could be a good thing to mention in a docstring.
     * @param typeId
     * @param typeVal
     */
    @ParameterizedTest
    @MethodSource("getPluginTypeIdValuePairs")
    void getPluginListByType(String typeId,String typeVal) {
        String actual = ce.getGDB().getPluginListByType(typeId,typeVal);
        String sysinfo_expected = "{\"plugins\":[{\"agent\":\"gc_agent\",\"status_code\":\"10\",\"agentcon"+
                "troller\":\"plugin/2\",\"pluginname\":\"io.cresco.sysinfo\",\"status_dest\":\"Plugin "+
                "Active\",\"jarfile\":\"sysinfo-1.0-SNAPSHOT.jar\",\"pluginid\":\"plugin/2\",\"isactiv"+
                "e\":\"true\",\"region\":\"global_region\",\"configparams\":\"{\\\"pluginname\\\":\\\""+
                "io.cresco.sysinfo\\\",\\\"jarfile\\\":\\\"sysinfo-1.0-SNAPSHOT.jar\\\"}\"},{\"agent\""+
                ":\"agent_smith\",\"status_code\":\"10\",\"agentcontroller\":\"plugin/0\",\"pluginnam"+
                "e\":\"io.cresco.sysinfo\",\"status_dest\":\"Plugin Active\",\"jarfile\":\"sysinfo-1.0"+
                "-SNAPSHOT.jar\",\"pluginid\":\"plugin/0\",\"isactive\":\"true\",\"region\":\"differen"+
                "t_test_region\",\"configparams\":\"{\\\"pluginname\\\":\\\"io.cresco.sysinfo\\\",\\\""+
                "jarfile\\\":\\\"sysinfo-1.0-SNAPSHOT.jar\\\"}\"},{\"agent\":\"rc_agent\",\"status_cod"+
                "e\":\"10\",\"agentcontroller\":\"plugin/0\",\"pluginname\":\"io.cresco.sysinfo\",\"st"+
                "atus_dest\":\"Plugin Active\",\"jarfile\":\"sysinfo-1.0-SNAPSHOT.jar\",\"pluginid\":"+
                "\"plugin/0\",\"isactive\":\"true\",\"region\":\"different_test_region\",\"configparam"+
                "s\":\"{\\\"pluginname\\\":\\\"io.cresco.sysinfo\\\",\\\"jarfile\\\":\\\"sysinfo-1.0-S"+
                "NAPSHOT.jar\\\"}\"}]}";
        String dashboard_expected = "{\"plugins\":[{\"agent\":\"gc_agent\",\"status_code\":\"10\",\"agentcon"+
                "troller\":\"plugin/1\",\"pluginname\":\"io.cresco.dashboard\",\"status_dest\":\"Plugi"+
                "n Active\",\"jarfile\":\"dashboard-1.0-SNAPSHOT.jar\",\"pluginid\":\"plugin/1\",\"isa"+
                "ctive\":\"true\",\"region\":\"global_region\",\"configparams\":\"{\\\"pluginname\\\":"+
                "\\\"io.cresco.dashboard\\\",\\\"jarfile\\\":\\\"dashboard-1.0-SNAPSHOT.jar\\\"}\"}]}";

        String repo_expected  = "{\"plugins\":[{\"agent\":\"gc_agent\",\"status_code\":\"10\",\"agentcon"+
                "troller\":\"plugin/0\",\"pluginname\":\"io.cresco.repo\",\"status_dest\":\"Pl"+
                "ugin Active\",\"jarfile\":\"repo-1.0-SNAPSHOT.jar\",\"pluginid\":\"plugin/0\""+
                ",\"isactive\":\"true\",\"region\":\"global_region\",\"configparams\":\"{\\\"pluginnam"+
                "e\\\":\\\"io.cresco.repo\\\",\\\"jarfile\\\":\\\"repo-1.0-SNAPSHOT.jar\\\"}\"}]}";

        switch(typeId) {
            case "pluginname":
                switch(typeVal){
                    case "io.cresco.dashboard": assertEquals(dashboard_expected,actual);break;
                    case "io.cresco.sysinfo": assertEquals(sysinfo_expected,actual);break;
                    case "io.cresco.repo": assertEquals(repo_expected,actual);
                }
            break;

            case "nodePath":
                assertEquals("{\"plugins\":[]}",actual);
            break;

            case "jarfile":
                switch(typeVal){
                    case "repo-1.0-SNAPSHOT.jar": assertEquals(repo_expected,actual); break;
                    case "sysinfo-1.0-SNAPSHOT.jar":assertEquals(sysinfo_expected,actual); break;
                    case "dashboard-1.0-SNAPSHOT.jar":assertEquals(dashboard_expected,actual); break;
                }
            break;
            default: assertEquals("{\"plugins\":[]}",actual);
        }
    }

    /**
     * Yep, some of these tests were written to fail for the previous implementation. Like many other functions in here,
     *  behavior changes when null args are passed vs blank strings. For two inputs where each can be one of: 1)in the
     *  database, 2)not in the database, or 3)null. That gives us nine total combinations. For cases out of those nine
     *  where we don't expect output, I suspect the "no output" response should be the same. Like several other functions,
     *  it can either be {"plugins":[]} or the test throws a NullPointerException. NPEs are undesirable because they
     *  don't tell you much. If we want to throw an exception in those cases, we should throw a more descriptive exception.
     * @param region
     * @param agent
     * @throws ParseException
     */
    @ParameterizedTest
    @MethodSource("getRegionAgentPairs")
    void getPluginList(String region, String agent) throws ParseException {
        String actual = ce.getGDB().getPluginList(region,agent);
        String allplugins = "{\"plugins\":[{\"agent\":\"agent_smith\",\"name\":\"plugin/0\",\"region\":\"different_test_region\"},{\"agent\":\"rc_agent\",\"name\":\"plugin/0\",\"region\":\"different_test_region\"},{\"agent\":\"gc_agent\",\"name\":\"plugin/0\",\"region\":\"global_region\"},{\"agent\":\"gc_agent\",\"name\":\"plugin/1\",\"region\":\"global_region\"},{\"agent\":\"gc_agent\",\"name\":\"plugin/2\",\"region\":\"global_region\"}]}";
        String global_region_plugins = "{\"plugins\":[{\"agent\":\"gc_agent\",\"name\":\"plugin/0\",\"region\":\"global_region\"},{\"agent\":\"gc_agent\",\"name\":\"plugin/1\",\"region\":\"global_region\"},{\"agent\":\"gc_agent\",\"name\":\"plugin/2\",\"region\":\"global_region\"}]}";
        String diff_region_plugins = "{\"plugins\":[{\"agent\":\"agent_smith\",\"name\":\"plugin/0\",\"region\":\"different_test_region\"},{\"agent\":\"rc_agent\",\"name\":\"plugin/0\",\"region\":\"different_test_region\"}]}";

        if(region == null && agent == null) assertEquals(allplugins,actual);
        else if(region.equals("global_region") && agent == null) assertEquals(global_region_plugins,actual);
        else if(region.equals("different_test_region") && agent == null) assertEquals(diff_region_plugins,actual);
        else if(region_contents.containsKey(region) && region_contents.get(region).containsKey(agent)){
            for (String pluginid : region_contents.get(region).get(agent).keySet()) {
                assertEquals(String.format("{\"plugins\":[{\"agent\":\"%s\",\"name\":\"%s\",\"region\":\"%s\"}]}"
                        ,agent,pluginid,region)
                        ,actual);
            }
        } else assertEquals("{\"plugins\":[]}",actual);
    }

    /**
     * This method does not seem to return aggregated results when some parameters are left null. In other words,
     * it doesn't look like passing a null argument does anything here. This differs from the examples I've seen so far.
     * This time I decided to force the tests to fail explicitly. I wrote them to call fail() to remind myself that we
     * need to pick a standard behavior for all of these.
     * @param region
     * @param agent
     * @param pluginid
     */
    @ParameterizedTest
    @MethodSource("getAgentRegionPluginIdTriples")
    void getPluginInfo(String region, String agent, String pluginid) {
        String actual = ce.getGDB().getPluginInfo(region,agent,pluginid);
        if(region_contents.containsKey(region) && region_contents.get(region).containsKey(agent)){
            if(region.equals("global_region")){
                //Test db only has single agent in region "global_region". I already check the agent above so I don't
                //need to check it again. If we get here, the "agent" parameter could only have been a valid one.
                switch(pluginid){
                    case "plugin/0":assertEquals("{\"pluginname\":\"io.cresco.repo\",\"jarfile\":\"repo-1.0-SNAPSHOT.jar\"}",actual);
                    break;
                    case "plugin/1":assertEquals("{\"pluginname\":\"io.cresco.dashboard\",\"jarfile\":\"dashboard-1.0-SNAPSHOT.jar\"}",actual);
                    break;
                    case "plugin/2":assertEquals("{\"pluginname\":\"io.cresco.sysinfo\",\"jarfile\":\"sysinfo-1.0-SNAPSHOT.jar\"}",actual);
                    break;
                    default:fail("Returns "+actual);
                }
            }
            if(region.equals("different_test_region")){
                //Two agents in this region but each only has one plugin,io.cresco.sysinfo.
                if(pluginid.equals("plugin/0")) assertEquals("{\"pluginname\":\"io.cresco.sysinfo\",\"jarfile\":\"sysinfo-1.0-SNAPSHOT.jar\"}"
                        ,actual);
                else fail("Returns "+actual);
            }
        }
        else fail("Returns "+actual);

    }

    /*
    @org.junit.jupiter.api.Test
    void getIsAttachedMetrics() {
    }

    @org.junit.jupiter.api.Test
    void getNetResourceInfo() {
    }

    @org.junit.jupiter.api.Test
    void getResourceInfo() {
    }

    @org.junit.jupiter.api.Test
    void getGPipeline() {
    }

    @org.junit.jupiter.api.Test
    void getGPipelineExport() {
    }

    @org.junit.jupiter.api.Test
    void getIsAssignedInfo() {
    }

    @org.junit.jupiter.api.Test
    void getPipelineInfo() {
    }

    @org.junit.jupiter.api.Test
    void getResourceTotal2() {
    }

    @org.junit.jupiter.api.Test
    void getEdgeHealthStatus() {
    }

    @org.junit.jupiter.api.Test
    void getNodeStatus() {
    }

    @org.junit.jupiter.api.Test
    void addNode() {
    }

    @org.junit.jupiter.api.Test
    void watchDogUpdate() {
    }

    @org.junit.jupiter.api.Test
    void removeNode() {
    }

    @org.junit.jupiter.api.Test
    void removeNode1() {
    }
    */
}
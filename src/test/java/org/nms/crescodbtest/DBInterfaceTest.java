package org.nms.crescodbtest;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.sun.org.apache.xpath.internal.Arg;
import io.cresco.agent.controller.globalscheduler.pNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    private ODatabaseDocumentTx model_db;
    private ODatabaseDocumentTx test_db;

    //Note: The following should match what's in the test database *exactly*
    //At this time the configs are set up so that the agent name will change on a subsequent invocation.
    //If I make changes to the test db, these need to be rechecked!
    private static final Set<String> agents = new HashSet<>(Arrays.asList("agent_smith","gc_agent","rc_agent"));
    private static final Set<String> regions = new HashSet<>(Arrays.asList("global_region","different_test_region"));
    private static final Set<String> pluginids = new HashSet<>(Arrays.asList("plugin/0","plugin/1","plugin/2"));
    private static final Set<String> jarfiles = new HashSet<>(Arrays.asList("repo-1.0-SNAPSHOT.jar","sysinfo-1.0-SNAPSHOT.jar","dashboard-1.0-SNAPSHOT.jar"));
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

    @org.junit.jupiter.api.BeforeAll
    void init_model_db() {
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
/*
    @org.junit.jupiter.api.Test
    void getResourceTotal() {

    }
*/
    @RepeatedTest(REPEAT_COUNT)
    void getRegionList() {
        String desired_output = "{\"regions\":[{\"name\":\"different_test_region\",\"agents\":\"2\"},{\"name\":\"globa"+
                "l_region\",\"agents\":\"1\"}]}";
        assertEquals(desired_output, ce.getGDB().getRegionList());
    }
/*
    @org.junit.jupiter.api.Test
    void submitDBImport() {
    }
*/
    Set<String> getRegionParameterSet(){
        //Add a region value that is almost certainly not in the database
        Set<String> ret = new HashSet<>(Arrays.asList((Long.toString(System.currentTimeMillis()))));
        ret.addAll(regions);
        return ret;
    }
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
        if(regions.contains(region)) {
            assertEquals(expected_output.get(region), ce.getGDB().getAgentList(region));
        } else {
            assertEquals("{\"agents\":[]}",ce.getGDB().getAgentList(region));
        }
    }


    @RepeatedTest(REPEAT_COUNT)
    void getPluginListRepo_test() {
        assertEquals("{\"plugins\":[{\"jarfile\":\"fake.jar\",\"version\":\"NO_VERSION\",\"md5\":\"DefinitelyR"+
                "ealMD5\"}]}",ce.getGDB().getPluginListRepo());
    }


    @RepeatedTest(REPEAT_COUNT)
    void getPluginListRepoSet_test() {
        /*Note: The function 'getPluginListRepoSet' only seems to use the database to figure out where the repo plugin lives (should be global
        * controller). Thus we may need a test that checks the lower-level function the aforementioned one depends on*/
        Map<String,List<pNode>> plist = ce.getGDB().getPluginListRepoSet();
        for(pNode aplugin : plist.get("some_plugin_name")){
            assertTrue(
                    aplugin.isEqual("some_plugin_name","some_plugin.jar","65388b8d8bf462df2cd3910bcada4110","9.99.999")
            );
        }

    }

    @RepeatedTest(REPEAT_COUNT)
    void getPluginListRepoInventory_test() {
        /*This test is similar to the getPluginListRepoSet_test() because it depends on the same lower-level db function.
         */
        String expected = "{\"server\":[],\"plugins\":[{\"pluginname\":\"some_plugin_name\",\"jarfile\":\"some_plugin."+
                "jar\",\"version\":\"9.99.999\",\"md5\":\"65388b8d8bf462df2cd3910bcada4110\"}]}";
        List<String> repoList = ce.getGDB().getPluginListRepoInventory();
        for(String res : repoList){
            assertEquals(expected,res);
        }
    }

    /**
     *First, please note that I intentionally left the test for args ("pluginname",null) failing. I think we should add
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
    @MethodSource("pluginTypeIdValuePairs")
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
        }
    }

    static List<Arguments> pluginTypeIdValuePairs(){
        /*Values pulled from test db using following query:
          select indexes.name
          from (select indexes from metadata:indexmanager unwind indexes)
          where indexes[name] like 'pNode%'
        */
        List<Arguments> ret  = new ArrayList<>();

        //indexInfoMap.put("nodePath",Arrays.asList(new String[]{""}));
        //indexInfoMap.put("jarfile",Arrays.asList(new String[]{""}));
        ret.add(Arguments.of("pluginname",null));
        for(String pname : Arrays.asList(new String[]{"io.cresco.dashboard","io.cresco.sysinfo","io.cresco.repo"})){
            ret.add(Arguments.of("pluginname",pname));
        }
        ret.add(Arguments.of("nodePath",null));
        for(Arguments argset : getAgentRegionPluginIdTriples()){
            ret.add(Arguments.of("nodePath",String.format("[\"%s\",\"%s\",\"%s\"]",argset.get())));
            /*Stream.of(argset.get())
                    .map((args)-> ))
                    .collect(Collectors.to*/
        }
        ret.add(Arguments.of("jarfile",null));
        for(String jar : jarfiles){
            ret.add(Arguments.of("jarfile",jar));
        }
        //new String[]{"pluginname","nodePath","jarfile"})
        return ret;
    }
/*
    @org.junit.jupiter.api.Test
    void getPluginList() {
    }

    @org.junit.jupiter.api.Test
    void getPluginInfo() {
    }

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
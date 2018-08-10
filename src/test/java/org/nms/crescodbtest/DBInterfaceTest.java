package org.nms.crescodbtest;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
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
    private final Set<String> agents = new HashSet<>(Arrays.asList("agent_smith","gc_agent","rc_agent"));
    private final Set<String> regions = new HashSet<>(Arrays.asList("global_region","different_test_region"));
    private final Set<String> pluginids = new HashSet<>(Arrays.asList("plugin/0","plugin/1","plugin/2"));
    private final Set<String> jarfiles = new HashSet<>(Arrays.asList("repo-1.0-SNAPSHOT.jar","sysinfo-1.0-SNAPSHOT.jar","dashboard-1.0-SNAPSHOT.jar"));


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

    @org.junit.jupiter.api.Test
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
    @org.junit.jupiter.api.Test
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


    @org.junit.jupiter.api.Test
    @RepeatedTest(10)
    void getPluginListRepo_test() {
        assertEquals("{\"plugins\":[{\"jarfile\":\"fake.jar\",\"version\":\"NO_VERSION\",\"md5\":\"DefinitelyR"+
                "ealMD5\"}]}",ce.getGDB().getPluginListRepo());
    }

    //NMS This test always fails because the results change each execution. In its current form it isn't amenable to testing like this
    //I need to study how this function is used. It appears to be broken
    @org.junit.jupiter.api.Test
    @RepeatedTest(10)
    void getPluginListRepoSet_test() {
        assertEquals("{null=[io.cresco.agent.controller.globalscheduler.pNode@a8c1f44]}",ce.getGDB().getPluginListRepoSet());
    }
/*
    @org.junit.jupiter.api.Test
    void getPluginListRepoInventory() {
    }

    @org.junit.jupiter.api.Test
    void getPluginListByType() {
    }

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
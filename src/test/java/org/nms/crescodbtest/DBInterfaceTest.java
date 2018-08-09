package org.nms.crescodbtest;


import java.util.Map;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
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
    private final GDBConf adminConf = new GDBConf("root","root","localhost","cresco_test");

    private final String local_db_path = "/home/nima/bin/orientdb-2.2.36/databases";
    //private final String db_backup_path = "/home/nima/bin/orientdb-2.2.36/backup_for_testing.zip";
    private final String db_backup_path = "/home/nima/code/IdeaProjects/cresco_db_rewrite/src/test/resources/global_region_agent.gz";

    private ODatabaseDocumentTx model_db;



    @org.junit.jupiter.api.BeforeAll
    void init_model_db() {
        try {
            //OrientHelpers.importFileToOrientDB(gdbRootUserConf, "src/test/resources/global_region_agent.gz");
            model_db = OrientHelpers.getInMemoryTestDB(db_backup_path).orElseThrow(() -> new Exception("Can't get test db"));
            //OrientHelpers.addDbUser(gdbRootUserConf,testing_user,testing_pw,new String[]{"reader"});
        }
        catch(Exception ex){
            System.out.println("Caught some exception while trying to set up DB for tests");
            ex.printStackTrace();
        }
        //ce = CrescoHelpers.getControllerEngine("test_agent","test_region","DBInterfaceTest"
        //        ,pluginConf,null);

    }
    @BeforeEach
    void set_up_controller() {
        Map<String,Object> pluginConf = CrescoHelpers.getMockPluginConfig(testingConf.getAsMap());
        ODatabaseDocumentTx test_db = model_db.copy();
        ce = CrescoHelpers.getControllerEngine("test_controller_agent","test_controller_region","DBInterfaceTest",
                pluginConf,test_db);
    }

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
    @org.junit.jupiter.api.Test
    void getAgentList_global_region() {
        String desired_output = "{\"agents\":[{\"environment\":\"environment\",\"plugins\":\"3\",\"name\":\"gc_agent\""+
                ",\"location\":\"location\",\"region\":\"global_region\",\"platform\":\"platform\"}]}";
        assertEquals(desired_output, ce.getGDB().getAgentList("global_region"));
    }

    @org.junit.jupiter.api.Test
    void getAgentList_different_test_region() {
        String desired_output = "{\"agents\":[{\"environment\":\"environment\",\"plugins\":\"1\",\"name\":\"agent-e96a"+
                "9e34-5dd4-4865-925f-641e008aad24\",\"location\":\"location\",\"region\":\"different_test_region\",\""+
                "platform\":\"platform\"},{\"environment\":\"environment\",\"plugins\":\"1\",\"name\":\"rc_agent\",\"l"+
                "ocation\":\"location\",\"region\":\"different_test_region\",\"platform\":\"platform\"}]}";
        assertEquals(desired_output, ce.getGDB().getAgentList("different_test_region"));
    }

/*
    @org.junit.jupiter.api.Test
    void getPluginListRepo() {
    }

    @org.junit.jupiter.api.Test
    void getPluginListRepoSet() {
    }

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
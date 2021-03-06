package io.pivotal.bds.gemfire.groovy.test;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;

import io.pivotal.bds.gemfire.groovy.data.ScriptExecutionContext;

public class ExecuteScript {

    private static final Logger LOG = LoggerFactory.getLogger(ExecuteScript.class);

    public static void main(String[] args) throws Exception {
        LOG.info("creating ClientCacheFactory");
        ClientCacheFactory ccf = new ClientCacheFactory();

        ccf.addPoolLocator("localhost", 10334);
        ccf.setPdxSerializer(
                new ReflectionBasedAutoSerializer("io.pivotal.bds.gemfire.groovy.data.*,io.pivotal.bds.gemfire.data.ecom.*"));

        LOG.info("creating ClientCache");
        ClientCache cc = ccf.create();

        try {
            LOG.info("creating script Region");
            ClientRegionFactory<String, String> crf = cc.createClientRegionFactory(ClientRegionShortcut.PROXY);
            Region<String, String> r = crf.create("script");

            ScriptExecutionContext sec = new ScriptExecutionContext("test.groovy", "whatever");

            Set<String> filter = new HashSet<>();
            filter.add("abc");

            LOG.info("executing function");
            // FunctionService.onRegion(r).withArgs(sec).withFilter(filter).execute("ScriptExecutionFunction").getResult();

            for (int i = 0; i < 100; ++i) {
                FunctionService.onRegion(r).withArgs(sec).withFilter(filter).execute("ScriptExecutionFunction").getResult();
            }
        } finally {
            LOG.info("closing ClientCache");
            cc.close();
        }
    }
}

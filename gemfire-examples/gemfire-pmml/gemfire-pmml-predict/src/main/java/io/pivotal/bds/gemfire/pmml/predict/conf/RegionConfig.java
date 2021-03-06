package io.pivotal.bds.gemfire.pmml.predict.conf;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.pivotal.bds.gemfire.pmml.common.data.EvaluatorParams;
import io.pivotal.bds.gemfire.pmml.common.keys.EvalKey;

@Configuration
public class RegionConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RegionConfig.class);
    
    @Bean
    public Region<EvalKey, EvaluatorParams> paramsRegion(ClientCache cache) {
        LOG.info("paramsRegion");
        ClientRegionFactory<EvalKey, EvaluatorParams> crf = cache.createClientRegionFactory(ClientRegionShortcut.PROXY);
        return crf.create("params");
    }
}

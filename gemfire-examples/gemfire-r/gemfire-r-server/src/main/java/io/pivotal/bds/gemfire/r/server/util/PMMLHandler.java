package io.pivotal.bds.gemfire.r.server.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;

import com.gemstone.gemfire.cache.Operation;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.pdx.PdxInstance;

import io.pivotal.bds.gemfire.r.common.PMMLKey;
import io.pivotal.bds.gemfire.r.common.PMMLPredictDef;
import io.pivotal.bds.gemfire.r.common.PMMLPredictDefKey;
import io.pivotal.bds.gemfire.util.Assert;
import io.pivotal.bds.gemfire.util.RegionHelper;

public class PMMLHandler extends Handler {

    private PMMLPredictDef def;

    public PMMLHandler(PMMLPredictDefKey key, PMMLPredictDef def) {
        super(key, def.getRegionName());
        this.def = def;
    }

    @Override
    public void doHandle(String regionName, Operation op, Object key, Object value) {
        if (!op.isCreate() && !op.isUpdate()) {
            return;
        }

        Region<PMMLKey, PMML> pmmlRegion = RegionHelper.getRegion("pmml");
        PMMLKey pmmlKey = def.getPmmlKey();
        PMML pmml = pmmlRegion.get(pmmlKey);
        Assert.notNull(pmml, "PMML model %s not found", pmmlKey.getId());

        ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
        ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelManager(pmml);
        
        List<FieldName> activeFields = modelEvaluator.getActiveFields();
        List<FieldName> targetFields = modelEvaluator.getTargetFields();
        List<FieldName> outputFields = modelEvaluator.getOutputFields();

        PdxInstance inst = (PdxInstance) value;

        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        for(FieldName activeField : activeFields){
            String fn = activeField.getValue();
            Object rawValue = inst.getField(fn);
            FieldValue activeValue = modelEvaluator.prepare(activeField, rawValue);
            arguments.put(activeField, activeValue);
        }
        
        Map<FieldName, ?> results = modelEvaluator.evaluate(arguments);
        
        for (Map.Entry<FieldName, ?> entry: results.entrySet()) {
            FieldName fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
        }
    }

}
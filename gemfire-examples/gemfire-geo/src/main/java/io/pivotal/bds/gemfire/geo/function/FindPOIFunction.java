package io.pivotal.bds.gemfire.geo.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionException;
import org.apache.geode.cache.execute.RegionFunctionContext;
import org.apache.geode.cache.execute.ResultSender;
import com.vividsolutions.jts.geom.Geometry;

import io.pivotal.bds.gemfire.geo.data.Boundary;
import io.pivotal.bds.gemfire.geo.data.BoundaryKey;
import io.pivotal.bds.gemfire.geo.data.FindPOIRequest;
import io.pivotal.bds.gemfire.geo.data.FindPOIResponse;
import io.pivotal.bds.gemfire.geo.data.PointOfInterest;
import io.pivotal.bds.gemfire.geo.data.PointOfInterestKey;
import io.pivotal.bds.gemfire.geo.util.GeoUtil;
import io.pivotal.bds.gemfire.util.RegionHelper;

public class FindPOIFunction extends BaseLockingFunction {

    private static final Logger LOG = LoggerFactory.getLogger(FindPOIFunction.class);
    private static final long serialVersionUID = 1L;

    @Override
    public void execute(FunctionContext ctx) {
        try {
            Region<BoundaryKey, Collection<PointOfInterestKey>> boundaryPOIXrefRegion = RegionHelper
                    .getRegion(BOUNDARY_POI_XREF_REGION_NAME);
            Region<PointOfInterestKey, PointOfInterest> poiRegion = RegionHelper.getRegion(POI_REGION_NAME);
            Region<BoundaryKey, Boundary> rootBoundaryRegion = RegionHelper.getRegion(ROOT_BOUNDARY_REGION_NAME);

            ResultSender<FindPOIResponse> sender = ctx.getResultSender();
            RegionFunctionContext rctx = (RegionFunctionContext) ctx;

            Set<?> filter = rctx.getFilter();

            Map<FindPOIRequest, List<PointOfInterestKey>> map = new HashMap<>();

            for (Object fo : filter) {
                FindPOIRequest req = (FindPOIRequest) fo;
                LOG.debug("execute: req={}", req);

                BoundaryKey reqBK = req.getBoundaryKey();
                Boundary reqB = rootBoundaryRegion.get(reqBK);
                Geometry geo = req.getGeometry();

                readLock.lock();

                try {
                    long start = System.currentTimeMillis();
                    List<Boundary> bounds = GeoUtil.intersects(reqB, geo);
                    long t1 = System.currentTimeMillis();

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("execute: bounds.size={}, req={}", bounds.size(), req);
                    }

                    for (Boundary bound : bounds) {
                        LOG.debug("execute: req={}, bound={}", req, bound);

                        Geometry boundGeo = bound.getGeometry();
                        BoundaryKey bk = bound.getKey();

                        Collection<PointOfInterestKey> poiks = boundaryPOIXrefRegion.get(bk);

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("execute: req={}, bound={}, poiks.size={}, poiks={}", req, bound, poiks.size(), poiks);
                        }

                        for (PointOfInterestKey poik : poiks) {
                            PointOfInterest poi = poiRegion.get(poik);
                            Geometry poiGeo = poi.getLocation();

                            if (boundGeo.intersects(poiGeo)) {
                                List<PointOfInterestKey> lpoik = map.get(req);

                                if (lpoik == null) {
                                    lpoik = new ArrayList<>();
                                    map.put(req, lpoik);
                                }

                                lpoik.add(poik);
                            }
                        }
                    }

                    long t2 = System.currentTimeMillis();
                    long d1 = t1 - start;
                    long d2 = t2 - t1;
                    LOG.info("execute: d1={}, d2={}", d1, d2);
                } finally {
                    readLock.unlock();
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("execute: map.size={}, map={}", map.size(), map);
            }

            FindPOIResponse resp = new FindPOIResponse(map);
            sender.lastResult(resp);
        } catch (Exception x) {
            throw new FunctionException(x.toString(), x);
        }
    }

    @Override
    public String getId() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public boolean isHA() {
        return true;
    }

    @Override
    public boolean optimizeForWrite() {
        return true;
    }

    @Override
    public void init(Properties props) {
    }

}

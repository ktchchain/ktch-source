package com.photon.photonchain.network.ehcacheManager;

import net.sf.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author:Lin
 * @Description:资产统计
 * @Date:17:09 2019/1/7
 * @Modified by:
 */
@Component
public class StatisticalAssetsManager {
    final static Logger logger = LoggerFactory.getLogger ( StatisticalAssetsManager.class );

    private static final String ASSETS_MAP = "ASSETS_MAP";

    private Cache assetsCache = EhCacheManager.getCache ( "assetsCache" );

    public void setStatisticalAssets(Map<String, Map<String, Long>> statisticalAssets) {
        EhCacheManager.put ( assetsCache, ASSETS_MAP, statisticalAssets );
    }

    public Map<String, Map<String, Long>> getStatisticalAssets() {
        return EhCacheManager.getCacheValue ( assetsCache, ASSETS_MAP, Map.class );
    }
}

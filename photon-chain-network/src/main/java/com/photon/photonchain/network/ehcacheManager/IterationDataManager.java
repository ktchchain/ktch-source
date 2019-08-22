package com.photon.photonchain.network.ehcacheManager;

import com.photon.photonchain.network.utils.FileUtil;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import net.sf.ehcache.Cache;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author:Lin
 * @Description:
 * @Date:10:45 2018/6/19
 * @Modified by:
 */
@Component
public class IterationDataManager {
    private Cache iterationDataCache = EhCacheManager.getCache("iterationDataCache");

    private static final String UNCONFIRMED_TRAN_MAP = "UNCONFIRMED_TRAN_MAP";

    public ConcurrentHashMap<String, UnconfirmedTran> getUnconfirmedTranMap() {
        return EhCacheManager.getCacheValue(iterationDataCache, UNCONFIRMED_TRAN_MAP, ConcurrentHashMap.class);
    }
    public ConcurrentHashMap getCloneUnconfirmedTranMap() {
        return FileUtil.clone(EhCacheManager.getCacheValue(iterationDataCache, UNCONFIRMED_TRAN_MAP, ConcurrentHashMap.class));
    }

    public void setUnconfirmedTranMap() {
        EhCacheManager.put(iterationDataCache, UNCONFIRMED_TRAN_MAP, new ConcurrentHashMap<String, UnconfirmedTran>());
    }
}

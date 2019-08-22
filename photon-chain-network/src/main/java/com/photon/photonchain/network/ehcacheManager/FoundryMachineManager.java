package com.photon.photonchain.network.ehcacheManager;

import com.photon.photonchain.storage.constants.Constants;
import net.sf.ehcache.Cache;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author:Lin
 * @Description:
 * @Date:14:42 2018/1/12
 * @Modified by:
 */
@Component
public class FoundryMachineManager {
    private Cache foundryMachineCache = EhCacheManager.getCache ( "foundryMachineCache" );
    private static final String WAIT = "WAIT";
    private static final String WAIT_COUNT = "WAIT_COUNT";
    private static final String RESETPARTICIPANT_TIME = "RESETPARTICIPANT_TIME";

    public void setFoundryMachine(String pubKey, boolean isStart) {
        EhCacheManager.put ( foundryMachineCache, pubKey, isStart );
    }

    public boolean foundryMachineIsStart(String pubKey) {
        boolean res = false;
        try {
            res = EhCacheManager.getCacheValue ( foundryMachineCache, pubKey, boolean.class );
        } catch (Exception e) {
        }
        return res;
    }

    public void setWaitfoundryMachine(String pubKey) {
        EhCacheManager.put ( foundryMachineCache, WAIT, pubKey );
    }

    public String getWaitfoundryMachine() {
        try {
            return EhCacheManager.getCacheValue ( foundryMachineCache, WAIT, String.class );
        } catch (Exception e) {
            return null;
        }
    }

    public void setWaitFoundryMachineCount(int count) {
        EhCacheManager.put ( foundryMachineCache, WAIT_COUNT, count );
    }

    public int getWaitFoundryMachineCount() {
        return EhCacheManager.getCacheValue ( foundryMachineCache, WAIT_COUNT, int.class );
    }

    public int getFoundryMachineCount(Map<String, Long> assets) {
        int count = (int) (assets.get ( Constants.BALANCE ) / 10000000000L);
        if ( count < 1 ) return 1;
        if ( count > 1000 ) return 1000;
        return count;
    }

    public void removeFoundryMachine(String pubKey) {
        EhCacheManager.remove ( foundryMachineCache, pubKey );
    }

    public void removeAllFoundryMachine() {
        foundryMachineCache.removeAll ( );
    }

    public void setResetParticipantTime(long time) {
        EhCacheManager.put ( foundryMachineCache, RESETPARTICIPANT_TIME, time );
    }

    public long getResetParticipantTime() {
        try {
            return EhCacheManager.getCacheValue ( foundryMachineCache, RESETPARTICIPANT_TIME, long.class );
        } catch (Exception e) {
            return 0;
        }
    }
}

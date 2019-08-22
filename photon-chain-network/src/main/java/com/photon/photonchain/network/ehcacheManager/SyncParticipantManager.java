package com.photon.photonchain.network.ehcacheManager;

import com.photon.photonchain.network.proto.BlockMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.network.proto.ParticipantMessage;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.entity.Block;
import net.sf.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @Author:Lin
 * @Description:
 * @Date:17:40 2018/1/17
 * @Modified by:
 */
@Component
public class SyncParticipantManager {
    final static Logger logger = LoggerFactory.getLogger(SyncParticipantManager.class);

    private Cache participantCache = EhCacheManager.getCache("participantCache");

    private Cache participantSyncCache = EhCacheManager.getCache("participantSyncCache");

    private static final String SYNC_PARTICIPANT = "SYNC_PARTICIPANT";

    private static final String SYNC_PARTICIPANT_QUEUE = "SYNC_PARTICIPANT_QUEUE";

    private static final String SYNC_COUNT = "SYNC_COUNT";

    //添加抓铸造机到队列
    public Map getParticipantQueue() {
        Queue<Map> participantQueue = EhCacheManager.getCacheValue(participantSyncCache, SYNC_PARTICIPANT_QUEUE, Queue.class);
        Map<String, Object> queueMap = participantQueue.poll();
        EhCacheManager.put(participantSyncCache, Constants.SYNC_PARTICIPANT_LIST, participantQueue);
        return queueMap;
    }

    public synchronized Queue<Map> addParticipantQueueQueue(List<ParticipantMessage.Participant> participants, Long blockHeight) {
        Queue<Map> syncParticipantQueue = EhCacheManager.getCacheValue(participantSyncCache, SYNC_PARTICIPANT_QUEUE, Queue.class);
        Map<String, Object> queueMap = new HashMap();
        queueMap.put(Constants.SYNC_PARTICIPANT_LIST, participants);
        queueMap.put(Constants.SYNC_BLOCK_HEIGHT, blockHeight);
        syncParticipantQueue.offer(queueMap);
        EhCacheManager.put(participantSyncCache, SYNC_PARTICIPANT_QUEUE, syncParticipantQueue);
        return syncParticipantQueue;
    }

    public void setParticipantQueue() {
        Queue<Map> syncParticipantQueue = new LinkedList<Map>();
        EhCacheManager.put(participantSyncCache, SYNC_PARTICIPANT_QUEUE, syncParticipantQueue);
    }

    public boolean isSyncParticipant() {
        return EhCacheManager.getCacheValue(participantSyncCache, SYNC_PARTICIPANT, boolean.class);
    }

    public void setSyncParticipant(boolean syncParticipant) {
        logger.info("【syncParticipant：" + syncParticipant + "】");
        EhCacheManager.put(participantSyncCache, SYNC_PARTICIPANT, syncParticipant);
    }

    public void setSyncCount(int syncCount) {
        EhCacheManager.put(participantSyncCache, SYNC_COUNT, syncCount);
    }

    public int getSyncCount() {
        return EhCacheManager.getCacheValue(participantSyncCache, SYNC_COUNT, int.class);
    }

    //获取节点同步数
    public Queue<Map> getSyncQueue() {
        return EhCacheManager.getCacheValue(participantSyncCache, SYNC_PARTICIPANT_QUEUE, Queue.class);
    }

    public void setParticipant(String pubKey, int count) {
        EhCacheManager.put(participantCache, pubKey, count);
    }

    public void addParticipant(String pubKey, int count) {
        if (!EhCacheManager.existKey(participantCache, pubKey)) {
            EhCacheManager.put(participantCache, pubKey, count);
        }
    }

    public void delParticipant(String pubKey) {
        EhCacheManager.remove(participantCache, pubKey);
    }

    public void delAllParticipant() {
        participantCache.removeAll();
    }

    public int getParticipantCount(String pubKey) {
        return EhCacheManager.getCacheValue(participantCache, pubKey, int.class);
    }

    public Map<String, Integer> getParticipantList() {
        Map<String, Integer> participantMap = new HashMap<>();
        participantCache.getKeys().forEach(key -> {
            participantMap.put((String) key, (Integer) participantCache.get(key).getObjectValue());
        });
        return participantMap;
    }
}
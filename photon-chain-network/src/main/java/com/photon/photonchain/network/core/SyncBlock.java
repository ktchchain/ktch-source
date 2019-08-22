package com.photon.photonchain.network.core;

import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.storage.repository.UnconfirmedTranRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.SYNC_BLOCK;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.REQUEST;

/**
 * @Author:Lin
 * @Description:
 * @Date:15:48 2018/1/17
 * @Modified by:
 */
@Component
public class SyncBlock {
    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;
    @Autowired
    private SyncBlockManager syncBlockManager;
    @Autowired
    private InitializationManager initializationManager;
    @Autowired
    private SyncUnconfirmedTranManager syncUnconfirmedTranManager;
    @Autowired
    private SyncTokenManager syncTokenManager;
    @Autowired
    private UnconfirmedTranRepository unconfirmedTranRepository;
    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;
    @Autowired
    private IterationDataManager iterationDataManager;
    @Autowired
    private SyncParticipantManager syncParticipantManager;
    @Autowired
    private SyncTimer syncTimer;

    public void init() {
        if ( nioSocketChannelManager.getActiveNioSocketChannelCount ( ) < BigInteger.ONE.longValue ( ) ) {
            unconfirmedTranManager.setUnconfirmedtranMap ( );
            unconfirmedTranManager.setNoDelUnconfirmedtranMap ( );
            iterationDataManager.setUnconfirmedTranMap ( );
            syncBlockManager.setHasNewBlock ( false );
            syncUnconfirmedTranManager.setHasNewTransaction ( false );
            syncTokenManager.setHasNewToken ( false );
            syncBlockManager.setSyncBlock ( false );
            syncUnconfirmedTranManager.setSyncTransaction ( false );
            syncTokenManager.setSyncToken ( false );
            syncParticipantManager.setSyncParticipant ( false );
            return;
        }
        iterationDataManager.setUnconfirmedTranMap ( );
        unconfirmedTranManager.setNoDelUnconfirmedtranMap ( );
        syncBlockManager.setSyncBlock ( true );
        syncBlockManager.setHasNewBlock ( false );
        syncBlockManager.setSyncBlockQueue ( );
        syncUnconfirmedTranManager.setSyncTransaction ( false );
        syncUnconfirmedTranManager.setHasNewTransaction ( false );
        syncUnconfirmedTranManager.setTransactionQueue ( );
        syncUnconfirmedTranManager.setUnComfirmedUnVarTranQueue ( );
        syncTokenManager.setSyncToken ( false );
        syncTokenManager.setHasNewToken ( false );
        syncTokenManager.setSyncTokenQueue ( );
        syncParticipantManager.setSyncParticipant ( false );
        syncParticipantManager.setParticipantQueue ( );
        syncTimer.syncBlockTimer = false;
        syncTimer.syncTransactionTimer = false;
        syncTimer.syncTokenTimer = false;
        syncTimer.syncParticipantTimer = false;
        syncTimer.startSyncBlock = false;
        syncTimer.startSyncTransaction = false;
        syncTimer.startSyncToken = false;
        syncTimer.startSyncParticipant = false;
        syncBlockManager.setSyncCount ( nioSocketChannelManager.getActiveNioSocketChannelCount ( ) );
        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder ( REQUEST, SYNC_BLOCK );
        long blockHeight = initializationManager.getBlockHeight ( );
        builder.setBlockHeight ( blockHeight );
        unconfirmedTranRepository.deleteAll ( );
        nioSocketChannelManager.write ( builder.build ( ) );
    }
}

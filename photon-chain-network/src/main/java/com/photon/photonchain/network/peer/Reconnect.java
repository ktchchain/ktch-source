package com.photon.photonchain.network.peer;

import com.photon.photonchain.network.core.CheckPoint;
import com.photon.photonchain.network.core.SyncBlock;
import com.photon.photonchain.network.ehcacheManager.NioSocketChannelManager;
import com.photon.photonchain.network.ehcacheManager.SyncBlockManager;
import com.photon.photonchain.storage.constants.Constants;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author:Lin
 * @Description:
 * @Date:16:52 2018/5/15
 * @Modified by:
 */
@Component
public class Reconnect {
    private Logger logger = LoggerFactory.getLogger ( Reconnect.class );
    @Autowired
    private CheckPoint checkPoint;
    @Autowired
    private PeerClient peerClient;
    @Autowired
    private SyncBlock syncBlock;
    @Autowired
    private SyncBlockManager syncBlockManager;
    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;

    public void init(ChannelHandlerContext ctx) {
        if ( nioSocketChannelManager.getActiveNioSocketChannelCount ( ) < Constants.FORGABLE_NODES ) {
            logger.info ( "断线重连机制启动..." );
            ctx.close ( );
            syncBlockManager.setSyncBlock ( true );
            nioSocketChannelManager.removeInvalidChannel ( );
            peerClient.init ( );
            try {
                Thread.sleep ( 3000 );
            } catch (InterruptedException e) {
                e.printStackTrace ( );
            }
            //checkPoint.checkDate();
            syncBlock.init ( );
        }
    }
}

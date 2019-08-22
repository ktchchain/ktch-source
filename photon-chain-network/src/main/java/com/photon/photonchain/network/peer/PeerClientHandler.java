package com.photon.photonchain.network.peer;


import com.photon.photonchain.network.core.MessageProcessor;
import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.network.utils.NetWorkUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.PUSH_MAC;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;

/**
 * @author Wu
 * Created by SKINK on 2017/12/24.
 */
@ChannelHandler.Sharable
@Component
public class PeerClientHandler extends SimpleChannelInboundHandler<InesvMessage.Message> {

    private static Logger logger = LoggerFactory.getLogger ( PeerClientHandler.class );

    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;

    @Autowired
    private MessageProcessor messageProcessor;

    @Autowired
    private Reconnect reconnect;

    @Autowired
    private SyncBlockManager syncBlockManager;

    @Autowired
    private SyncUnconfirmedTranManager syncUnconfirmedTranManager;

    @Autowired
    private SyncTokenManager syncTokenManager;

    @Autowired
    private SyncParticipantManager syncParticipantManager;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, InesvMessage.Message msg) throws Exception {
        logger.info ( "接收消息..." );
        switch (msg.getMessageType ( )) {
            case REQUEST:
                messageProcessor.requestProcessor ( ctx, msg );
                break;
            case RESPONSE:
                messageProcessor.responseProcessor ( ctx, msg );
                break;
            default:
                break;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info ( ctx.channel ( ).remoteAddress ( ).toString ( ) + "Peer通道激活" );
        super.channelActive ( ctx );
        InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder ( RESPONSE, PUSH_MAC );
        builder.setMac ( NetWorkUtil.getMACAddress ( ) );
        ctx.writeAndFlush ( builder.build ( ) );
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if ( (!syncBlockManager.isSyncBlock ( ) && !syncUnconfirmedTranManager.isSyncTransaction ( ) && !syncTokenManager.isSyncToken ( ) && !syncParticipantManager.isSyncParticipant ( )) ) {
            nioSocketChannelManager.removeInvalidChannel ( );
            reconnect.init ( ctx );
        }
    }
}

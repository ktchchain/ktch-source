package com.photon.photonchain.network.peer;


import com.photon.photonchain.network.core.MessageProcessor;
import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.network.utils.NetWorkUtil;
import com.photon.photonchain.storage.entity.NodeAddress;
import com.photon.photonchain.storage.repository.NodeAddressRepository;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetSocketAddress;
import java.util.List;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.NODE_ADDRESS;
import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.PUSH_MAC;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;
import static com.photon.photonchain.network.utils.NetWorkUtil.ipToHexString;
import static com.photon.photonchain.network.utils.NetWorkUtil.ping;
import static com.photon.photonchain.network.utils.NetWorkUtil.stringToIp;

/**
 * @Author wu
 * Created by SKINK on 2017/12/24.
 */
@ChannelHandler.Sharable
@Component
public class PeerServerHandler extends SimpleChannelInboundHandler<InesvMessage.Message> {

    private static Logger logger = LoggerFactory.getLogger ( PeerServerHandler.class );

    @Autowired
    private MessageProcessor messageProcessor;

    @Autowired
    private NodeAddressRepository nodeAddressRepository;

    @Autowired
    private InitializationManager initializationManager;

    @Autowired
    private Reconnect reconnect;

    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info ( ctx.channel ( ).remoteAddress ( ).toString ( ) + "Peer通道激活" );
        super.channelActive ( ctx );
        String address = ctx.channel ( ).remoteAddress ( ).toString ( );
        InesvMessage.Message.Builder macBuilder = MessageManager.createMessageBuilder ( RESPONSE, PUSH_MAC );
        macBuilder.setMac ( NetWorkUtil.getMACAddress ( ) );
        ctx.writeAndFlush ( macBuilder.build ( ) );
        String[] addressArray = address.split ( ":" );
        NodeAddress nodeAddress = new NodeAddress ( ipToHexString ( addressArray[0].replaceAll ( "\\/", "" ) ), Integer.valueOf ( addressArray[1] ) );
        if ( nodeAddressRepository.findOne ( nodeAddress.getHexIp ( ) ) != null ) {
            nodeAddressRepository.update ( nodeAddress );
        } else {
            if(ping(nodeAddress.getHexIp())){
                nodeAddressRepository.save ( nodeAddress );
                initializationManager.getNodeList ( ).add ( nodeAddress.getHexIp ( ) );
            }else{
                logger.error("ping不通："+stringToIp(nodeAddress.getHexIp()));
            }
        }
        List<String> nodeAddressList = initializationManager.getNodeList ( );
        nodeAddressList.remove ( ((InetSocketAddress) ctx.channel ( ).remoteAddress ( )).getHostString ( ) );
        InesvMessage.Message.Builder nodeAddressBuilder = MessageManager.createMessageBuilder ( RESPONSE, NODE_ADDRESS );
        nodeAddressBuilder.addAllNodeAddressList ( nodeAddressList );
        ctx.writeAndFlush ( nodeAddressBuilder.build ( ) );
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if ( (!syncBlockManager.isSyncBlock ( ) && !syncUnconfirmedTranManager.isSyncTransaction ( ) && !syncTokenManager.isSyncToken ( ) && !syncParticipantManager.isSyncParticipant ( )) ) {
            nioSocketChannelManager.removeInvalidChannel ( );
            reconnect.init ( ctx );
        }
    }
}

package com.photon.photonchain;


import com.photon.photonchain.interfaces.utils.PropertyUtil;
import com.photon.photonchain.network.core.*;
import com.photon.photonchain.network.ehcacheManager.InitializationManager;
import com.photon.photonchain.network.ehcacheManager.NioSocketChannelManager;
import com.photon.photonchain.network.ehcacheManager.SyncBlockManager;
import com.photon.photonchain.network.peer.PeerClient;
import com.photon.photonchain.storage.entity.NodeAddress;
import com.photon.photonchain.storage.repository.NodeAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author wu
 */
@SpringBootApplication
@EnableCaching
public class InesvchainApplication implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private GenesisBlock genesisBlock;
    @Autowired
    private SyncBlock syncBlock;
    @Autowired
    private Initialization initialization;
    @Autowired
    private PeerClient peerClient;
    @Autowired
    private NodeAddressRepository nodeAddressRepository;
    @Autowired
    private CheckPoint checkPoint;
    @Autowired
    private InitializationManager initializationManager;
    @Autowired
    private ResetData resetData;
    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;
    @Autowired
    private SyncBlockManager syncBlockManager;

    public static void main(String[] args) {
        //SpringApplication.run ( InesvchainApplication.class, args );
        SpringApplicationBuilder builder = new SpringApplicationBuilder(InesvchainApplication.class);
        builder.headless(false).run(args);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        loadInitialNode ( );
        genesisBlock.init ( );
        initialization.init ( );
        peerClient.init ( );
        try {
            Thread.sleep ( 10000 );
        } catch (InterruptedException e) {
            e.printStackTrace ( );
        }
        checkPoint.checkDate ( );
        if ( nioSocketChannelManager.getActiveNioSocketChannelCount ( ) >= BigInteger.ONE.longValue ( ) && initializationManager.getBlockHeight ( ) > 2 ) {
            syncBlockManager.setSyncBlock ( true );
            resetData.backBlock ( 2 );
        }
        syncBlock.init ( );
    }

//    @Autowired


    public void loadInitialNode() {
        String NODE_ADDRESS = PropertyUtil.getProperty ( "NODE_ADDRESS" );
        String[] nodeAddress = NODE_ADDRESS.split ( "\\|" );
        for (String address : nodeAddress) {
            nodeAddressRepository.save ( new NodeAddress ( address, 0 ) );
        }
    }
}



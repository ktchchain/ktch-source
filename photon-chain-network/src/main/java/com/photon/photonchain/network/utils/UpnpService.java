package com.photon.photonchain.network.utils;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.InetAddress;

/**
 * @author Wu
 *
 * Created by SKINK on 2017/12/27.
 */
public class UpnpService {
  private static Logger logger = LoggerFactory.getLogger(UpnpService.class);

  public static void main(String[] args)
      throws IOException, SAXException, ParserConfigurationException {

    GatewayDiscover discover = new GatewayDiscover();

    discover.discover();
    GatewayDevice d = discover.getValidGateway();
    if (null != d) {

    } else {

      return;
    }
    InetAddress localAddress = d.getLocalAddress();

    String externalIPAddress = d.getExternalIPAddress();

  }

}

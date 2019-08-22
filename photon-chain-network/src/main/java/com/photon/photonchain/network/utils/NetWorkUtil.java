package com.photon.photonchain.network.utils;

import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.net.*;

/**
 * @author Wu
 * <p>
 * Created by SKINK on 2017/12/26.
 */
public class NetWorkUtil {

    public static InetAddress convertAddress(int seed) throws UnknownHostException {
        byte[] v4addr = new byte[4];
        v4addr[0] = (byte) (0xFF & (seed));
        v4addr[1] = (byte) (0xFF & (seed >> 8));
        v4addr[2] = (byte) (0xFF & (seed >> 16));
        v4addr[3] = (byte) (0xFF & (seed >> 24));
        return InetAddress.getByAddress ( v4addr );
    }

    public static InetAddress convertAddress(String hexIp) throws UnknownHostException {
        return convertAddress ( bytesToInt ( Hex.decode ( hexIp ) ) );
    }

    public static String ipToHexString(String ipAddr) {
        byte[] ret = new byte[4];
        String[] ipArr = ipAddr.split ( "\\." );
        ret[0] = (byte) (Integer.parseInt ( ipArr[3] ) & 0xFF);
        ret[1] = (byte) (Integer.parseInt ( ipArr[2] ) & 0xFF);
        ret[2] = (byte) (Integer.parseInt ( ipArr[1] ) & 0xFF);
        ret[3] = (byte) (Integer.parseInt ( ipArr[0] ) & 0xFF);
        return Hex.toHexString ( ret );
    }

    public static String stringToIp(String str) {
        int m = str.length ( ) / 2;
        if ( m * 2 < str.length ( ) ) {
            m++;
        }
        String[] strs = new String[m];
        int j = 0;
        for (int i = 0; i < str.length ( ); i++) {
            if ( i % 2 == 0 ) {//每隔两个
                strs[j] = str.charAt ( i ) + "";
            } else {
                strs[j] = strs[j] + str.charAt ( i );
                j++;
            }
        }
        StringBuffer sb = new StringBuffer ( );
        for (int i = strs.length - 1; i >= 0; i--) {
            sb.append ( Long.parseLong ( strs[i], 16 ) );
            if ( i != 0 ) {
                sb.append ( "." );
            }
        }
        return sb.toString ( );
    }

    public static int bytesToInt(byte[] bytes) {
        int addr = bytes[3] & 0xFF;
        addr |= ((bytes[2] << 8) & 0xFF00);
        addr |= ((bytes[1] << 16) & 0xFF0000);
        addr |= ((bytes[0] << 24) & 0xFF000000);
        return addr;
    }

    public static String getMACAddress() {
        byte[] mac = new byte[0];
        try {
            mac = NetworkInterface.getByInetAddress ( InetAddress.getLocalHost ( ) ).getHardwareAddress ( );
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace ( );
        }
        StringBuilder sb = new StringBuilder ( );
        for (int i = 0; i < mac.length; i++) {
            if ( i != 0 ) {
                sb.append ( "-" );
            }
            String s = Integer.toHexString ( mac[i] & 0xFF );
            sb.append ( s.length ( ) == 1 ? 0 + s : s );
        }
        return sb.toString ( ).toUpperCase ( );
    }

    public static boolean ping(String hexIp) {
        Socket socket = new Socket ( );
        try {
            socket.connect ( new InetSocketAddress ( convertAddress ( hexIp ), 1906 ), 500 );
        } catch (Exception e) {
            return false;
        } finally {
            try {
                socket.close ( );
            } catch (IOException e) {
                e.printStackTrace ( );
            }
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println ( stringToIp ( "d9885c2f" ) );
    }
}

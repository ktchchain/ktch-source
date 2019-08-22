package com.photon.photonchain.interfaces.utils;

import com.photon.photonchain.storage.constants.Constants;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author:Lin
 * @Description:
 * @Date:15:14 2018/1/11
 * @Modified by:
 */
public class FileUtil {

    public static boolean writeFileContent(String filepath, String newstr) throws IOException {
        Boolean bool = false;
        String filein = newstr + "\r\n";
        String temp = "";
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        FileOutputStream fos = null;
        PrintWriter pw = null;
        try {
            File file = new File ( filepath );
            fis = new FileInputStream ( file );
            isr = new InputStreamReader ( fis );
            br = new BufferedReader ( isr );
            StringBuffer buffer = new StringBuffer ( );
            for (int i = 0; (temp = br.readLine ( )) != null; i++) {
                buffer.append ( temp );
                buffer = buffer.append ( System.getProperty ( "line.separator" ) );
            }
            buffer.append ( filein );
            fos = new FileOutputStream ( file );
            pw = new PrintWriter ( fos );
            pw.write ( buffer.toString ( ).toCharArray ( ) );
            pw.flush ( );
            bool = true;
        } catch (Exception e) {
            e.printStackTrace ( );
        } finally {
            if ( pw != null ) {
                pw.close ( );
            }
            if ( fos != null ) {
                fos.close ( );
            }
            if ( br != null ) {
                br.close ( );
            }
            if ( isr != null ) {
                isr.close ( );
            }
            if ( fis != null ) {
                fis.close ( );
            }
        }
        return bool;
    }

    public static List<String> readFileByLines(String fileName) {
        File file = new File ( fileName );
        BufferedReader reader = null;
        List<String> list = new ArrayList<String> ( );
        try {
            reader = new BufferedReader ( new FileReader ( file ) );
            String tempString = null;
            while ((tempString = reader.readLine ( )) != null) {
                list.add ( tempString );
            }
            reader.close ( );
        } catch (IOException e) {
            e.printStackTrace ( );
        } finally {
            if ( reader != null ) {
                try {
                    reader.close ( );
                } catch (IOException e1) {
                }
            }
        }
        return list;
    }

    public static String readToString(String fileName) {
        String encoding = "UTF-8";
        File file = new File ( fileName );
        Long filelength = file.length ( );
        byte[] filecontent = new byte[filelength.intValue ( )];
        try {
            FileInputStream in = new FileInputStream ( file );
            in.read ( filecontent );
            in.close ( );
        } catch (FileNotFoundException e) {
            e.printStackTrace ( );
        } catch (IOException e) {
            e.printStackTrace ( );
        }
        try {
            return new String ( filecontent, encoding );
        } catch (UnsupportedEncodingException e) {
            System.err.println ( "The OS does not support " + encoding );
            e.printStackTrace ( );
            return null;
        }
    }

    public static String readLastLine(File file, String charset) throws IOException {
        if ( !file.exists ( ) || file.isDirectory ( ) || !file.canRead ( ) ) {
            return null;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile ( file, "r" );
            long len = raf.length ( );
            if ( len == 0L ) {
                return "";
            } else {
                long pos = len - 1;
                while (pos > 0) {
                    pos--;
                    raf.seek ( pos );
                    if ( raf.readByte ( ) == '\n' ) {
                        break;
                    }
                }
                if ( pos == 0 ) {
                    raf.seek ( 0 );
                }
                byte[] bytes = new byte[(int) (len - pos)];
                raf.read ( bytes );
                if ( charset == null ) {
                    return new String ( bytes );
                } else {
                    return new String ( bytes, charset );
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace ( );
        } finally {
            if ( raf != null ) {
                try {
                    raf.close ( );
                } catch (Exception e2) {
                }
            }
        }
        return null;
    }
}
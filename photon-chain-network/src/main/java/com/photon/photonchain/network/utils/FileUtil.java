package com.photon.photonchain.network.utils;

import com.photon.photonchain.storage.constants.Constants;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
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
            if ( !file.exists ( ) ) {
                file.createNewFile ( );
            }
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
        File file = new File(fileName);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(filecontent, encoding);
        } catch (UnsupportedEncodingException e) {
            System.err.println("The OS does not support " + encoding);
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, String> traverseFolder(String path) {
        String encoding = "UTF-8";
        Map<String, String> resultMap = new HashMap<> ( );
        File file = new File ( path );
        if ( !file.exists ( ) ) {
            file.mkdir ( );
        }
        File[] files = file.listFiles ( );
        if ( files == null || files.length == 0 ) {
        } else {
            for (File file2 : files) {
                String account = "";
                String address = "";
                if ( file2.isDirectory ( ) ) {
                    traverseFolder ( file2.getAbsolutePath ( ) );
                } else {
                    account = readAccountFileString ( file2 );
                    address = file2.getName ( );
                }
                if ( address.contains ( "kx" ) ) {
                    resultMap.put ( address, account );
                }
            }
        }

        return resultMap;
    }

    public static <T extends Serializable> T clone(T obj) {
        T cloneObj = null;
        try {
            // 写入字节流
            ByteArrayOutputStream out = new ByteArrayOutputStream ( );
            ObjectOutputStream obs = new ObjectOutputStream ( out );
            obs.writeObject ( obj );
            obs.close ( );

            // 分配内存，写入原始对象，生成新对象
            ByteArrayInputStream ios = new ByteArrayInputStream ( out.toByteArray ( ) );
            ObjectInputStream ois = new ObjectInputStream ( ios );
            // 返回生成的新对象
            cloneObj = (T) ois.readObject ( );
            ois.close ( );
        } catch (Exception e) {
            e.printStackTrace ( );
        }
        return cloneObj;
    }

    public static String readAccountFileString(File file) {
        String encoding = "UTF-8";
        Long filelength = file.length ( ) - "\r\n".length ( );
        Long readLenght = filelength < 0 ? 0 : filelength;
        byte[] filecontent = new byte[readLenght.intValue ( )];
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

    public static void writeNewMap(String filepath, Map<String, Map<String, Long>> accountAssets) {
        File file = new File ( filepath );
        FileOutputStream fos = null;
        PrintWriter pw = null;
        String temp = null;
        StringBuffer buffer = new StringBuffer ( );
        for (String account : accountAssets.keySet ( )) {
            Map<String, Long> accountMap = accountAssets.get ( account );
            if ( account.equals ( Constants.PTN ) ) {
                temp = account + "-" + accountMap.get ( Constants.SYNC_BLOCK_HEIGHT );
            } else {
                temp = account + "-" + accountMap.get ( Constants.TOTAL_INCOME ) + "-" + accountMap.get ( Constants.TOTAL_EFFECTIVE_INCOME ) + "-" + accountMap.get ( Constants.TOTAL_EXPENDITURE ) + "-" + accountMap.get ( Constants.BALANCE );
            }
            buffer.append ( temp );
            buffer.append ( System.getProperty ( "line.separator" ) );
        }
        try {
            fos = new FileOutputStream ( file );
            pw = new PrintWriter ( fos );
            pw.write ( buffer.toString ( ).toCharArray ( ) );
            pw.flush ( );
        } catch (Exception e1) {
            e1.printStackTrace ( );
        } finally {
            if ( pw != null ) {
                pw.close ( );
            }
        }
    }
}
package com.sadiq.megatransfer;

import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by sadiq7753 on 5/24/2015.
 */
public class Configuration {
    public static LinkedList<PeerConnection.IceServer> getIceServer(){
        // Initialize ICE server list
        LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("turn:cloudkibo@162.243.217.34:3478?transport=udp",
                "cloudkibo", "cloudkibo"));
        iceServers.add(new PeerConnection.IceServer("turn:turn.bistri.com:80?transport=udp",
                "homeo", "homeo"));
        iceServers.add(new PeerConnection.IceServer("turn:turn.bistri.com:80?transport=tcp",
                "homeo", "homeo"));
        iceServers.add(new PeerConnection.IceServer("turn:turn.anyfirewall.com:443?transport=tcp",
                "webrtc", "webrtc"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.anyfirewall.com:3478"));

        return iceServers;
    }

    public static MediaConstraints getMediaConstraints(){
        // Initialize PeerConnection
        MediaConstraints pcMediaConstraints = new MediaConstraints();
        pcMediaConstraints.optional.add(new MediaConstraints.KeyValuePair(
                "DtlsSrtpKeyAgreement", "true"));
        pcMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        pcMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        return pcMediaConstraints;
    }

    private static final int CHUNK_SIZE = 16000;
    private static final int CHUNKS_PER_ACK = 16;
    private static final String FOLDER_NAME = "MegaTransfer";

    public static byte[] convertFileToByteArray(File f) {

        java.io.FileInputStream fis = null;
        byte[] stream = new byte[(int) f.length()];
        try {
            fis = new java.io.FileInputStream(f);
        } catch (java.io.FileNotFoundException ex) {
            return null;
        }
        try {
            fis.read(stream);
            fis.close();
        } catch (java.io.IOException ex) {
            return null;
        }
        return stream;
    }

    public static Boolean convertByteArrayToFile(byte[] bytes, String fileName){

        try {

                String folderName = FOLDER_NAME;
                //convert array of bytes into file
                FileOutputStream fileOuputStream =
                        new FileOutputStream(getDownloadStorageDir(folderName)+"/"+fileName);
                fileOuputStream.write(bytes);
                fileOuputStream.close();

                return true;


        }catch(Exception e){
            e.printStackTrace();
            return false;
        }

    }

    public static JSONObject getFileMetaData(String filePath){

        File file = new File(filePath);

        JSONObject meta = new JSONObject();

        try {

            meta.put("name", file.getName());
            meta.put("size", file.length());
            meta.put("filetype", getExtension(filePath));
            meta.put("browser", "chrome"); // This is a hack. Will remove it later.

        } catch (JSONException e) {
            e.printStackTrace();

            return meta; // This will contain some missing information. Client should know about exception from this

        }

        return meta;

    }

    public static ByteBuffer toByteBuffer(String text){
        return ByteBuffer.wrap(text.getBytes());
    }

    public static int getChunkSize(){
        return CHUNK_SIZE;
    }

    public static int getChunksPerACK(){
        return CHUNKS_PER_ACK;
    }


    public static File getDownloadStorageDir(String foldername) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), foldername);
        if (!file.mkdirs()) {
            Log.e("FILESTORAGE", "Directory not created");
        }
        return file;
    }

    public static String getExtension(String uri) {
        if (uri == null) {
            return null;
        }

        int dot = uri.lastIndexOf(".");
        if (dot >= 0) {
            return uri.substring(dot);
        } else {
            // No extension.
            return "";
        }
    }

}

package com.sadiq.megatransfer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.Base64;
import com.ipaulpro.afilechooser.utils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRendererGui;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;


public class SelectFile extends ActionBarActivity {

    String rid;
    PeerConnectionFactory factory;
    Peer peer;

    String peerId;
    String filePath;
    Boolean initiator;
    public String fileData;

    String fileNameToSave;
    int numberOfChunksInFileToSave;
    int numberOfChunksReceived;
    int sizeOfFileToSave;
    int chunkNumberToRequest;

    SocketService socketService;
    boolean isBound = false;

    private HashMap<String, String> user;
    private String room;
    ArrayList<Byte> fileBytesArray = new ArrayList<Byte>();



    private static final int REQUEST_CHOOSER = 1234;
    private boolean initiatorFileTransfer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_file);

        Intent i = new Intent(this, SocketService.class);
        startService(i);
        bindService(i, socketConnection, Context.BIND_AUTO_CREATE);

        Button btnClose = (Button) findViewById(R.id.button2);
        Intent ii = getIntent();
        rid = ii.getExtras().getString("nameOfFriend");
      Log.e("sadiq: ", rid);
        btnClose.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                // Closing SecondScreen Activity
                Intent getContentIntent = FileUtils.createGetContentIntent();

                Intent intent = Intent.createChooser(getContentIntent, "Select a file");
                startActivityForResult(intent, REQUEST_CHOOSER);
                //finish();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHOOSER:
                if (resultCode == RESULT_OK) {

                    final Uri uri = data.getData();

                    // Get the File path from the Uri
                    String path = FileUtils.getPath(this, uri);

                   Log.e("SADIQ", path);

                    // Alternatively, use FileUtils.getFile(Context, Uri)
                    if (path != null && FileUtils.isLocal(path)) {
                        File file = new File(path);

                        try {

                            fileData = Base64.encode(FileUtils.loadFile(file));

                            initiatorFileTransfer = true;

                            initiator = true;

                            createPeerConnectionFactory();

                            peer = new Peer();

                            if(initiator){
                                createOffer();
                            }


                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
        }
    }

    public void requestChunk(){

        Log.d("FILERECEIVE", "Requesting CHUNK: "+ chunkNumberToRequest);

        JSONObject request_chunk = new JSONObject();

        try {

            request_chunk.put("eventName", "request_chunk");

            JSONObject request_data = new JSONObject();
            request_data.put("chunk", chunkNumberToRequest);
            request_data.put("browser", "chrome"); // This chrome is hardcoded for testing purpose

            request_chunk.put("data", request_data);

            peer.dc.send(new DataChannel.Buffer(Configuration.toByteBuffer(request_chunk.toString()), false));


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    protected void onDestroy() {

        if(isBound){
            unbindService(socketConnection);
        }

        super.onDestroy();
    }


    public void createPeerConnectionFactory(){
        PeerConnectionFactory.initializeAndroidGlobals(getApplicationContext(), true, true,

                VideoRendererGui.getEGLContext());

        factory = new PeerConnectionFactory();
    }

    public void createOffer(){
        peer.pc.createOffer(peer, Configuration.getMediaConstraints());
    }

    public void createAnswer(JSONObject payload){
        try{

            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
            peer.pc.createAnswer(peer, Configuration.getMediaConstraints());

        }catch(JSONException e){
            Toast.makeText(getApplicationContext(),
                    e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void setRemoteSDP(JSONObject payload){

        try{

            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);

        }catch(JSONException e){
            Toast.makeText(getApplicationContext(),
                    e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void addIceCandidate(JSONObject payload){

        try{

            if (peer.pc.getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.getString("id"),
                        payload.getInt("label"),
                        payload.getString("candidate")
                );
                peer.pc.addIceCandidate(candidate);
            }

        }catch(JSONException e){
            Toast.makeText(getApplicationContext(),
                    e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
        }

    }


    public void sendSocketMessageDataChannel(String msg){
        socketService.sendSocketMessageDataChannel(msg, rid);
    }

    private class DcObserver implements DataChannel.Observer {

        public DcObserver(){

        }

        @Override
        public void onMessage(DataChannel.Buffer buffer) {

            ByteBuffer data = buffer.data;
            final byte[] bytes = new byte[ data.capacity() ];
            data.get(bytes);

            final File file = new File(filePath);

            if(buffer.binary){

                String strData = new String( bytes );
                Log.d("FILETRANSFER", strData);

                runOnUiThread(new Runnable(){
                    public void run() {
                        for(int i=0; i<bytes.length; i++)
                            fileBytesArray.add(bytes[i]);

                        if (numberOfChunksReceived % Configuration.getChunksPerACK() == (Configuration.getChunksPerACK() - 1)
                                || numberOfChunksInFileToSave == (numberOfChunksReceived + 1)) {

                            if (numberOfChunksInFileToSave > numberOfChunksReceived) {
                                chunkNumberToRequest += Configuration.getChunksPerACK();

                                requestChunk();
                            }

                        }

                        numberOfChunksReceived++;
                    }
                });

            }
            else {

                runOnUiThread(new Runnable() {
                    public void run() {
                        String strData = new String( bytes );

                        Log.d("FILETRANSFER", strData);

                        try {

                            JSONObject jsonData = new JSONObject(strData);

                            if(jsonData.getJSONObject("data").has("file_meta")){

                                fileNameToSave = jsonData.getJSONObject("data").getJSONObject("file_meta").getString("name");
                                sizeOfFileToSave = jsonData.getJSONObject("data").getJSONObject("file_meta").getInt("size");
                                numberOfChunksInFileToSave = (int) Math.ceil(sizeOfFileToSave / Configuration.getChunkSize());
                                numberOfChunksReceived = 0;
                                chunkNumberToRequest = 0;

                            }
                            else if(jsonData.getJSONObject("data").has("kill")){

                            }
                            else if(jsonData.getJSONObject("data").has("ok_to_download")){

                            }
                            else {

                                boolean isBinaryFile = true;

                                int chunkNumber = jsonData.getJSONObject("data").getInt("chunk");

                                Log.d("FILETRANSFER", "Chunk Number "+ chunkNumber);
                                if(chunkNumber % Configuration.getChunksPerACK() == 0){
                                    for(int i = 0; i< Configuration.getChunksPerACK(); i++){

                                        if(file.length() < Configuration.getChunkSize()){
                                            ByteBuffer byteBuffer = ByteBuffer.wrap(Configuration.convertFileToByteArray(file));
                                            DataChannel.Buffer buf = new DataChannel.Buffer(byteBuffer, isBinaryFile);

                                            peer.dc.send(buf);
                                            break;
                                        }

                                        Log.d("FILETRANSFER", "File Length "+ file.length());
                                        Log.d("FILETRANSFER", "Ceiling "+ Math.ceil(file.length() / Configuration.getChunkSize()));
                                        if((chunkNumber+i) >= Math.ceil(file.length() / Configuration.getChunkSize())){
                                            break;
                                        }

                                        int upperLimit = (chunkNumber + i + 1) * Configuration.getChunkSize();

                                        if(upperLimit > (int)file.length()){
                                            upperLimit = (int)file.length();
                                        }

                                        int lowerLimit = (chunkNumber + i) * Configuration.getChunkSize();
                                        Log.d("LIMITS", ""+ lowerLimit +" "+ upperLimit);
                                        ByteBuffer byteBuffer = ByteBuffer.wrap(Configuration.convertFileToByteArray(file), lowerLimit, upperLimit);
                                        DataChannel.Buffer buf = new DataChannel.Buffer(byteBuffer, isBinaryFile);

                                        peer.dc.send(buf);
                                        Log.d("CHUNK", "Chunk has been sent");
                                    }
                                }
                            }

                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });

            }




        }

        @Override
        public void onStateChange() {

            Log.e("FILE_ERROR", "DataChannel State Changed");

        }
    }

    private class PcObserver implements PeerConnection.Observer{

        public PcObserver(){

        }

        @Override
        public void onAddStream(MediaStream arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onDataChannel(final DataChannel dataChannel) {
            final DataChannel dc = dataChannel;
            runOnUiThread(new Runnable() {
                public void run() {
                    //peer.dc = dc;

                    DcObserver dcObserver = new DcObserver();

                    peer.dc.registerObserver(dcObserver);

                    //dc.registerObserver(dcObserver);
                }
            });


        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            try {

                JSONObject payload = new JSONObject();
                payload.put("type", "candidate");
                payload.put("label", candidate.sdpMLineIndex);
                payload.put("id", candidate.sdpMid);
                payload.put("candidate", candidate.sdp);

                sendSocketMessageDataChannel(payload.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onRemoveStream(MediaStream arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onRenegotiationNeeded() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState arg0) {
            // TODO Auto-generated method stub

        }

    }


    private class Peer implements SdpObserver {

        private PeerConnection pc;
        private DataChannel dc;

        public Peer() {

            PcObserver pcObserver = new PcObserver();

            pc = factory.createPeerConnection(Configuration.getIceServer(),
                    Configuration.getMediaConstraints(), pcObserver);

            dc = pc.createDataChannel("sendDataChannel", new DataChannel.Init());

            //DcObserver dcObserver = new DcObserver();

            //dc.registerObserver(dcObserver);
        }

        @Override
        public void onCreateFailure(String msg) {
            Toast.makeText(getApplicationContext(),
                    msg, Toast.LENGTH_SHORT)
                    .show();

        }

        @Override
        public void onCreateSuccess(SessionDescription sdp) {
            try {

                JSONObject payload = new JSONObject();
                payload.put("type", sdp.type.canonicalForm());
                payload.put("sdp", sdp.description);

                sendSocketMessageDataChannel(payload.toString());

                pc.setLocalDescription(Peer.this, sdp);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onSetFailure(String arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSetSuccess() {
            // TODO Auto-generated method stub

        }

    }



    private ServiceConnection socketConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SocketService.SocketBinder binder = (SocketService.SocketBinder) service;
            socketService = binder.getService();
            isBound = true;

            binder.setListener(new BoundServiceListener() {
                @Override
                public void receiveSocketMessage(String type, String body) {
                }

                @Override
                public void receiveSocketArray(String type, JSONObject payload) {

                    if(type.equals("message")){

                        try {

                            String type2 = payload.getString("type");

                            Toast.makeText(getApplicationContext(),
                                    payload.toString(), Toast.LENGTH_SHORT)
                                    .show();

                            if(type2.equals("offer")){

                                createPeerConnectionFactory();

                                peer = new Peer();

                                SessionDescription sdp = new SessionDescription(
                                        SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                                        payload.getString("sdp")
                                );
                                peer.pc.setRemoteDescription(peer, sdp);
                                peer.pc.createAnswer(peer, Configuration.getMediaConstraints());
                            }
                            else if(type2.equals("answer")){
                                SessionDescription sdp = new SessionDescription(
                                        SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                                        payload.getString("sdp")
                                );
                                peer.pc.setRemoteDescription(peer, sdp);
                            }
                            else if(type2.equals("candidate")){
                                PeerConnection pc = peer.pc;
                                if (pc.getRemoteDescription() != null) {
                                    IceCandidate candidate = new IceCandidate(
                                            payload.getString("id"),
                                            payload.getInt("label"),
                                            payload.getString("candidate")
                                    );
                                    pc.addIceCandidate(candidate);
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (NullPointerException e){
                            e.printStackTrace();

							/*
							 * todo This needs to be fixed. It does not receive offer and receives
							 * the candidate. This is a network error
							 */

                            Toast.makeText(getApplicationContext(),
                                    "Network error occurred. Try again after connecting to Internet", Toast.LENGTH_SHORT)
                                    .show();

                        }

                    }

                }
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;

        }
    };



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_select_file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

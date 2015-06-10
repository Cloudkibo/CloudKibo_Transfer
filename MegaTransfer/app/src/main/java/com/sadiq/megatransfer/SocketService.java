package com.sadiq.megatransfer;

/**
 * Created by sadiq7753 on 5/24/2015.
 all rights reserved...

 */

import java.net.URISyntaxException;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;

import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

public class SocketService extends Service {

    private final IBinder socketBinder = new SocketBinder();
    private BoundServiceListener mListener;

    Socket socket;

    private HashMap<String, String> user;
    private String room;

    @Override
    public IBinder onBind(Intent intent) {
        return socketBinder;
    }


    public class SocketBinder extends Binder {

        public SocketService getService() {
            return SocketService.this;
        }

        public void setListener(BoundServiceListener listener) {
            mListener = listener;
        }

    }

    public void setSocketIOConfig() {


        try {

            socket = IO.socket("http://45.55.233.191");


            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    socket.emit("foo", "hi");
                    //socket.disconnect();
                }

            }).on("id", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    mListener.receiveSocketMessage("id", args[0].toString());

                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("msg", args[0]);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    socket.emit("message", obj);
                }
            }).on("event", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                }

            }).on("message", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("msg", args[0]);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    mListener.receiveSocketArray("message", obj);
                }
            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                }

            });
            socket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //user = (HashMap) intent.getExtras().get("user");
        //room = intent.getExtras().getString("room");

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {

        setSocketIOConfig();

        super.onCreate();
    }


    public void sendSocketMessageDataChannel(String msg, String filePeer) {

        JSONObject message = new JSONObject();

        try {

            message.put("msg", msg);
            message.put("to", filePeer);
            message.put("from", user.get("username"));

            socket.emit("message", new JSONArray().put(message));


        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
package com.sadiq.megatransfer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivity extends ActionBarActivity {

    String rid;
    String uid;

    SocketService socketService;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //---button code to next screen
        Button btnNextScreen = (Button) findViewById(R.id.button);
        final EditText idtext = (EditText) findViewById(R.id.editText);


        // Listening to button event
        btnNextScreen.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                //Starting a new Intent

                rid = idtext.getText().toString();
                Intent nextScreen = new Intent(getApplicationContext(),
                        SelectFile.class);
                nextScreen.putExtra("nameOfFriend", rid);

                startActivity(nextScreen);

            }
        });

        Intent i = new Intent(this, SocketService.class);
        startService(i);
        bindService(i, socketConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onDestroy(){
        if(isBound)
            unbindService(socketConnection);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void setID(String body){
        textView.setText("Your ID: "+ "LUN"+ body);
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

    private boolean isBound;
    private ServiceConnection socketConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SocketService.SocketBinder binder = (SocketService.SocketBinder) service;
            socketService = binder.getService();
            isBound = true;

            binder.setListener(new BoundServiceListener() {
                @Override
                public void receiveSocketMessage(String type, String body) {

                    if(type.equals("id")) {
                        Log.e("Sadiq", type);
                        Log.e("Id", body);
                    }

                    //setID(body);

                }

                @Override
                public void receiveSocketArray(String type, JSONObject body) {

                }
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;

        }
    };
}

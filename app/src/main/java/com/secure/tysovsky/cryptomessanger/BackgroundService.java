package com.secure.tysovsky.cryptomessanger;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.engineio.client.Socket;
import com.github.nkzawa.socketio.client.IO;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

public class BackgroundService extends Service {
    String username = "undefined";
    ArrayList<notificationPair> newMessages = new ArrayList<>();
    com.github.nkzawa.socketio.client.Socket socket;
    public BackgroundService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.Log("-----");
        SharedPreferences prefs = getSharedPreferences("CryptoSP", MODE_PRIVATE);
        username = prefs.getString("username", null);
        if(username == null){
            username = "undefined";
        }


        Utils.Log("BackgroundService.onStartCommand.username: " + username);
        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            socket = IO.socket("http://ec2-52-27-83-251.us-west-2.compute.amazonaws.com:80", options);


            final JSONObject data = new JSONObject();
            data.put("sender", username);
            Utils.Log("BackgroundService.onStartCommand.data: " + data.toString());
            socket.on("newMessage", newMessageArrived);
            socket.on(socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    socket.emit("authenticateService", data);
                }
            });
            socket.on(socket.EVENT_RECONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    socket.emit("authenticateService", data);
                }
            });
            socket.connect();


        } catch (URISyntaxException e) {
            Utils.Log("ERROR: BackgroundService.onStartCommand: URISyntaxException: " + e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            Utils.Log("ERROR: BackgroundService.onStartCommand: JSONException: " + e.getMessage());
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private Emitter.Listener newMessageArrived = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            int index = -1;
            JSONObject data = (JSONObject) args[0];

            Utils.Log("BackgroundService.newMessageArrived: " + data.toString());

            try {
                for (int i = 0; i < newMessages.size(); i++) {
                    if (newMessages.get(i).username.equals(data.getString("sender"))) {
                        index = i;
                    }
                }

                if(index > -1){
                    newMessages.get(index).incrementMessages();
                }
                else{
                    newMessages.add(new notificationPair(data.getString("sender")));
                }
            } catch (JSONException e) {
                Utils.Log("ERROR: BackgroundService.newMessageArrived: JSONException: " + e.getMessage());
                e.printStackTrace();
            }


            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(BackgroundService.this);
            mBuilder.setContentTitle("New Message")
                    .setSmallIcon(R.drawable.ic_message)
                    .setContentText("New messages recieved")
                            .setAutoCancel(true)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(getNotificationString()));

            Intent intent = new Intent(BackgroundService.this, MainActivity.class);
            intent.putExtra("username", username);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(BackgroundService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);

            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(01, mBuilder.build());
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(socket.connected()){
            socket.disconnect();
        }
    }

    class notificationPair{
        String username;
        int unreadMessages;

        public notificationPair(String username){
            this.username = username;
            this.unreadMessages = 1;
        }

        public notificationPair(String username, int unreadMessages){
            this.username = username;
            this.unreadMessages = unreadMessages;
        }

        public void incrementMessages(){
            this.unreadMessages++;
        }

        public void incrementMessage(int messages){
            this.unreadMessages+=messages;
        }
    }

    public String getNotificationString(){
        String toReturn = "";
        for(int i = 0; i< newMessages.size(); i++){
            if(newMessages.get(i).unreadMessages == 1){
                toReturn+="1 new message from " + newMessages.get(i).username + "\n";
            }else{
                toReturn+=newMessages.get(i).unreadMessages + " new messages from " + newMessages.get(i).username + "\n";
            }
        }

        return toReturn;
    }
}

package com.secure.tysovsky.cryptomessanger;

import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class ChatActivity extends ActionBarActivity {

    private static String recipient = "";
    private static String sender  = "";
    private static String _password = "";
    private static String AuthMessage = "";
    private static byte[] AuthIV = null;
    private Boolean isAuthenticated = false;

    List<Message> messages = new ArrayList<Message>();
    ListView messagesListView;
    ArrayAdapter<Message> adapter;
    private static String password = "";


    EditText messageText;
    EditText passwordText;
    Button btnSend;
    String initialMessage = "";

    Calendar calendar;

    private com.github.nkzawa.socketio.client.Socket socket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.primary)));

        Utils.Log("-----");
        //Retrieve the recipient and sender usernames
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            recipient = extras.getString("recipient");
            sender = extras.getString("sender");
            initialMessage = extras.getString("message", "");
            _password = extras.getString("password", "");
            Utils.Log("ChatActivity.onCreate.recipient: " + recipient);
            Utils.Log("ChatActivity.onCreate.sender: " + sender);
            Utils.Log("ChatActivity.onCreate.initialMessage: " + initialMessage);
            Utils.Log("ChatActivity.onCreate._password: " + _password);
        }
        else{
            Utils.Log("ChatActivity.onCreate: not extras found");
        }

        messageText = (EditText)findViewById(R.id.user_message_text);
        btnSend = (Button)findViewById(R.id.btn_send_message);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendMessage();
            }
        });

        //Set up the listView
        messagesListView = (ListView)findViewById(R.id.messagesListView);
        adapter = new MessagesArrayAdapter();
        messagesListView.setAdapter(adapter);

        setTitle(recipient);

        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            socket = IO.socket("http://ec2-52-27-83-251.us-west-2.compute.amazonaws.com:80", options);
            socket.on("authenticate", handleAuthorizationMessage);
            socket.connect();
            Authenticate();

        } catch (URISyntaxException e) {
            Utils.Log("ERROR: ChatActivity.onCreate: URISyntaxException: " + e.getMessage());
            e.printStackTrace();
        }


        //Dialogue for checking the corectness of a password
       MaterialDialog dialog = new MaterialDialog.Builder(ChatActivity.this)
                .title(R.string.enter_password)
                .titleColor(getResources().getColor(R.color.lightest))
                .customView(R.layout.layout_enter_password, true)
                .backgroundColor(getResources().getColor(R.color.light))
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .positiveColor(getResources().getColor(R.color.lightest))
                .negativeColor(getResources().getColor(R.color.lightest))
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(!isAuthenticated){
                            Toast.makeText(ChatActivity.this,R.string.please_enter_password, Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                })
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);


                        String typedPass = passwordText.getText().toString();


                        if(!AuthMessage.isEmpty() && AuthIV != null){
                            String decryptedMessage = Crypto.AESdecrypt(typedPass, AuthMessage, AuthIV);
                            if(decryptedMessage.equals(Utils.AUTHENTICATION_MESSAGE)){
                                isAuthenticated = true;
                                password = typedPass;
                                socket.on("message", handleIncomingMessages);
                                JSONObject data= new JSONObject();
                                try {
                                    data.put("sender", sender);
                                    data.put("recipient", recipient);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                socket.emit("retrieve", data);
                            }
                            else{
                                Toast.makeText(ChatActivity.this, R.string.wrong_password, Toast.LENGTH_LONG).show();
                                finish();
                            }
                        }

                        else{
                            Toast.makeText(ChatActivity.this, R.string.server_error, Toast.LENGTH_LONG).show();
                            finish();
                        }

                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        Toast.makeText(ChatActivity.this, R.string.please_enter_password, Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        super.onNeutral(dialog);
                        finish();
                    }
                })
                .build();

        passwordText = (EditText)dialog.getCustomView().findViewById(R.id.conversation_password);
        if(initialMessage.isEmpty()){
            dialog.show();
        }

        initialMessage = "";
        calendar = Calendar.getInstance();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(socket != null){
            socket.disconnect();
        }

    }

    @Override
    protected  void onResume(){
        super.onResume();
        if(!socket.connected()){
            //socket.connect();
        }
        //Authenticate();
    }

    private void Authenticate(){
        Utils.Log("------");
        JSONObject data = new JSONObject();
        try{
            data.put("sender", sender);
            data.put("recipient", recipient);
            //If true,the conversation has just been created
            if(!initialMessage.isEmpty()){
                sendMessage(Utils.AUTHENTICATION_MESSAGE, _password);
                sendMessage(initialMessage, _password);
                data.put("first", true);
                //initialMessage = "";
            }
            else {
                data.put("first", false);
            }
            Utils.Log("ChatActivity.Authenticate.data: " + data.toString());
            socket.emit("authenticate", data);
        } catch (JSONException e) {
            Utils.Log("ERROR: ChatActivity.Authenticate: JSONException: " + e.getMessage());
        }
    }

    private void sendMessage(){
        Utils.Log("------");
        String message = messageText.getText().toString();
        Utils.Log("ChatActivity.sendMessage().message: " + message);
        //If the message is empty, return
        if(message.isEmpty()){return;}
        messageText.setText("");
        String encryptedMessage = "";
        //Generate a random input vector
        byte[] IV = Crypto.GenerateRandomIV();
        //Convert IV into base 64 encoded string
        String ivString = Base64.encodeToString(IV, Base64.NO_WRAP);
        Utils.Log("ChatActivity.sendMessage().ivString: " + ivString);
        //Encrypt the message
        if(_password.isEmpty()){
            encryptedMessage = Crypto.AESencrypt(password, message, IV);
        }
        else{
            encryptedMessage = Crypto.AESencrypt(_password, message, IV);
        }

        Utils.Log("ChatActivity.sendMessage().encryptedMessage: " + encryptedMessage);

        //Update the listView
        messages.add(new Message(message, "", sender));
        adapter.notifyDataSetChanged();
        messagesListView.setSelection(adapter.getCount() - 1);


        JSONObject data = new JSONObject();
        try
        {
            data.put("text", encryptedMessage);
            data.put("iv", ivString);
            data.put("sender", sender);
            data.put("recipient", recipient);
            socket.emit("message", data);
        }
        catch (JSONException e){
            Utils.Log("ERROR: ChatActivity.SendMessage(): JSONException: " + e.getMessage());
        }

    }

    private void sendMessage(String _message, String _password){
        Utils.Log("-----");
        String message = _message;
        Utils.Log("sendMessage(~).message: " + message);
        //If the message is empty, return
        if(message.isEmpty()){return;}
       // messageText.setText("");
        String encryptedMessage = "";
        //Generate a random input vector
        byte[] IV = Crypto.GenerateRandomIV();
        //Convert IV into base 64 encoded string
        String ivString = Base64.encodeToString(IV, Base64.NO_WRAP);
        Utils.Log("sendMessage(~).ivString: " + ivString);
        //Encrypt the message
        encryptedMessage = Crypto.AESencrypt(_password, message, IV);
        Utils.Log("sendMessage(~).encryptedMessage: " + encryptedMessage);


        //Update the listView
        if(!message.equals(Utils.AUTHENTICATION_MESSAGE)){
            messages.add(new Message(message, "", sender));
        }

        //adapter.notifyDataSetChanged();


        JSONObject data = new JSONObject();
        try
        {
            data.put("text", encryptedMessage);
            data.put("iv", ivString);
            data.put("sender", sender);
            data.put("recipient", recipient);
            if(_message.equals(Utils.AUTHENTICATION_MESSAGE)){
                data.put("first", true);
            }
            socket.emit("message", data);
        }
        catch (JSONException e){
            Utils.Log("ERROR: ChatActivity.SendMessage(~): JSONException: " + e.getMessage());
        }

    }

    private Emitter.Listener handleAuthorizationMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Utils.Log("-----");
            JSONObject data = (JSONObject)args[0];

            try{
                AuthMessage = data.getString("text");
                Utils.Log("ChatActivity.handleAuthorizationMessage.AuthMessage: " + AuthMessage);

                AuthIV = Base64.decode(data.getString("iv"), Base64.NO_WRAP);
                Utils.Log("ChatActivity.handleAuthorizationMessage.ivString: " + data.getString("iv"));

            } catch (JSONException e) {
                Utils.Log("ERROR: ChatActivity.handleAuthorizationMessage: JSONException: " + e.getMessage());
                e.printStackTrace();
            }
        }
    };

    //Event handler for incoming messages
    private Emitter.Listener handleIncomingMessages = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.Log("-----");
                    JSONObject data = (JSONObject) args[0];
                    String message;
                    byte[] IV;

                    try{
                        if (!(data.getString("sender").equals(recipient) || data.getString("sender").equals(sender))) {
                            return;
                        }
                        message = data.getString("text").toString();
                        Utils.Log("ChatActivity.handleIncomingMessages.message: " + message);

                        String decryptedMessage = "_empty_";
                        try
                        {
                            Utils.Log("ChatActivity.handleIncomingMessages.ivString: " + data.getString("iv"));
                            IV = Base64.decode(data.getString("iv"), Base64.NO_WRAP);
                            decryptedMessage = Crypto.AESdecrypt(password, message, IV);
                            Utils.Log("ChatActivity.handleIncomingMessages.decryptedMessage: " + decryptedMessage);
                        }
                        finally {

                        }
                        //Handle the listView
                        messages.add(new Message(decryptedMessage, "7/22/2015 1:00AM", data.getString("sender")));
                        adapter.notifyDataSetChanged();
                        messagesListView.setSelection(adapter.getCount() - 1);

                    }
                    catch (JSONException e)
                    {
                        Utils.Log("ERROR: ChatActivity.handleIncomingMessage(): JSONException: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

        }
    };

    public class MessagesArrayAdapter extends ArrayAdapter<Message> {
        public MessagesArrayAdapter(){
            super(ChatActivity.this, R.layout.layout_conversation, messages);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View messageView = convertView;


            Message currentMessage = messages.get(position);


            if(currentMessage.getSenderid().equals(sender)){
                messageView = getLayoutInflater().inflate(R.layout.layout_message_sent, parent, false);
            }
            else{
                messageView = getLayoutInflater().inflate(R.layout.layout_message_recieved, parent, false);
            }

            TextView messageText = (TextView)messageView.findViewById(R.id.message_text);
            TextView messageDate = (TextView)messageView.findViewById(R.id.message_date);

            messageText.setText(messages.get(position).getMessage());
            //messageDate.setText(calendar.getTime().toString());

            return messageView;
        }
    }

}

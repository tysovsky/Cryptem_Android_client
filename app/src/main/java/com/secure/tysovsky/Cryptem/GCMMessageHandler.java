package com.secure.tysovsky.Cryptem;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

/**
 * Created by tysovsky on 9/23/15.
 */
public class GCMMessageHandler extends GcmListenerService {
    public static final int MESSAGE_NOTIFICATION_ID = 18263;
    DBManager dbManager = new DBManager(this);
    public static Activity currentActivity;
    public static final Object CURRENTACTIVITYLOCK = new Object();

    SharedPreferences preferences;


    @Override
    public void onMessageReceived(String from, final Bundle data) {

        preferences = getSharedPreferences("CryptemSP", MODE_PRIVATE);

        Utils.Log("Push message received");

        final String type = data.getString("type", "none");

        switch (type){
            //region Message Recieved
            case Utils.TYPE_MESSAGE:
                //Get new messages
                new HttpHandler() {

                    @Override
                    public HttpUriRequest getHttpRequestMethod() {
                        HttpPost httpPost = new HttpPost(Utils.SERVER_GET_MESSAGES);

                        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                        nameValuePairs.add(new BasicNameValuePair("username", dbManager.getUsername()));

                        try {
                            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        return httpPost;
                    }

                    @Override
                    public void onResponse(String result) {

                        ArrayList<Message> messages = parseMessages(result);

                        for (int i = 0; i < messages.size(); i++) {
                            try {
                                PublicKey key = Crypto.RSAStrigToPublicKey(dbManager.getRSAKeySign(messages.get(i).getSender()));

                                byte[] signature = Base64.decode(messages.get(i).getSignature(), Base64.NO_WRAP);
                                byte[] data = Base64.decode(messages.get(i).getMessage(), Base64.NO_WRAP);

                                //Verify signature
                                if(Crypto.RSAVerify(data, signature, key)){
                                    Utils.Log("Signature verification successful");
                                    String plainText = Crypto.AESdecrypt(dbManager.getAESKey(messages.get(i).getSender()),
                                            messages.get(i).getMessage(),
                                            Base64.decode(messages.get(i).getIv(), Base64.NO_WRAP));

                                    messages.get(i).setMessage(plainText);
                                    messages.get(i).setEncrypted(false);

                                    dbManager.addMessage(messages.get(i));
                                    if (dbManager.getConversation(messages.get(i).getSender()) == null) {
                                        //Insert new conversation
                                        dbManager.addConversation(new Conversation(messages.get(i).getSender(), 0));
                                    }

                                    dbManager.incrementUnreadMessages(messages.get(i).getSender());
                                }

                                else{
                                    Utils.Log("Signature verification failed");
                                    messages.remove(i);
                                }

                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            } catch (InvalidKeySpecException e) {
                                e.printStackTrace();
                            } catch (NoSuchProviderException e) {
                                e.printStackTrace();
                            } catch (InvalidKeyException e) {
                                e.printStackTrace();
                            } catch (SignatureException e) {
                                e.printStackTrace();
                            }

                        }

                        //If the activity is open
                        if (currentActivity != null) {
                            if (currentActivity.getClass() == CryptemActivity.class) {
                                CryptemActivity activity = (CryptemActivity) currentActivity;

                                //If message fragment is currently shown
                                if (activity.messagesFragment != null && activity.messagesFragment.currentlyOpen) {
                                    String openConversation = activity.messagesFragment.conversation.getUsername();

                                    for (int i = 0; i < messages.size(); i++) {

                                        //If the right conversation is open, update messages
                                        if (messages.get(i).getSender().equals(openConversation)) {
                                            activity.messagesFragment.messages.add(messages.get(i));
                                            activity.messagesFragment.notifyAdapter();
                                        }
                                        //Else raise notification
                                        else {
                                            createNotification(messages.get(i).getSender(), messages.get(i).getMessage(), messages.get(i).getIv());
                                        }
                                    }

                                }

                                //If conversations fragment is currently shown
                                else if (activity.conversationsFragment != null && activity.conversationsFragment.currentlyOpen) {
                                    //update conversations
                                    activity.conversationsFragment.updateConversations();

                                    //Raise notification
                                    for (int i = 0; i < messages.size(); i++) {
                                        //createNotification(messages.get(i).getSender(), messages.get(i).getMessage(), messages.get(i).getIv());
                                    }
                                }
                                else {
                                    for (int i = 0; i < messages.size(); i++) {
                                        createNotification(messages.get(i).getSender(), messages.get(i).getMessage(), messages.get(i).getIv());
                                    }
                                }
                            }
                        }
                        //Else raise notification
                        else {
                            for (int i = 0; i < messages.size(); i++) {
                                createNotification(messages.get(i).getSender(), messages.get(i).getMessage(), messages.get(i).getIv());
                            }
                        }

                    }
                }.execute();
                break;
            //endregion

            //region DHKE Initiated
            case Utils.TYPE_DHKEINITIATED:

                final String sender = data.getString("sender");
                final String recipient = data.getString("recipient");



                //Retrieve a prime and a generator from the server
                new HttpHandler(){
                    @Override
                    public HttpUriRequest getHttpRequestMethod() {

                        HttpPost httpPost = new HttpPost(Utils.SERVER_DHKEGETDATA);

                        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

                        nameValuePairs.add(new BasicNameValuePair("sender", sender));
                        nameValuePairs.add(new BasicNameValuePair("recipient", recipient));

                        try {
                            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }


                        return httpPost;
                    }

                    @Override
                    public void onResponse(String result) {

                        Utils.Log(result);

                        //Generate public key
                        try {

                            JSONObject rdata = new JSONObject(result);
                            String prime = rdata.getString("prime");
                            String senderPublicKey = rdata.getString("publickey");
                            String RSAKeySign = rdata.getString("rsakeysign");
                            String RSAKeyEnc = rdata.getString("rsakeyenc");

                            //Store recipient's keys in the database
                            dbManager.insertRSAKeyEnc(sender, RSAKeyEnc);
                            dbManager.insertRSAKeySign(sender, RSAKeySign);

                            byte[] privateKey = Crypto.DHGeneratePrivateKey();

                            //Calculate shared secret
                            byte[] sharedSecret = Crypto.DHGenerateSharedPrivateKey(
                                    Base64.decode(prime, Base64.NO_WRAP),
                                    Base64.decode(senderPublicKey, Base64.NO_WRAP),
                                    privateKey);

                            //Store shared secret
                            dbManager.insertKey(sender, Base64.encodeToString(sharedSecret, Base64.NO_WRAP));
                            Utils.Log("Shared secret: " + Base64.encodeToString(sharedSecret, Base64.NO_WRAP));

                            //Calculate recipient's public key
                            final byte[] publicKey = Crypto.DHGeneratePublicKey(
                                    Base64.decode(prime, Base64.NO_WRAP),
                                    privateKey);

                            //Send public key
                            new HttpHandler(){
                                @Override
                                public HttpUriRequest getHttpRequestMethod() {
                                    HttpPost httpPost = new HttpPost(Utils.SERVER_INSERTRECIPIENTKEY);

                                    ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                                    nameValuePairs.add(new BasicNameValuePair("sender", sender));
                                    nameValuePairs.add(new BasicNameValuePair("recipient", recipient));
                                    nameValuePairs.add(new BasicNameValuePair("publickey", Base64.encodeToString(publicKey, Base64.NO_WRAP)));

                                    try {
                                        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }

                                    return httpPost;
                                }

                                @Override
                                public void onResponse(String result) {
                                    Utils.Log(result);
                                }
                            }.execute();


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }.execute();

                break;
            //endregion

            //region DHKE Recipient Key Available
            case Utils.TYPE_DHKEKEYAVAILABLE:

                Utils.Log("DHKE Key available");
                //Retrieve other user's public key
                new HttpHandler(){
                    @Override
                    public HttpUriRequest getHttpRequestMethod() {
                        HttpPost httpPost = new HttpPost(Utils.SERVER_GETRECIPIENTKEY);

                        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                        nameValuePairs.add(new BasicNameValuePair("sender", data.getString("sender")));
                        nameValuePairs.add(new BasicNameValuePair("recipient", data.getString("recipient")));

                        try {
                            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        return httpPost;
                    }

                    @Override
                    public void onResponse(String result) {
                        Utils.Log("Public key recieved");
                        try {

                            JSONObject data = new JSONObject(result);
                            String publicKey = data.getString("publickey");
                            String prime = data.getString("prime");

                            Utils.Log("Result: " + result);

                            String myPrivateKey = dbManager.getDHKEPrivateKey(data.getString("recipient"));

                            Utils.Log("Private key: " + myPrivateKey);

                            byte[] sharedSecret = Crypto.DHGenerateSharedPrivateKey(
                                    Base64.decode(prime, Base64.NO_WRAP),
                                    Base64.decode(publicKey, Base64.NO_WRAP),
                                    Base64.decode(myPrivateKey, Base64.NO_WRAP));

                            Utils.Log("Shared secret: " + Base64.encodeToString(sharedSecret, Base64.NO_WRAP));
                            dbManager.insertKey(data.getString("recipient"), Base64.encodeToString(sharedSecret, Base64.NO_WRAP));

                            if(currentActivity != null){
                                Toast.makeText(currentActivity, "Key Exchange Complete", Toast.LENGTH_LONG).show();
                            }

                            dbManager.deleteDHKEEntry(data.getString("recipient"));

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Utils.Log("Json Exception: " + e.getMessage());
                        }

                    }
                }.execute();

                break;
            //endregion

            //Not supposed to happen
            default:
                Utils.Log("Bit of a fuck up here");
                break;
        }

    }

    private void createNotification(String from, String plainText, String iv) {

        Context context = getBaseContext();
        NotificationCompat.Builder mBuilder = null;
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);


        if(preferences.getBoolean("decrypt_notification", true)){
            String key = dbManager.getAESKey(from);

            if(key == null){return;}



            mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_message).setContentTitle(from)
                    .setContentText(plainText)
                    .setAutoCancel(true);
        }

        else{
            mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_message).setContentTitle("New Message Received")
                    .setContentText(from)
                    .setAutoCancel(true);
        }

        Intent resultIntent = new Intent(context, CryptemActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);


        mNotificationManager.notify(MESSAGE_NOTIFICATION_ID, mBuilder.build());


    }

    private ArrayList<Message> parseMessages(String result) {
        Utils.Log("Starting parsing");
        ArrayList<Message> messages = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(result);

            for (int i = 0; i < array.length(); i++) {
                JSONObject msg = array.getJSONObject(i);

                Utils.Log(array.getJSONObject(i).toString());

                Message message = new Message();
                message.setMessage(msg.getString("message"));
                message.setSender(msg.getString("sender"));
                message.setRecipient(msg.getString("recipient"));
                message.setIv(msg.getString("iv"));
                message.setSignature(msg.getString("signature"));


                messages.add(message);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return messages;
    }
}

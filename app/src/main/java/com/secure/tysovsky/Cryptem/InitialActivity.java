package com.secure.tysovsky.Cryptem;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.secure.tysovsky.Cryptem.R;
import com.squareup.okhttp.internal.Util;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class InitialActivity extends AppCompatActivity {

    private com.github.nkzawa.socketio.client.Socket socket;
    private IO.Options socketOptions = new IO.Options();

    EditText ETUsername;
    TextView TVUsername_status;
    ProgressWheel wheel;
    ProgressWheel usernameCheckWheel;
    Button generateKeys;
    Button buttonTest;
    String username = "";
    DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);


        dbManager = new DBManager(this);

        ETUsername = (EditText)findViewById(R.id.username_check);
        TVUsername_status = (TextView)findViewById(R.id.username_status);
        wheel = (ProgressWheel)findViewById(R.id.progress_wheel);
        wheel.stopSpinning();
        usernameCheckWheel = (ProgressWheel)findViewById(R.id.username_check_progress);
        usernameCheckWheel.stopSpinning();
        generateKeys = (Button)findViewById(R.id.generate_keys);
        generateKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                username = ETUsername.getText().toString();
                new GenerateKeysTask().execute();
            }
        });

        buttonTest = (Button)findViewById(R.id.button_test);
        buttonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                socket.disconnect();
                Intent i = new Intent(InitialActivity.this, CryptemActivity.class);
                startActivity(i);
            }
        });



        try {
            socketOptions.reconnection = true;
            socket = IO.socket(Utils.SERVER_ADRESS, socketOptions);
            socket.on("username_status", HandleUsernameStatus);
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        ETUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                usernameCheckWheel.spin();
                if(ETUsername.getText().length() != 0){
                    JSONObject data = new JSONObject();
                    try {
                        data.put("username", ETUsername.getText().toString());
                        socket.emit("usernameCheck", data);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else{

                }
            }
        });


    }

    private Emitter.Listener HandleUsernameStatus = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject)args[0];
            try {
                int status = data.getInt("status");
                Utils.Log("Status recieved: " + status);
                if(status == Utils.SUCESS_CODE){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TVUsername_status.setText("This username is not taken");
                            username = ETUsername.getText().toString();
                            usernameCheckWheel.stopSpinning();
                        }
                    });
                }
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TVUsername_status.setText("This username is taken");
                            usernameCheckWheel.stopSpinning();
                        }
                    });

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };


    private class GenerateKeysTask extends AsyncTask<String, Void, Boolean>{

        @Override
        protected void onPreExecute(){
            wheel.spin();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            try {


                //Will be used for digital signatures
                KeyPair RSAKeysSignature = Crypto.RSAGenerateKeyPair();

                //Will be used for receiving encrypted messages from server
                KeyPair RSAKeysEncryption = Crypto.RSAGenerateKeyPair();

                String RSASignaturePrivate = Base64.encodeToString(RSAKeysSignature.getPrivate().getEncoded(), Base64.NO_WRAP);
                final String RSASignaturePublic = Crypto.RSAtoPemString(RSAKeysSignature.getPublic());

                String RSAEncryptionPrivate = Base64.encodeToString(RSAKeysEncryption.getPrivate().getEncoded(), Base64.NO_WRAP);
                final String RSAEncryptionPublic = Crypto.RSAtoPemString(RSAKeysEncryption.getPublic());


                dbManager.addUserDetails(username, RSAEncryptionPublic, RSAEncryptionPrivate, RSASignaturePublic, RSASignaturePrivate);

                //Send HTTP POST request to register
                new HttpHandler(){

                    @Override
                    public HttpUriRequest getHttpRequestMethod() {

                        HttpPost httpPost = new HttpPost(Utils.SERVER_REGISTER);

                        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

                        nameValuePairs.add(new BasicNameValuePair("username", username));
                        nameValuePairs.add(new BasicNameValuePair("rsakeysignature", RSASignaturePublic));
                        nameValuePairs.add(new BasicNameValuePair("rsakeypublic", RSAEncryptionPublic));

                        try{
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


            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean success){
            wheel.stopSpinning();
        }
    }

}

package com.secure.tysovsky.cryptomessanger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.andexert.expandablelayout.library.ExpandableLayout;
import com.squareup.okhttp.internal.Util;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpProcessor;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends ActionBarActivity {

    ExpandableLayout loginPanel;
    ExpandableLayout registerPanel;

    Button btnLogin;
    Button btnRegister;
    EditText lUsername;
    EditText lPassword;
    CheckBox lRememberMe;
    EditText rUsername;
    EditText rPassword;
    EditText rConfirmPassword;
    CheckBox rRememberMe;

    String username = "";
    String password = "";


    final HttpClient httpClient = new DefaultHttpClient();
    HttpPost httpPost = null;

    final String loginURL = "http://ec2-52-27-83-251.us-west-2.compute.amazonaws.com:80/login";
    final String registerURL = "http://ec2-52-27-83-251.us-west-2.compute.amazonaws.com:80/register";

    final List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Utils.Log("----APPLICATION STARTED----");
        setTitle("Welcome");

        SharedPreferences prefs = getSharedPreferences("CryptoSP", MODE_PRIVATE);
        String prefUsername = prefs.getString("username", null);
        if(prefUsername != null){
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("username", prefUsername);
            startActivity(intent);
        }


        loginPanel = (ExpandableLayout)findViewById(R.id.login_panel);
        registerPanel = (ExpandableLayout)findViewById(R.id.register_panel);

        btnLogin = (Button)findViewById(R.id.btn_login);
        btnRegister = (Button)findViewById(R.id.btn_register);
        rUsername = (EditText)findViewById(R.id.register_username);
        rPassword = (EditText)findViewById(R.id.register_password);
        rConfirmPassword = (EditText)findViewById(R.id.register_confirm_password);
        rRememberMe = (CheckBox)findViewById(R.id.register_remember_me);
        lUsername = (EditText)findViewById(R.id.login_username);
        lPassword = (EditText)findViewById(R.id.login_password);
        lRememberMe = (CheckBox)findViewById(R.id.login_remember_me);

        lRememberMe.setChecked(true);
        rRememberMe.setChecked(true);

        loginPanel.show();

        loginPanel.getHeaderRelativeLayout().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginPanel.isOpened()) {
                    loginPanel.hide();
                    registerPanel.show();
                } else {
                    loginPanel.show();
                    registerPanel.hide();
                }
            }
        });

        registerPanel.getHeaderRelativeLayout().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(registerPanel.isOpened()){
                    registerPanel.hide();
                    loginPanel.show();
                }
                else{
                    registerPanel.show();
                    loginPanel.hide();
                }
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = lUsername.getText().toString();
                password = lPassword.getText().toString();
                new LoginTask().execute();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = rUsername.getText().toString();
                password = "";
                if(rPassword.getText().toString().equals(rConfirmPassword.getText().toString())){
                    password = rPassword.getText().toString();
                    new RegisterTask().execute("");

                }
            }
        });


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finishAffinity();
    }

    private class LoginTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            Utils.Log("-----");
            Utils.Log("LoginActivity.LoginTask.username: " + username);
            Utils.Log("LoginActivity.LoginTask.password: " + password);
            httpPost = new HttpPost(loginURL);
            nameValuePair.clear();
            nameValuePair.add(new BasicNameValuePair("username", username));
            nameValuePair.add(new BasicNameValuePair("password", password));
            HttpResponse response = null;


            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
            } catch (UnsupportedEncodingException e) {
                // log exception
                e.printStackTrace();
            }

            //making a POST request.
            try {
                response = httpClient.execute(httpPost);

                if(response == null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "Server Error", Toast.LENGTH_LONG).show();
                        }
                    });

                    return null;
                }
                String result = Utils.getResponseBody(response);
                Utils.Log("LoginActivity.LoginTask.result: " + result);
                JSONObject loginToken = new JSONObject(result);
                if(loginToken.getString("status").equals(Utils.RESPONSE_LOGIN_SUCCESS)){
                    if(lRememberMe.isChecked()){
                        SharedPreferences.Editor preferencesEditor = getSharedPreferences("CryptoSP", MODE_PRIVATE).edit();
                        preferencesEditor.putString("username", username);
                        preferencesEditor.apply();
                    }
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("username", loginToken.getString("username"));
                    startActivity(intent);
                }else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (ClientProtocolException e) {
                // Log exception
                e.printStackTrace();
            } catch (IOException e) {
                // Log exception
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return Utils.getResponseBody(response);
        }

        @Override
        protected void onPostExecute(String result){

        }

    }

    private class RegisterTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            Utils.Log("-----");
            Utils.Log("LoginActivity.RegisterTask.username: " + username);
            Utils.Log("LoginActivity.RegisterTask.password: " + password);
            httpPost = new HttpPost(registerURL);
            nameValuePair.add(new BasicNameValuePair("username", username));
            nameValuePair.add(new BasicNameValuePair("password", password));


            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
            } catch (UnsupportedEncodingException e) {
                // log exception
                e.printStackTrace();
            }

            //making POST request.
            try {
                HttpResponse response = httpClient.execute(httpPost);

                if(response == null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "Server Error", Toast.LENGTH_LONG).show();
                        }
                    });

                    return null;
                }

                String result = Utils.getResponseBody(response);
                Utils.Log("LoginActivity.RegisterTask.result: " + result);

                if(result.equals("sucess")){
                    if(rRememberMe.isChecked()){
                        SharedPreferences.Editor preferencesEditor = getSharedPreferences("CryptoSP", MODE_PRIVATE).edit();
                        preferencesEditor.putString("username", username);
                        preferencesEditor.apply();
                    }
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                }
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "Error", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (ClientProtocolException e) {
                // Log exception
                e.printStackTrace();
            } catch (IOException e) {
                // Log exception
                e.printStackTrace();
            }

            return "Done";
        }

        @Override
        protected void onPostExecute(String result){

        }

    }
}

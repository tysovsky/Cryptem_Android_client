package com.secure.tysovsky.Cryptem;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.secure.tysovsky.Cryptem.Interfaces.OnActionBarEventListener;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class CryptemActivity extends AppCompatActivity
        implements Interfaces.OnConversationClickedListener, OnActionBarEventListener {

    ConversationsFragment conversationsFragment = new ConversationsFragment();
    public MessagesFragment messagesFragment = new MessagesFragment();

    private GCMClientManager gcmManager;
    private String PROJECT_NUMBER = "644026005539";

    private String username = null;

    private DBManager dbManager;

    private ArrayList<Conversation> conversations = null;


    @Override
    protected void onResume() {
        Utils.Log("Activity onResume called");
        synchronized (GCMMessageHandler.CURRENTACTIVITYLOCK){
            GCMMessageHandler.currentActivity = this;
        }
        super.onResume();



    }

    @Override
    protected void onPause() {
        Utils.Log("Activity onPause called");
        synchronized (GCMMessageHandler.CURRENTACTIVITYLOCK){
            GCMMessageHandler.currentActivity = null;
        }
        super.onPause();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.Log("Activity onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cryptem);

        dbManager = new DBManager(this);


        if(dbManager.getUsername() == null || dbManager.getUsername().isEmpty()){
            Intent i = new Intent(CryptemActivity.this, InitialActivity.class);
            startActivity(i);

            finish();
        }
        else{
            username = dbManager.getUsername();
        }


        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.primary)));


        if(savedInstanceState == null){
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.conversations_fragment, conversationsFragment, ConversationsFragment.TAG);
            transaction.commit();

        }
        else{
            if(getFragmentManager().findFragmentByTag(ConversationsFragment.TAG) != null){
                conversationsFragment = (ConversationsFragment)getFragmentManager().findFragmentByTag(ConversationsFragment.TAG);
            }

            if(getFragmentManager().findFragmentByTag(MessagesFragment.TAG) != null){
                messagesFragment = (MessagesFragment)getFragmentManager().findFragmentByTag(MessagesFragment.TAG);
            }

        }

        gcmManager = new GCMClientManager(this, PROJECT_NUMBER);
        gcmManager.registerIfNeeded(new GCMClientManager.RegistrationCompletedHandler() {
            @Override
            public void onSuccess(final String registrationId, boolean isNewRegistration) {
                new HttpHandler() {

                    @Override
                    public HttpUriRequest getHttpRequestMethod() {
                        HttpPost httpPost = new HttpPost(Utils.SERVER_UPDATE_GCMID);

                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                        nameValuePairs.add(new BasicNameValuePair("GCMID", registrationId));
                        nameValuePairs.add(new BasicNameValuePair("username", username));

                        try {
                            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        return httpPost;
                    }

                    @Override
                    public void onResponse(String result) {

                    }
                }.execute();
            }

            @Override
            public void onFailure(String ex) {
                super.onFailure(ex);
            }
        });

        conversationsFragment.setOnConversationClickedListener(this);
        conversationsFragment.setOnActionBarChangeRequestListener(this);
        messagesFragment.setOnActionBarChangeRequestListener(this);



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cryptem, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.home:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                }
                Utils.Log("Home button clicked");
                return true;
            case R.id.action_settings:
                Utils.Log("Settings button clicked");

                Intent launchSettings = new Intent(CryptemActivity.this, SettingsActivity.class);
                launchSettings.putExtra("username", dbManager.getUsername());
                startActivity(launchSettings);

                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConversationClicked(Conversation conversation) {
        //Utils.Log("Conversation pressed. Username: " + conversation.getUsername() );

        messagesFragment.setConversation(conversation);
        //messagesFragment.setMessages(dbManager.getMessages(conversation.getUsername()));

        FragmentManager manager = getFragmentManager();
        FragmentTransaction fragmentTransaction = manager.beginTransaction();
        fragmentTransaction.replace(R.id.conversations_fragment, messagesFragment, MessagesFragment.TAG);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    //Stupid bug in appcompat library
    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void OnActionBarChangeRequest(int type, String parameter) {
        switch (type){
            case Utils.ACTION_BAR_NAME_CHANGE:
                this.setTitle(parameter);
                break;
            case Utils.ACTION_BAR_ENABLE_BACK_BUTTON:
                this.getSupportActionBar().setHomeButtonEnabled(true);
                this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                break;
            case Utils.ACTION_BAR_DISABLE_BACK_BUTTON:
                this.getSupportActionBar().setHomeButtonEnabled(true);
                this.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                break;
        }
    }
}

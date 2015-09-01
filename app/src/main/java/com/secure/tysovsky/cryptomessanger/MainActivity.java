package com.secure.tysovsky.cryptomessanger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private ListView conversationListView;
    FloatingActionButton btnNewConversation;
    private List<Conversation> conversations = new ArrayList<Conversation>();
    private static String username = "null";
    EditText ETRecipientUsername;
    EditText ETConversationPassword;
    EditText ETNewMessage;

    ArrayAdapter<Conversation> adapter= null;


    final HttpClient httpClient = new DefaultHttpClient();
    HttpPost httpPost = null;
    String retrieveUrl = "http://ec2-52-27-83-251.us-west-2.compute.amazonaws.com/getConversations";



    final List<NameValuePair> nameValuePair = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            username = extras.getString("username");
        }


        conversationListView = (ListView)findViewById(R.id.conversations_list);
        btnNewConversation = (FloatingActionButton)findViewById(R.id.btn_new_conversation);
        adapter = new ConversationsArrayAdapter();
        conversationListView.setAdapter(adapter);

        btnNewConversation.attachToListView(conversationListView);
        btnNewConversation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDialog startConversationDialog = new MaterialDialog.Builder(MainActivity.this)
                        .title("New Conversation")
                        .customView(R.layout.layout_new_conversation, true)
                        .positiveText("Send").negativeText("Cancel")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                                String _recipient = ETRecipientUsername.getText().toString();
                                String _password = ETConversationPassword.getText().toString();
                                String _message = ETNewMessage.getText().toString();
                                if (_recipient.isEmpty() || _message.isEmpty() || _password.isEmpty()) {
                                    Toast.makeText(MainActivity.this, "Please fill in all the fields", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                intent.putExtra("recipient", _recipient);
                                intent.putExtra("message", _message);
                                intent.putExtra("sender", username);
                                intent.putExtra("password", _password);
                                startActivity(intent);
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                super.onNegative(dialog);
                            }
                        })
                        .build();
                ETRecipientUsername = (EditText) startConversationDialog.getCustomView().findViewById(R.id.new_conversation_recipient);
                ETConversationPassword = (EditText) startConversationDialog.getCustomView().findViewById(R.id.new_conversation_password);
                ETNewMessage = (EditText) startConversationDialog.getCustomView().findViewById(R.id.new_conversation_message);

                startConversationDialog.show();

            }
        });

        conversationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("recipient", conversations.get(i).getUsername());
                intent.putExtra("sender", username);
                startActivity(intent);
            }
        });

        SharedPreferences prefs = getSharedPreferences("CryptoSP", MODE_PRIVATE);
        String prefUsername = prefs.getString("username", null);
        if(prefUsername != null){
            Intent serviceIntent = new Intent(this, BackgroundService.class);
            //serviceIntent.putExtra("username", username);
            startService(serviceIntent);
        }



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id){
            case R.id.menu_settings:
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_LONG).show();
                break;
            case R.id.menu_logout:
                Toast.makeText(this, "Logout Pressed", Toast.LENGTH_LONG).show();
                SharedPreferences preferences = getSharedPreferences("CryptoSP", MODE_PRIVATE);
                String prefUsername = preferences.getString("username", null);
                if(prefUsername != null){
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.remove("username");
                    editor.apply();
                }
                Intent serviceIntent = new Intent(MainActivity.this, BackgroundService.class);
                stopService(serviceIntent);
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        conversations.clear();
        new RetrieveMessageTask().execute();
    }

    private class RetrieveMessageTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            Utils.Log("-----");
            httpPost = new HttpPost(retrieveUrl);
            nameValuePair.clear();
            nameValuePair.add(new BasicNameValuePair("username", username));
            HttpResponse response = null;

            try{
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
                response = httpClient.execute(httpPost);
                String result = Utils.getResponseBody(response);
                Utils.Log("MainActivity.RetrieveMessageTask.result: " + result);


                ArrayList<String> _conversations = Utils.parseConversationsList(result);


                for(int i = 0; i < _conversations.size(); i++){
                    conversations.add(new Conversation(_conversations.get(i), 0));
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
    public class ConversationsArrayAdapter extends ArrayAdapter<Conversation> {
        public ConversationsArrayAdapter(){
            super(MainActivity.this, R.layout.layout_conversation, conversations);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View conversationView = convertView;
            if(conversationView == null){
                conversationView = getLayoutInflater().inflate(R.layout.layout_conversation, parent, false);
            }

            Conversation currentConversation = conversations.get(position);

            ImageView profileImage = (ImageView)conversationView.findViewById(R.id.chat_profilePicture);
            profileImage.setTag(new Integer(position));
            profileImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Toast.makeText(MainActivity.this, "You've clicked an image in item#" + v.getTag().toString(), Toast.LENGTH_LONG).show();
                }
            });

            TextView usernameView = (TextView)conversationView.findViewById(R.id.chat_username);
            TextView messagesView = (TextView)conversationView.findViewById(R.id.chat_newMessages);

            usernameView.setText(currentConversation.getUsername());
            //messagesView.setText(currentConversation.getUnreadMessagesString());
            messagesView.setText("~ new messages");

            return conversationView;
        }
    }



}

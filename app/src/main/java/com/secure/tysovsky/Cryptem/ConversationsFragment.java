package com.secure.tysovsky.Cryptem;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import com.squareup.okhttp.internal.Util;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;


/**
 * Created by tysovsky on 9/22/15.
 */
public class ConversationsFragment extends Fragment {
    private Interfaces.OnConversationClickedListener conversationClickedListener;
    private Interfaces.OnActionBarEventListener actionBarChangeRequestListener;

    public static String TAG = "ConversationFragment";
    public static boolean currentlyOpen = false;

    ListView conversationsList;

    public ArrayList<Conversation> conversations = new ArrayList<>();
    ArrayAdapter<Conversation> adapter;

    FloatingActionButton newConversation;

    EditText newConversationRecipient;

    DBManager dbManager;


    @Override
    public void onAttach(Activity activity) {
        Utils.Log("ConversationFragment onAttach called");
        super.onAttach(activity);
        //if(activity instanceof )
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.Log("ConversationFragment onCreate called");
        super.onCreate(savedInstanceState);
        dbManager = new DBManager(getActivity());

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Utils.Log("ConversationFragment onCreateView called");
        View view = inflater.inflate(R.layout.fragment_conversations, container, false);

        conversationsList = (ListView)view.findViewById(R.id.conversations_list);


        adapter = new ConversationsAdapter(getActivity(), conversations);


        conversationsList.setAdapter(adapter);
        conversationsList.setOnItemLongClickListener(onItemLongClickListener);

        newConversation = (FloatingActionButton)view.findViewById(R.id.btn_new_conversation);
        newConversation.setOnClickListener(newConversationOnClickListener);


        return view;
    }

    @Override
    public void onStart() {
        Utils.Log("ConversationFragment onStart called");
        super.onStart();
        conversationsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Utils.Log("conversations size: " + conversations.size());
                conversationClickedListener.onConversationClicked(conversations.get(i));
            }
        });
    }

    @Override
    public void onResume() {
        Utils.Log("ConversationFragment onResume called");
        super.onResume();

        currentlyOpen = true;

        updateConversations();

        if(actionBarChangeRequestListener != null){
            actionBarChangeRequestListener.OnActionBarChangeRequest(Utils.ACTION_BAR_DISABLE_BACK_BUTTON, null);
            actionBarChangeRequestListener.OnActionBarChangeRequest(Utils.ACTION_BAR_NAME_CHANGE, "Cryptem");
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        currentlyOpen = false;
    }

    //region newConversationOnClickListener
    private View.OnClickListener newConversationOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title("New conversation")
                    .titleColor(getResources().getColor(R.color.lightest))
                    .customView(R.layout.layout_new_conversation, true)
                    .positiveText(R.string.ok)
                    .positiveColor(getResources().getColor(R.color.lightest))
                    .negativeColor(getResources().getColor(R.color.lightest))
                    .negativeText(R.string.cancel)
                    .backgroundColor(getResources().getColor(R.color.primary))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {



                            super.onPositive(dialog);

                            final String recipient = newConversationRecipient.getText().toString().trim();
                            final String username = dbManager.getUsername();

                            //Don't let a user start a conversation with himself
                            if(!recipient.equals(username) && !recipient.isEmpty()){
                                if(dbManager.getConversation(recipient) != null){
                                    conversationClickedListener.onConversationClicked(dbManager.getConversation(recipient));
                                }

                                else if(!dbManager.getKey(recipient).isEmpty()){
                                    dbManager.addConversation(new Conversation(recipient, 0));
                                }

                                else{
                                    Conversation conversation = new Conversation();
                                    conversation.setUsername(recipient);

                                    //Initiate key exchange
                                    new HttpHandler() {
                                        @Override
                                        public HttpUriRequest getHttpRequestMethod() {

                                            HttpPost httpPost = new HttpPost(Utils.SERVER_DHKEINITIATE);

                                            String prime = Base64.encodeToString(Crypto.getPrime(512).toByteArray(), Base64.NO_WRAP);

                                            //Generate private key
                                            byte[] privateKey = Crypto.DHGeneratePrivateKey();

                                            //Calculate public key
                                            final byte[] publicKey = Crypto.DHGeneratePublicKey(
                                                    Base64.decode(prime, Base64.NO_WRAP), privateKey);

                                            dbManager.InsertDHKEStep1(recipient, prime, "2");
                                            //Store private key
                                            dbManager.InsertDHKEPrivateKey(recipient, Base64.encodeToString(privateKey, Base64.NO_WRAP));

                                            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

                                            nameValuePairs.add(new BasicNameValuePair("sender", username));
                                            nameValuePairs.add(new BasicNameValuePair("recipient", recipient));
                                            nameValuePairs.add(new BasicNameValuePair("prime", prime));
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

                                    dbManager.addConversation(new Conversation(recipient, 0));

                                    conversationClickedListener.onConversationClicked(conversation);

                                    updateConversations();
                                }

                            }



                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            super.onNegative(dialog);
                        }
                    }).build();

            newConversationRecipient = (EditText)dialog.getCustomView().findViewById(R.id.new_conversation_recipient);

            dialog.show();

        }
    };
    //endregion

    private AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {

            MaterialDialog deleteDialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.options)
                    .titleColor(getResources().getColor(R.color.lightest))
                    .backgroundColor(getResources().getColor(R.color.primary))
                    .items(R.array.conversation_options)
                    .itemColor(getResources().getColor(R.color.lightest))
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, View _view, int which, CharSequence charSequence) {
                            switch (which) {

                                //Set password
                                case 0:
                                    Toast.makeText(getActivity(), R.string.not_implemented, Toast.LENGTH_LONG).show();
                                    break;

                                //Summary
                                case 1:
                                    Toast.makeText(getActivity(), R.string.not_implemented, Toast.LENGTH_LONG).show();
                                    break;

                                //Delete
                                case 2:

                                    new MaterialDialog.Builder(getActivity())
                                            .title(R.string.delete_conversation)
                                            .titleColor(getResources().getColor(R.color.lightest))
                                            .backgroundColor(getResources().getColor(R.color.primary))
                                            .positiveText(R.string.yes)
                                            .positiveColor(getResources().getColor(R.color.lightest))
                                            .negativeText(R.string.no)
                                            .negativeColor(getResources().getColor(R.color.lightest))
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    super.onPositive(dialog);

                                                    dbManager.deleteConversation(conversations.get(i).username);
                                                    conversations.remove(i);
                                                    updateConversations();
                                                }
                                            })
                                            .build().show();


                                    break;
                            }
                        }
                    }).build();
            deleteDialog.show();

            return true;
        }
    };

    public void updateConversations(){
        conversations = dbManager.getConversations();
        Utils.Log("Conversations size: " + conversations.size());
        adapter.clear();
        adapter.addAll(conversations);
        adapter.notifyDataSetChanged();
    }

    public void setOnConversationClickedListener(Interfaces.OnConversationClickedListener listener){
        this.conversationClickedListener = listener;
    }

    public void setOnActionBarChangeRequestListener(Interfaces.OnActionBarEventListener listener){
        this.actionBarChangeRequestListener = listener;
    }



}

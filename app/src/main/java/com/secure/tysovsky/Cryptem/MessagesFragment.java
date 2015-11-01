package com.secure.tysovsky.Cryptem;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tysovsky on 9/22/15.
 */
public class MessagesFragment extends Fragment{
    private Interfaces.OnActionBarEventListener actionBarChangeRequestListener;

    public static String TAG = "MessageFragment";

    public static  boolean currentlyOpen = false;

    Conversation conversation;
    String username = "";
    List<Message> messages = new ArrayList<>();
    ArrayAdapter<Message> adapter;
    ListView messageList;
    Button sendMessage;
    MaterialEditText messageText;
    DBManager dbManager;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Utils.Log("MessageFragment onAttach called");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.Log("MessageFragment onCreate called");


        if(savedInstanceState != null){
            conversation = new Conversation();
            conversation.setUsername(savedInstanceState.getString("username", ""));
            conversation.setId(savedInstanceState.getInt("_id", 0));
            conversation.setUnreadMessages(savedInstanceState.getInt("unreadMessages", 0));

            //actionBarChangeRequestListener.OnActionBarChangeRequest(Utils.ACTION_BAR_NAME_CHANGE, conversation.getUsername());
            //actionBarChangeRequestListener.OnActionBarChangeRequest(Utils.ACTION_BAR_ENABLE_BACK_BUTTON, null);
        }

        getActivity().setTitle(conversation.username);

        dbManager = new DBManager(getActivity());
        username = dbManager.getUsername();

        dbManager.resetUnreadMessages(conversation.username);

        setHasOptionsMenu(true);



    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Utils.Log("MessageFragment onCreateView called");
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        messageList = (ListView)view.findViewById(R.id.messages_list);
        messages = dbManager.getMessages(conversation.getUsername());
        adapter = new MessagesAdapter(getActivity(), messages, dbManager.getUsername(), conversation.getUsername());
        messageList.setAdapter(adapter);

        messageList.setOnItemLongClickListener(messageLongClicked);


        if(savedInstanceState == null){
            messageList.setSelection(adapter.getCount() - 6);
        }

        sendMessage = (Button)view.findViewById(R.id.btn_message_send);
        sendMessage.setOnClickListener(sendClicked);
        messageText = (MaterialEditText)view.findViewById(R.id.user_message_text);
        return view;
    }



    @Override
    public void onStart() {
        super.onStart();
        Utils.Log("MessageFragment onStart called");
        messageText.setText("");
    }

    @Override
    public void onResume() {
        super.onResume();
        currentlyOpen = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        currentlyOpen = false;
        dbManager.resetUnreadMessages(conversation.username);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Utils.Log("MessageFragment onDestroyView called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.Log("MessageFragment onDestroy called");
        conversation = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Utils.Log("MessageFragment onDetach called");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putString("username", conversation.getUsername());
        outState.putInt("_id", conversation.getId());
        outState.putInt("unreadMessages", conversation.getUnreadMessages());

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.home:
                if (getActivity().getFragmentManager().getBackStackEntryCount() > 0) {
                    getActivity().getFragmentManager().popBackStack();
                }
                Utils.Log("Back button pressed in conversationsFragment");
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    public void setConversation(Conversation conversation){
        this.conversation = conversation;
        //getActivity().setTitle(conversation.username);

    }

    private AdapterView.OnItemLongClickListener messageLongClicked = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, final View view, final int i, long l) {

            MaterialDialog deleteDialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.options)
                    .titleColor(getResources().getColor(R.color.lightest))
                    .backgroundColor(getResources().getColor(R.color.primary))
                    .items(R.array.message_options)
                    .itemColor(getResources().getColor(R.color.lightest))
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, View _view, int which, CharSequence charSequence) {
                            switch (which) {
                                //Copy
                                case 0:

                                    TextView msgText = (TextView)view.findViewById(R.id.message_text);
                                    ClipboardManager cbManager = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clipData = ClipData.newPlainText("message", msgText.getText().toString());
                                    cbManager.setPrimaryClip(clipData);

                                    Toast.makeText(getActivity(), R.string.clipboard_text_copied, Toast.LENGTH_LONG).show();
                                    break;

                                //Delete
                                case 1:


                                    new MaterialDialog.Builder(getActivity())
                                            .title(R.string.delete_message)
                                            .backgroundColor(getResources().getColor(R.color.primary))
                                            .titleColor(getResources().getColor(R.color.lightest))
                                            .positiveText(R.string.yes)
                                            .positiveColor(getResources().getColor(R.color.lightest))
                                            .negativeText(R.string.no)
                                            .negativeColor(getResources().getColor(R.color.lightest))
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    super.onPositive(dialog);

                                                    dbManager.deleteMessage(messages.get(i).dbid);


                                                    Animation anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right);
                                                    anim.setDuration(250);
                                                    anim.setAnimationListener(new Animation.AnimationListener() {
                                                        @Override
                                                        public void onAnimationStart(Animation animation) {

                                                        }

                                                        @Override
                                                        public void onAnimationEnd(Animation animation) {
                                                            messages.remove(i);
                                                            adapter.notifyDataSetChanged();
                                                        }

                                                        @Override
                                                        public void onAnimationRepeat(Animation animation) {

                                                        }
                                                    });

                                                    view.startAnimation(anim);
                                                }

                                                @Override
                                                public void onNegative(MaterialDialog dialog) {
                                                    super.onNegative(dialog);
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

    private View.OnClickListener sendClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            Utils.Log("Send clicked");

            //If the message field is not empty
            if(!messageText.getText().toString().isEmpty()){

                final Message message = new Message();
                message.setSender(username);
                message.setRecipient(conversation.getUsername());

                //If DHKE was completed and a key exists
                if(!dbManager.getAESKey(conversation.getUsername()).isEmpty()){
                    String signature = "";
                    String key = dbManager.getAESKey(conversation.getUsername());
                    byte[] iv = Crypto.GenerateRandomIV();
                    final String plainText = messageText.getText().toString();
                    final String cipherText = Crypto.AESencrypt(key, plainText, iv);
                    final String base64IV = Base64.encodeToString(iv, Base64.NO_WRAP);


                    try {
                        PrivateKey RSAKeySign = Crypto.RSAStringToPrivateKey(dbManager.getRSAKeySignaturePrivate());

                        signature = Base64.encodeToString(Crypto.RSASign(Base64.decode(cipherText, Base64.NO_WRAP), RSAKeySign), Base64.NO_WRAP);


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


                    message.setMessage(cipherText);
                    message.setIv(base64IV);
                    message.setSignature(signature);

                    new HttpHandler() {
                        @Override
                        public HttpUriRequest getHttpRequestMethod() {
                            HttpPost httpPost = new HttpPost(Utils.SERVER_SEND_MESSAGE);

                            List<NameValuePair> nameValuePairs = new ArrayList<>();
                            nameValuePairs.add(new BasicNameValuePair("sender", message.getSender()));
                            nameValuePairs.add(new BasicNameValuePair("recipient", message.getRecipient()));
                            nameValuePairs.add(new BasicNameValuePair("message", message.getMessage()));
                            nameValuePairs.add(new BasicNameValuePair("iv", message.getIv()));
                            nameValuePairs.add(new BasicNameValuePair("signature", message.getSignature()));

                            try {
                                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            return httpPost;
                        }

                        @Override
                        public void onResponse(String result) {
                            Utils.Log("HttpResult: " + result);

                            message.setMessage(plainText);
                            message.setEncrypted(false);

                            dbManager.addMessage(message);


                            messages.add(message);
                            adapter.notifyDataSetChanged();

                            messageList.setSelection(messages.size() + 1);
                        }
                    }.execute();


                    messageText.setText("");

                }

                //DHKE was not complete yet, store messages unencrypted temporarily
                //and then encrypt and send them once the exchange is complete
                else{
                    message.setEncrypted(false);
                    message.setMessage(messageText.getText().toString());
                    message.setIv(Base64.encodeToString(Crypto.GenerateRandomIV(), Base64.NO_WRAP));

                    dbManager.addMessage(message);

                    messages.add(message);
                    adapter.notifyDataSetChanged();

                    messageList.setSelection(adapter.getCount() - 1);
                    messageText.setText("");

                }

            }



        }
    };

    public void notifyAdapter(){
        adapter.notifyDataSetChanged();
        messageList.setSelection(adapter.getCount() - 1);
    }

    public void setMessages(ArrayList<Message> messages){
        this.messages = messages;
    }

    public void setOnActionBarChangeRequestListener(Interfaces.OnActionBarEventListener listener){
        this.actionBarChangeRequestListener = listener;
    }
}

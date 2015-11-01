package com.secure.tysovsky.Cryptem;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import java.util.List;

/**
 * Created by tysovsky on 9/22/15.
 */
public class MessagesAdapter extends ArrayAdapter<Message>{

    String sender;
    String recipient;
    DBManager dbManager;

    public MessagesAdapter(Context context, List<Message> messages, String username, String recipient) {
        super(context, 0, messages);
        this.sender = username;
        this.recipient = recipient;
        dbManager = new DBManager(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Message message = getItem(position);


        if (message.getSender().equals(sender)) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_message_sent, parent, false);
        } else {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_message_recieved, parent, false);
        }


        TextView messageText = (TextView) convertView.findViewById(R.id.message_text);
        TextView messageDate = (TextView) convertView.findViewById(R.id.message_date);

        if (message.isEncrypted()) {
            if (!dbManager.getAESKey(recipient).isEmpty()) {
                String key = dbManager.getAESKey(recipient);
                Utils.Log("IV: " + message.iv);
                byte[] iv = Base64.decode(message.getIv(), Base64.NO_WRAP);
                Utils.Log("IV size: " + iv.length);
                String plainText = Crypto.AESdecrypt(key, message.getMessage(), iv);

                messageText.setText(plainText);
            }
        } else {
            messageText.setText(message.getMessage());
        }


        return convertView;
    }

}

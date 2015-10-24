package com.secure.tysovsky.Cryptem;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by tysovsky on 9/22/15.
 */
public class ConversationsAdapter extends ArrayAdapter<Conversation> {


    public ConversationsAdapter(Context context, List<Conversation> conversations){
        super(context, 0, conversations);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_conversation, parent, false);
        }

        Conversation currentConversation = getItem(position);

        ImageView profileImage = (ImageView)convertView.findViewById(R.id.chat_profilePicture);
        profileImage.setTag(new Integer(position));
        profileImage.setOnClickListener(profileImageOnClickListener);

        TextView usernameView = (TextView)convertView.findViewById(R.id.chat_username);
        TextView messagesView = (TextView)convertView.findViewById(R.id.chat_newMessages);

        usernameView.setText(currentConversation.getUsername());

        if(currentConversation.getUnreadMessages() == 0){
            messagesView.setText(R.string.no_new_messages);
            messagesView.setTypeface(null, Typeface.NORMAL);
        }
        else if(currentConversation.getUnreadMessages() == 1){
            messagesView.setText(R.string.one_new_message);
            messagesView.setTypeface(null, Typeface.BOLD);
        }
        else{
            messagesView.setText(currentConversation.getUnreadMessages() + " new messages");
            messagesView.setTypeface(null, Typeface.BOLD);
        }


        return convertView;
    }

    private View.OnClickListener profileImageOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

        }
    };

}

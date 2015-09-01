package com.secure.tysovsky.cryptomessanger;

import android.graphics.Bitmap;

/**
 * Created by tysovsky on 7/21/2015.
 */
public class Conversation {
    String username;
    int unreadMessages;
    String id;
    String unreadMessagesString;

    public Conversation(String username, int unreadMessages){
        this.username = username;
        this.unreadMessages = unreadMessages;
        if(unreadMessages == 0)
        {
            unreadMessagesString = "You have no unread messages";
        }
        else if (unreadMessages == 1){
            unreadMessagesString = "You have 1 unread message";
        }
        else{
            unreadMessagesString = "You have " + unreadMessages + " unread messages";
        }

    }

    public String getUsername()
    {
        return this.username;
    }

    public String getUnreadMessagesString(){
        return this.unreadMessagesString;
    }

    public int getUnreadMessages(){
        return unreadMessages;
    }
}

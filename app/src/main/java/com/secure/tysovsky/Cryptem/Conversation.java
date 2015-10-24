package com.secure.tysovsky.Cryptem;

/**
 * Created by tysovsky on 7/21/2015.
 */
public class Conversation {
    String username;
    int unreadMessages;
    int _id;
    String id;
    String unreadMessagesString;

    public Conversation(){
        this.username = "";
        this.unreadMessages = 0;
        this._id = -1;
    }

    /**
     *
     * @param username
     * @param unreadMessages
     */
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

    public int getId(){
        return this._id;
    }

    public void setUsername(String username){
        this.username = username;
    }


    public void setId(int id){
        this._id = id;
    }

    public void setUnreadMessages(int unreadMessages){
        this.unreadMessages = unreadMessages;
    }

    public int getUnreadMessages(){
        return unreadMessages;
    }
}

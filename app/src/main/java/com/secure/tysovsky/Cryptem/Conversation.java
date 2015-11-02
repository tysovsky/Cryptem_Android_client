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
    String encrypted;
    String passwordHash;

    public Conversation(){
        this.username = "";
        this.unreadMessages = 0;
        this._id = -1;
        this.encrypted = "false";
        this.passwordHash = "";
    }

    /**
     *
     * @param username
     * @param unreadMessages
     */
    public Conversation(String username, int unreadMessages){
        this.username = username;
        this.unreadMessages = unreadMessages;
        this.encrypted = "false";
        this.passwordHash = "";
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

    public String isEncrypted(){return this.encrypted;}

    public void setEncrypted(String encrypted){this.encrypted = encrypted;}

    public String getPasswordHash(){return this.passwordHash;}

    public void setPasswordHash(String passwordHash){this.passwordHash = passwordHash;}
}

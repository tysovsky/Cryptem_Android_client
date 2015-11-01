package com.secure.tysovsky.Cryptem;

/**
 * Created by tysovsky on 7/21/2015.
 */
public class Message {
    String message;
    String date;
    String sender;
    String recipient;
    String iv;
    String signature;
    int dbid;
    boolean encrypted;

    public Message(){
        this.message = "";
        this.date = "";
        this.sender = "";
        this.recipient = "";
        this.iv = "";
        this.signature = "";
        this.dbid = -1;
        encrypted = true;
    }
    public Message (String message, String date, String sender, int dbid){
        this.message = message;
        this.date = date;
        this.sender = sender;
        this.dbid = dbid;
    }

    public String getMessage(){
        return this.message;
    }

    public String getDate(){
        return this.date;
    }

    public String getSender(){
        return sender;
    }

    public String getRecipient(){
        return this.recipient;
    }

    public String getIv(){
        return this.iv;
    }

    public String getSignature(){return this.signature;}

    public int getDbid(){
        return this.dbid;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public void setSender(String sender){
        this.sender = sender;
    }

    public void setRecipient(String recipient){
        this.recipient = recipient;
    }

    public void setDbid(int dbid){
        this.dbid = dbid;
    }

    public void setDate(String date){
        this.date = date;
    }

    public void setIv(String iv){
        this.iv = iv;
    }

    public void setSignature(String signature){this.signature = signature;}

    public void setEncrypted(boolean encrypted){
        this.encrypted = encrypted;
    }

    public boolean isEncrypted(){
        return this.encrypted;
    }
}

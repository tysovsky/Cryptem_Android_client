package com.secure.tysovsky.cryptomessanger;

/**
 * Created by tysovsky on 7/21/2015.
 */
public class Message {
    String message;
    String date;
    String senderid;

    public Message (String message, String date, String senderid){
        this.message = message;
        this.date = date;
        this.senderid = senderid;
    }

    public String getMessage(){
        return this.message;
    }

    public String getDate(){
        return this.date;
    }

    public String getSenderid(){
        return senderid;
    }
}

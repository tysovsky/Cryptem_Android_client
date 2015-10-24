package com.secure.tysovsky.Cryptem;

/**
 * Created by tysovsky on 9/22/15.
 */
public class Interfaces {
    public  interface OnActionBarEventListener{
        public void OnActionBarChangeRequest(int type, String parameter);
    }

    public  interface OnConversationClickedListener{
        public void onConversationClicked(Conversation conversation);
    }
}

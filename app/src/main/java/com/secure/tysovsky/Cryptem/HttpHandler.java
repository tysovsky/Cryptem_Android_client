package com.secure.tysovsky.Cryptem;

import org.apache.http.client.methods.HttpUriRequest;

/**
 * Created by tysovsky on 9/23/15.
 */
public abstract class HttpHandler {

    public abstract HttpUriRequest getHttpRequestMethod();

    public abstract void onResponse(String result);

    public void execute(){
        new AsyncHttpTask(this).execute();
    }
}

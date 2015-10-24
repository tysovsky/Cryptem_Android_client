package com.secure.tysovsky.Cryptem;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by tysovsky on 9/23/15.
 */
public class AsyncHttpTask extends AsyncTask<String, Void, String> {
    private HttpHandler httpHandler;

    public AsyncHttpTask(HttpHandler httpHandler){
        this.httpHandler = httpHandler;
    }

    @Override
    protected String doInBackground(String... strings) {
        String result = "";
        try {

            HttpClient httpClient = new DefaultHttpClient();

            HttpResponse httpResponse = httpClient.execute(httpHandler.getHttpRequestMethod());

            result = Utils.getResponseBody(httpResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        httpHandler.onResponse(result);
    }
}

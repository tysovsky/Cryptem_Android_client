package com.secure.tysovsky.cryptomessanger;

import android.util.Log;

import org.apache.http.HttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.util.ArrayList;

/**
 * Created by tysovsky on 8/2/2015.
 */
public class Utils {

    public static final String RESPONSE_LOGIN_SUCCESS = "success";
    public static final String RESPONSE_LOGIN_FAILURE ="failure";
    public static final String AUTHENTICATION_MESSAGE = "You have been successfully logged in";
    public static final boolean DEBUG = true;

    public static String getResponseBody(HttpResponse response){
        if(response.getEntity().getContentLength() != 0){
            StringBuilder sb = new StringBuilder();
            try{
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 65728 );
                String line = null;
                while((line = reader.readLine()) != null){
                    sb.append(line);
                }

                return sb.toString();
            }catch (IOException e){

            }catch (Exception e){

            }
        }

        return "failed";
    }

    public static ArrayList<String> parseConversationsList(String response){
        ArrayList<String> toReturn = new ArrayList<>();
        String divider = " | ";
        int startIndex = 0;
        int currIndex = 0;

        String result = "";
        while(startIndex + 3 < response.length()){
            currIndex = response.indexOf(divider, startIndex + 3);
            result = response.substring(startIndex+3, currIndex);
            toReturn.add(result);
            startIndex = currIndex;
        }


        return  toReturn;
    }

    /**
     * Converts byte array to hexidecimal useful for logging and fault finding
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void Log(String stringToLog){
        if(DEBUG){
            Log.i("DEBUG", stringToLog);
        }
    }
}

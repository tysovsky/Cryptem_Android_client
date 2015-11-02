package com.secure.tysovsky.Cryptem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.squareup.okhttp.internal.Util;

import java.util.ArrayList;

/**
 * Created by tysovsky on 9/24/15.
 */
public class DBManager extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 19;
    private static final String DATABASE_NAME = "cryptem.db";

    private static final String TABLE_USER_DETAILS = "userdetails";
    private static final String TABLE_CONVERSATIONS = "conversations";
    private static final String TABLE_MESSAGES = "messages";
    private static final String TABLE_KEYS = "keys";
    private static final String TABLE_DHKE = "dhke";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_UNREAD_MESSAGES = "unread";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_SENDER = "sender";
    private static final String COLUMN_RECIPIENT = "recipient";
    private static final String COLUMN_KEY = "key";
    private static final String COLUMN_RSAKEYSIGN = "rsakeysign";
    private static final String COLUMN_RSAKEYENC = "rsakeyenc";
    private static final String COLUMN_PASSWORDHASH = "passwordhash";
    private static final String COLUMN_IV = "iv";
    private static final String COLUMN_DHKE_PRIME = "prime";
    private static final String COLUMN_DHKE_GENERATOR = "generator";
    private static final String COLUMN_DHKE_PRIVATE_KEY = "private";
    private static final String COLUMN_DHKE_PUBLIC_KEY = "public";
    private static final String COLUMN_ENCRYPTED = "encrypted";
    private static final String COLUMN_RSAKEYENCRYPTIONPRIVATE = "RSAKeyEncryptionPrivate";
    private static final String COLUMN_RSAKEYENCRYPTIONPUBLIC = "RSAKeyEncryptionPublic";
    private static final String COLUMN_RSAKEYSIGNATUREPRIVATE = "RSAKeySignaturePrivate";
    private static final String COLUMN_RSAKEYSIGNATUREPUBLIC = "RSAKeySignaturePublic";


    public DBManager(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        Utils.Log("Creating database");

        String CONVERSATIONS_QUERY = "CREATE TABLE " + TABLE_CONVERSATIONS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT, "+
                COLUMN_UNREAD_MESSAGES + " INTEGER, " +
                COLUMN_ENCRYPTED + " TEXT, " +
                COLUMN_PASSWORDHASH + " TEXT " +
                ");";
        sqLiteDatabase.execSQL(CONVERSATIONS_QUERY);

        String MESSAGES_QUERY = "CREATE TABLE " + TABLE_MESSAGES + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MESSAGE + " TEXT, "+
                COLUMN_SENDER + " TEXT, " +
                COLUMN_RECIPIENT + " TEXT, " +
                COLUMN_ENCRYPTED + " TEXT, " +
                COLUMN_IV + " TEXT " +
                ");";

        sqLiteDatabase.execSQL(MESSAGES_QUERY);

        String KEYS_QUERY = "CREATE TABLE " + TABLE_KEYS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_KEY + " TEXT, " +
                COLUMN_RSAKEYSIGN + " TEXT, " +
                COLUMN_RSAKEYENC + " TEXT, " +
                COLUMN_ENCRYPTED + " TEXT " +
                ");";

        sqLiteDatabase.execSQL(KEYS_QUERY);

        String DHKE_QUERY = "CREATE TABLE " + TABLE_DHKE + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_DHKE_PRIME + " TEXT, " +
                COLUMN_DHKE_GENERATOR + " TEXT, " +
                COLUMN_DHKE_PRIVATE_KEY + " TEXT, " +
                COLUMN_DHKE_PUBLIC_KEY + " TEXT " +
                ");";
        sqLiteDatabase.execSQL(DHKE_QUERY);




    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        Utils.Log("Updating database");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_CONVERSATIONS);
        //sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_DETAILS);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_KEYS);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_DHKE);

       onCreate(sqLiteDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
        onUpgrade(db, oldVersion, newVersion);
    }

    //region Conversations
    public void addConversation(Conversation conversation){
        ContentValues values = new ContentValues();
        values.put(COLUMN_UNREAD_MESSAGES, conversation.getUnreadMessages());
        values.put(COLUMN_USERNAME, conversation.getUsername());
        values.put(COLUMN_ENCRYPTED, conversation.isEncrypted());
        values.put(COLUMN_PASSWORDHASH, conversation.getPasswordHash());

        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_CONVERSATIONS, null, values);
        db.close();
    }

    public void deleteConversation(String username){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_CONVERSATIONS + " WHERE " + COLUMN_USERNAME + "=\"" + username + "\";");

        //Delete messages associated with the conversation
        db.execSQL("DELETE FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_SENDER +
                "=\"" + username + "\" OR " + COLUMN_RECIPIENT + "=\"" + username + "\";");

        db.close();
    }

    public ArrayList<Conversation> getConversations(){
        ArrayList<Conversation> conversations = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String QUERY = "SELECT * FROM " + TABLE_CONVERSATIONS + " WHERE 1";

        try {
            Cursor cursor = db.rawQuery(QUERY, null);
            cursor.moveToFirst();

            while(!cursor.isAfterLast()){
                if(cursor.getString(cursor.getColumnIndex(COLUMN_ID)) != null){
                    Conversation conversation = new Conversation();

                    conversation._id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
                    conversation.username = cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME));
                    conversation.unreadMessages = cursor.getInt(cursor.getColumnIndex(COLUMN_UNREAD_MESSAGES));
                    conversation.setEncrypted(cursor.getString(cursor.getColumnIndex(COLUMN_ENCRYPTED)));
                    conversation.setPasswordHash(cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORDHASH)));

                    conversations.add(conversation);

                    cursor.moveToNext();

                }

            }
        }catch (SQLiteException e){
            db.close();
            return null;
        }


        db.close();
        return conversations;
    }

    public Conversation getConversation(String username){
        Conversation conversation = new Conversation();
        SQLiteDatabase db = getReadableDatabase();
        String QUERY = "SELECT * FROM " + TABLE_CONVERSATIONS + " WHERE " + COLUMN_USERNAME + "=\"" + username+"\";";

        Cursor cursor = db.rawQuery(QUERY, null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            if(cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)) != null){
                conversation.username = cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME));
                conversation.unreadMessages = cursor.getInt(cursor.getColumnIndex(COLUMN_UNREAD_MESSAGES));
            }
            cursor.moveToNext();
        }

        if(conversation.getUsername().isEmpty()){
            return null;
        }
        return conversation;
    }

    public int getUnreadMessages(String username){

        String QUERY = "SELECT * FROM " + TABLE_CONVERSATIONS +
                " WHERE " + COLUMN_USERNAME + "='" + username +"';";

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(QUERY, null);
        cursor.moveToFirst();
        if(cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)) != null){
            return cursor.getInt(cursor.getColumnIndex(COLUMN_UNREAD_MESSAGES));
        }

        db.close();
        return 0;

    }

    public void incrementUnreadMessages(String username){

        int currentUnreadMessages = getUnreadMessages(username) + 1;

        String QUERY = "UPDATE " + TABLE_CONVERSATIONS +
                " SET " + COLUMN_UNREAD_MESSAGES + "='" + currentUnreadMessages + "'" +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";

        SQLiteDatabase db = getWritableDatabase();

        db.execSQL(QUERY);

        db.close();

    }

    public void resetUnreadMessages(String username){
        String QUERY = "UPDATE " + TABLE_CONVERSATIONS +
                " SET " + COLUMN_UNREAD_MESSAGES + "='0'" +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(QUERY);
        db.close();
    }

    public void setConversationEncrypted(int id, String encrypted){
        String QUERY = "UPDATE " + TABLE_CONVERSATIONS +
                " SET " + COLUMN_ENCRYPTED + "='" + encrypted + "'" +
                " WHERE " + COLUMN_ID + "='" + id + "';";
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(QUERY);
        db.close();
    }

    public void setConversationPasswordHash(int id, String passwordHash){
        String QUERY = "UPDATE " + TABLE_CONVERSATIONS +
                " SET " + COLUMN_PASSWORDHASH + "='" + passwordHash + "'" +
                " WHERE " + COLUMN_ID + "='" + id + "';";
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(QUERY);
        db.close();
    }
    //endregion



    //region User Details
    public void addUserDetails(String username, String RSAKeyEncryptionPublic, String RSAKeyEncryptionPrivate,
                               String RSAKeySignaturePublic, String RSAKeySignaturePrivate){
        SQLiteDatabase db = getWritableDatabase();
        //We should only have one user at a time
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_DETAILS);

        String USER_DETAIL_QUERY = "CREATE TABLE " + TABLE_USER_DETAILS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT ," +
                COLUMN_USERNAME + " TEXT, "+
                COLUMN_RSAKEYENCRYPTIONPRIVATE + " TEXT, " +
                COLUMN_RSAKEYENCRYPTIONPUBLIC + " TEXT, " +
                COLUMN_RSAKEYSIGNATUREPRIVATE + " TEXT, " +
                COLUMN_RSAKEYSIGNATUREPUBLIC + " TEXT " +
                ");";

        db.execSQL(USER_DETAIL_QUERY);

        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_RSAKEYENCRYPTIONPRIVATE, RSAKeyEncryptionPrivate);
        values.put(COLUMN_RSAKEYENCRYPTIONPUBLIC, RSAKeyEncryptionPublic);
        values.put(COLUMN_RSAKEYSIGNATUREPRIVATE, RSAKeySignaturePrivate);
        values.put(COLUMN_RSAKEYSIGNATUREPUBLIC, RSAKeySignaturePublic);


        db.insert(TABLE_USER_DETAILS, null, values);
        db.close();
    }

    public String getUsername(){
        String username = null;
        SQLiteDatabase db = getReadableDatabase();
        String QUERY = "SELECT * FROM " + TABLE_USER_DETAILS + " WHERE 1";

        try {
            Cursor cursor = db.rawQuery(QUERY, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()){
                if(cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)) != null){
                    username = cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME));
                    cursor.moveToNext();
                }
            }
        }catch (SQLiteException e){
            Utils.Log("SQLite exception: " + e.getMessage());
            db.close();
            return null;
        }

        db.close();
        return username;
    }

    public String getRSAKeyEncryptionPrivate(){
        String RSAKey = null;
        SQLiteDatabase db = getReadableDatabase();
        String QUERY = "SELECT * FROM " + TABLE_USER_DETAILS + " WHERE 1";

        try{
            Cursor cursor = db.rawQuery(QUERY, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()){
                if(cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYENCRYPTIONPRIVATE)) != null){
                    RSAKey = cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYENCRYPTIONPRIVATE));
                    cursor.moveToNext();
                }
            }
        }catch (SQLiteException e){
            db.close();
            return null;
        }


        db.close();
        return RSAKey;
    }

    public String getRSAKeyEncryptionPublic(){
        String RSAKey = null;
        SQLiteDatabase db = getReadableDatabase();
        String QUERY = "SELECT * FROM " + TABLE_USER_DETAILS + " WHERE 1";

        try{
            Cursor cursor = db.rawQuery(QUERY, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()){
                if(cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYENCRYPTIONPUBLIC)) != null){
                    RSAKey = cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYENCRYPTIONPUBLIC));
                    cursor.moveToNext();
                }
            }
        }catch (SQLiteException e){
            db.close();
            return null;
        }


        db.close();
        return RSAKey;
    }

    public String getRSAKeySignaturePrivate(){
        String RSAKey = null;
        SQLiteDatabase db = getReadableDatabase();
        String QUERY = "SELECT * FROM " + TABLE_USER_DETAILS + " WHERE 1";

        try{
            Cursor cursor = db.rawQuery(QUERY, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()){
                if(cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYSIGNATUREPRIVATE)) != null){
                    RSAKey = cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYSIGNATUREPRIVATE));
                    cursor.moveToNext();
                }
            }
        }catch (SQLiteException e){
            db.close();
            return null;
        }


        db.close();
        return RSAKey;
    }

    public String getRSAKeySignaturePublic(){
        String RSAKey = null;
        SQLiteDatabase db = getReadableDatabase();
        String QUERY = "SELECT * FROM " + TABLE_USER_DETAILS + " WHERE 1";

        try {
            Cursor cursor = db.rawQuery(QUERY, null);
            cursor.moveToFirst();

            while (!cursor.isAfterLast()){
                if(cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYSIGNATUREPUBLIC)) != null){
                    RSAKey = cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYSIGNATUREPUBLIC));
                    cursor.moveToNext();
                }
            }
        }catch (SQLiteException e){
            db.close();
            return null;
        }


        db.close();
        return RSAKey;
    }
    //endregion



    //region Messages
    public void addMessage(Message message){
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE, message.getMessage());
        values.put(COLUMN_SENDER, message.getSender());
        values.put(COLUMN_RECIPIENT, message.getRecipient());
        values.put(COLUMN_IV, message.getIv());
        values.put(COLUMN_ENCRYPTED, message.isEncrypted()?"TRUE":"FALSE");

        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_MESSAGES, null, values);
        db.close();
    }

    public ArrayList<Message>getMessages(String sender){
        ArrayList<Message> messages = new ArrayList<>();
        String QUERY = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_SENDER +
                "=\"" + sender + "\" OR " + COLUMN_RECIPIENT + "=\"" + sender + "\";";
        SQLiteDatabase db = getReadableDatabase();

        try{
            Cursor cursor = db.rawQuery(QUERY, null);
            cursor.moveToFirst();

            while(!cursor.isAfterLast()){
                if(cursor.getString(cursor.getColumnIndex(COLUMN_ID)) != null){
                    Message message = new Message();
                    message.setMessage(cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE)));
                    message.setSender(cursor.getString(cursor.getColumnIndex(COLUMN_SENDER)));
                    message.setRecipient(cursor.getString(cursor.getColumnIndex(COLUMN_RECIPIENT)));
                    message.setDbid(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
                    message.setEncrypted(cursor.getString(cursor.getColumnIndex(COLUMN_ENCRYPTED)).equals("TRUE")?true:false);
                    message.setIv(cursor.getString(cursor.getColumnIndex(COLUMN_IV)));


                    messages.add(message);

                    cursor.moveToNext();
                }
            }
        }catch (SQLiteException e){
            db.close();
            return null;
        }



        db.close();
        return messages;

    }

    public void deleteMessage(int id){
        String QUERY = "DELETE FROM " + TABLE_MESSAGES + " WHERE " +
                COLUMN_ID + "='" + id + "';";
        SQLiteDatabase db = getWritableDatabase();

        db.execSQL(QUERY);

        db.close();
    }

    public void updateMessage(Message message){
        String QUERY = "UPDATE " + TABLE_MESSAGES +
                " SET " + COLUMN_MESSAGE + "='" + message.getMessage() + "', " +
                COLUMN_IV + "='" + message.getIv() + "', " +
                COLUMN_ENCRYPTED + "='" + (message.isEncrypted()?"TRUE":"FALSE") +
                "' WHERE " + COLUMN_ID + "='" + message.getDbid() + "';";
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(QUERY);
        db.close();
    }
    //endregion


    //region Diffie-Hellman
    public void InsertDHKEStep1(String username, String prime, String generator){
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_DHKE_PRIME, prime);
        values.put(COLUMN_DHKE_GENERATOR, generator);

        SQLiteDatabase db = getWritableDatabase();

        db.insert(TABLE_DHKE, null, values);

        db.close();


    }

    public void InsertDHKEPrivateKey(String username, String privateKey){
        String QUERY = "UPDATE " + TABLE_DHKE +
                " SET " + COLUMN_DHKE_PRIVATE_KEY + "='" + privateKey + "'" +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(QUERY);
        db.close();
    }

    public void InsertDHKEPublicKey(String username, String publicKey){
        String QUERY = "UPDATE " + TABLE_DHKE +
                " SET " + COLUMN_DHKE_PUBLIC_KEY + "='" + publicKey + "'" +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(QUERY);
        db.close();
    }

    public String getDHKEPrime(String username){
        String prime = "";
        String QUERY = "SELECT * FROM " + TABLE_DHKE +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(QUERY, null);
        cursor.moveToFirst();

        if(cursor.getString(cursor.getColumnIndex(COLUMN_DHKE_PRIME)) != null){
            prime = cursor.getString(cursor.getColumnIndex(COLUMN_DHKE_PRIME));
        }

        db.close();

        return prime;
    }

    public String getDHKEGenerator(String username){
        String generator = "";
        String QUERY = "SELECT * FROM " + TABLE_DHKE +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(QUERY, null);
        cursor.moveToFirst();

        if(cursor.getString(cursor.getColumnIndex(COLUMN_DHKE_GENERATOR)) != null){
            generator = cursor.getString(cursor.getColumnIndex(COLUMN_DHKE_GENERATOR));
        }

        db.close();

        return generator;
    }

    public String getDHKEPrivateKey(String username){
        String key = "";
        String QUERY = "SELECT * FROM " + TABLE_DHKE +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(QUERY, null);
        cursor.moveToFirst();

        if(cursor.getString(cursor.getColumnIndex(COLUMN_DHKE_PRIVATE_KEY)) != null){
            key = cursor.getString(cursor.getColumnIndex(COLUMN_DHKE_PRIVATE_KEY));
        }

        db.close();

        return key;
    }

    public String getDHKEPublicKey(String username){
        String key = "";
        String QUERY = "SELECT * FROM " + TABLE_DHKE +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(QUERY, null);
        cursor.moveToFirst();

        if(cursor.getString(cursor.getColumnIndex(COLUMN_DHKE_PUBLIC_KEY)) != null){
            key = cursor.getString(cursor.getColumnIndex(COLUMN_DHKE_PUBLIC_KEY));
        }

        db.close();

        return key;
    }

    public void deleteDHKEEntry(String username){
        String QUERY = "DELETE FROM " + TABLE_DHKE +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(QUERY);
        db.close();
    }
    //endregion


    //region Keys
    public void insertKey(String username, String key){
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("username", username);
        db.insertOrThrow(TABLE_KEYS, null, cv);

        String QUERY = "UPDATE " + TABLE_KEYS +
                " SET " + COLUMN_KEY + "='" + key + "'" +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";


        db.execSQL(QUERY);
        db.close();
    }

    public void insertRSAKeySign(String username, String key){

        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("username", username);
        db.insertOrThrow(TABLE_KEYS, null, cv);

        String QUERY = "UPDATE " + TABLE_KEYS +
                " SET " + COLUMN_RSAKEYSIGN + "='" + key + "'" +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";


        db.execSQL(QUERY);
        db.close();
    }

    public void insertRSAKeyEnc(String username, String key){
        SQLiteDatabase db = getWritableDatabase();

        Utils.Log("hey");

        ContentValues cv = new ContentValues();
        cv.put("username", username);
        db.insertOrThrow(TABLE_KEYS, null, cv);

        String QUERY = "UPDATE " + TABLE_KEYS +
                " SET " + COLUMN_RSAKEYENC + "='" + key + "'" +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";


        db.execSQL(QUERY);
        db.close();
    }


    public String getAESKey(String username){
        String key = "";

        String QUERY = "SELECT * FROM " + TABLE_KEYS +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(QUERY, null);
        cursor.moveToFirst();

        if(cursor.getCount() > 0){
            if(cursor.getString(cursor.getColumnIndex(COLUMN_KEY)) != null){
                key = cursor.getString(cursor.getColumnIndex(COLUMN_KEY));
            }
        }



        return key;

    }

    public String getRSAKeySign(String username){
        String key = "";

        String QUERY = "SELECT * FROM " + TABLE_KEYS +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(QUERY, null);
        cursor.moveToFirst();

        if(cursor.getCount() > 0){
            if(cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYSIGN)) != null){
                key = cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYSIGN));
            }
        }

        return key;

    }

    public String getRSAKeyEnc(String username){
        String key = "";

        String QUERY = "SELECT * FROM " + TABLE_KEYS +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(QUERY, null);
        cursor.moveToFirst();

        if(cursor.getCount() > 0){
            if(cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYENC)) != null){
                key = cursor.getString(cursor.getColumnIndex(COLUMN_RSAKEYENC));
            }
        }

        return key;

    }

    public void updateAESKey(String username, String key){
        String QUERY = "UPDATE " + TABLE_KEYS +
                " SET " + COLUMN_KEY + "='" + key + "'" +
                " WHERE " + COLUMN_USERNAME + "='" + username + "';";

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(QUERY);
        db.close();
    }
    //endregion
}

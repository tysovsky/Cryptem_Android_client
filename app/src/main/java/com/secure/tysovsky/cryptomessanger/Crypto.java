package com.secure.tysovsky.cryptomessanger;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by tysovsky on 8/1/2015.
 */
public class Crypto {

    private static final String TAG = "AESCrypt";

    //AESCrypt-ObjC uses CBC and PKCS7Padding
    private static final String AES_MODE = "AES/CBC/PKCS7Padding";
    public static final String CHARSET = "UTF-8";

    //AESCrypt-ObjC uses SHA-256 (and so a 256-bit key)
    private static final String HASH_ALGORITHM = "SHA-256";

    //AESCrypt-ObjC uses blank IV (not the best security, but the aim here is compatibility)
    private static final byte[] ivBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    //togglable log option (please turn off in live!)
    public static boolean DEBUG_LOG_ENABLED = false;


    /**
     * Generates SHA256 hash of the password which is used as key
     *
     * @param password used to generated key
     * @return SHA256 of the password
     */
    public static SecretKeySpec generateKey(final String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] bytes = password.getBytes("UTF-8");
        digest.update(bytes, 0, bytes.length);
        byte[] key = digest.digest();

        //log("SHA-256 key ", key);

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        return secretKeySpec;
    }

    /**
     * Generate random Initialization Vector of size 16 bytes
     * @return A byte array of random bytes
     */
    public static byte[] GenerateRandomIV()
    {
        byte[] IV = new byte[16];
        //Use the same IV for debugging
        if(Utils.DEBUG){
            for(int i = 0; i < IV.length; i++){
                IV[i] = 0x00;
            }
            return IV;
        }
        SecureRandom random = new SecureRandom();
        random.nextBytes(IV);
        return IV;
    }

    /**
     * Generate random Initialization Vector
     * @param size the size of IV, in bytes
     * @returnA byte array of random bytes
     */
    public static byte[] GenerateRandomIV(int size){
        byte[] IV = new byte[size];

        SecureRandom random = new SecureRandom();
        random.nextBytes(IV);
        return IV;
    }


    //region Advanced Encryption Standard

    /**
     * Encrypt and encode message using 256-bit AES with key generated from password.
     * @param password used to generated key
     * @param message the thing you want to encrypt assumed String UTF-8
     * @return Base64 encoded CipherText
     * @throws GeneralSecurityException if problems occur during encryption
     */
    public static String AESencrypt(final String password, String message)
            throws GeneralSecurityException {

        try {
            final SecretKeySpec key = generateKey(password);


            byte[] cipherText = AESencrypt(key, ivBytes, message.getBytes(CHARSET));

            //NO_WRAP is important as was getting \n at the end
            String encoded = Base64.encodeToString(cipherText, Base64.NO_WRAP);
            return encoded;
        } catch (UnsupportedEncodingException e) {
            if (Utils.DEBUG)
                Log.e(TAG, "UnsupportedEncodingException ", e);
            throw new GeneralSecurityException(e);
        }
    }

    /**
     * Encrypt and encode message using 256-bit AES with a key generated from password and a custom IV
     * @param password string used to generate key
     * @param plainText plaintext to encrypt
     * @param iv 16-byte Initialization Vector
     * @return Base64 encoded CipherText
     */
    public static String AESencrypt(final String password, String plainText, byte[] iv){
        try {
            final SecretKeySpec key = generateKey(password);

            byte[] cipherText = AESencrypt(key, iv, plainText.getBytes(CHARSET));

            String encoded = Base64.encodeToString(cipherText, Base64.NO_WRAP);

            return encoded;
        }catch (UnsupportedEncodingException e){
            Utils.Log( "ERROR: Crypto.encrypt(~): UnsupportedEncodingException: " + e.getMessage());
        }catch (NoSuchAlgorithmException e){
            Utils.Log("ERROR: Crypto.encrypt(~): NoSuchAlgorithmException: " + e.getMessage());
        }
        catch (GeneralSecurityException e){
            Utils.Log("ERROR: Crypto.encrypt(~): GeneralSecurityException: " + e.getMessage());
        }

        return "| encryption failed |";
    }


    /**
     * More flexible AES encrypt that doesn't encode
     * @param key AES key typically 128, 192 or 256 bit
     * @param iv Initiation Vector
     * @param message in bytes (assumed it's already been decoded)
     * @return Encrypted cipher text (not encoded)
     * @throws GeneralSecurityException if something goes wrong during encryption
     */
    public static byte[] AESencrypt(final SecretKeySpec key, final byte[] iv, final byte[] message)
            throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance(AES_MODE);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        //Utils.Log("Crypto.encrypt(~~).ivSpec: " + ivSpec);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] cipherText = cipher.doFinal(message);

        //Utils.Log("Crypto.encrypt(~~).cipherText: " + cipherText);

        return cipherText;
    }


    /**
     * Decrypt and decode ciphertext using 256-bit AES with key generated from password
     *
     * @param password used to generated key
     * @param base64EncodedCipherText the encrpyted message encoded with base64
     * @return message in Plain text (String UTF-8)
     * @throws GeneralSecurityException if there's an issue decrypting
     */
    public static String AESdecrypt(final String password, String base64EncodedCipherText)
            throws GeneralSecurityException {

        try {
            final SecretKeySpec key = generateKey(password);

            byte[] decodedCipherText = Base64.decode(base64EncodedCipherText, Base64.NO_WRAP);

            byte[] decryptedBytes = AESdecrypt(key, ivBytes, decodedCipherText);

            String message = new String(decryptedBytes, CHARSET);

            return message;
        } catch (UnsupportedEncodingException e) {
            if (DEBUG_LOG_ENABLED)
                Log.e(TAG, "UnsupportedEncodingException ", e);

            throw new GeneralSecurityException(e);
        }
    }

    /**
     * Decrypt and decide CipherText using 256-bit AES with key generated from password
     * @param password string used to generate key
     * @param base64EncodedCipherText the encrypted message encoded with base64
     * @param iv custom Initialization Vector
     * @return message in Plain Text (UTF-8 String)
     */
    public static String AESdecrypt(final String password, String base64EncodedCipherText, byte[] iv){
        try {
            final SecretKeySpec key = generateKey(password);

            byte[] decodedCipherText = Base64.decode(base64EncodedCipherText, Base64.NO_WRAP);

            byte[] decryptedBytes = AESdecrypt(key, iv, decodedCipherText);

            String plainText = new String(decryptedBytes, CHARSET);

            return plainText;

        }catch (NoSuchAlgorithmException e){
            Utils.Log("ERROR: Crypto.decrypt: NoSuchAlgorithmException: " + e.getMessage());
        }catch (GeneralSecurityException e){
            Utils.Log("ERROR: Crypto.decrypt: GeneralSecurityException: " + e.getMessage());
        }catch (UnsupportedEncodingException e){
            Utils.Log("ERROR: Crypto.decrypt: UnsupportedEncodingException: " + e.getMessage());
        }

        return "| decryption failed |";
    }


    /**
     * More flexible AES decrypt that doesn't encode
     * @param key AES key typically 128, 192 or 256 bit
     * @param iv Initiation Vector
     * @param decodedCipherText in bytes (assumed it's already been decoded)
     * @return Decrypted message cipher text (not encoded)
     * @throws GeneralSecurityException if something goes wrong during encryption
     */
    public static byte[] AESdecrypt(final SecretKeySpec key, final byte[] iv, final byte[] decodedCipherText)
            throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance(AES_MODE);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(decodedCipherText);

        return decryptedBytes;
    }
    //endregion



    //region Digital Signature Algorithm

    /**
     * Generate a pair of 1024-bit Public and Private keys to be used for DSA signing/verifying
     * @return KeyPair object that contains Private and Public keys
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateDSAKeyPair()
            throws NoSuchProviderException, NoSuchAlgorithmException {
        //Create a Key Pair Generator
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG","SUN");
        keyGen.initialize(1024, random);

        return keyGen.generateKeyPair();

    }

    /**
     * Generate a DSA signature
     * @param privateKey Private Keys used to sign the message
     * @param message Message to sign
     * @return DSA signature of the message
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public static byte[] DSASign(PrivateKey privateKey, byte[] message)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
        dsa.initSign(privateKey);
        dsa.update(message);

        return dsa.sign();
    }

    /**
     * Verify DSA signature
     * @param publicKey Public Key used for verification
     * @param message message to verify
     * @param signature DSA signature of the message
     * @return whether the signature os correct or not
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public static boolean DSAVerify(PublicKey publicKey, byte[] message, byte[] signature)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        Signature sig = Signature.getInstance("SHA1withDSA", "SUN");
        sig.initVerify(publicKey);
        sig.update(message);

        return sig.verify(signature);

    }
    //endregion


}

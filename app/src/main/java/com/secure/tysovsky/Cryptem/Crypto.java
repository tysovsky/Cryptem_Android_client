package com.secure.tysovsky.Cryptem;

import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openssl.jcajce.JcaPEMWriter;

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

    public static BigInteger getPrime(int bitSize){
        return BigInteger.probablePrime(bitSize, new SecureRandom());
    }

    /**
     * Generate random Initialization Vector of size 16 bytes
     * @return A byte array of random bytes
     */
    public static byte[] GenerateRandomIV()
    {
        byte[] IV = new byte[16];
        //Use constant, all 0 iv for debugging
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
            if (Utils.DEBUG)
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
    

    //region Diffie-Hellman Key Exchange

    /**
     * Generate randomly distributed, 512-bit private key for Diffie-Hellman
     * @return 512-bit random key
     */
    public static byte[] DHGeneratePrivateKey(){
        SecureRandom random = new SecureRandom();
        BigInteger privateKey = new BigInteger(512, random);
        return privateKey.toByteArray();
    }

    /**
     * Generate a public key for Diffie-Hellman
     * @param prime modulus used
     * @param privateKey user's private key
     * @return Diffie Hellman public key that can be shared with the other party
     */
    public static byte[] DHGeneratePublicKey(byte[] prime, byte[] privateKey){
        BigInteger _generator = new BigInteger("2");
        BigInteger _prime = new BigInteger(prime);
        BigInteger _privateKey = new BigInteger(privateKey);

        Utils.Log("Prime: " + _prime.toString());
        Utils.Log("Generator: " + _generator.toString());

        return _generator.modPow(_privateKey, _prime).toByteArray();
    }

    /**
     * Get a secret shared between two parties
     * @param prime modulus ised
     * @param publicKey public key from the other party
     * @param privateKey user's private key
     * @return A secret shared between both parties
     */
    public static byte[] DHGenerateSharedPrivateKey(byte[] prime, byte[] publicKey, byte[] privateKey){

        BigInteger _prime = new BigInteger(prime);
        BigInteger _publicKey = new BigInteger(publicKey);
        BigInteger _privateKey = new BigInteger(privateKey);
        return _publicKey.modPow(_privateKey, _prime).toByteArray();
    }

    //endregion


    //region Rivest-Shamir-Adleman

    /**
     * Generate a pair of RSA keys of specified size
     * @param size the size of the modulus
     * @return KeyPair containing Private and Public Keys
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair RSAGenerateKeyPair(int size)
            throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(size);
        return kpg.generateKeyPair();
    }

    /**
     * Generate a pair of RSA keys of size 2048
     * @return KeyPair containing Private and Public Keys
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair RSAGenerateKeyPair()
            throws NoSuchAlgorithmException {
        return RSAGenerateKeyPair(2048);
    }




    public static byte[] RSAEncrypt(byte[] data, PublicKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherData = cipher.doFinal(data);

        return cipherData;
    }

    public static byte[] RSAEncrypt(byte[] data, PrivateKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherData = cipher.doFinal(data);

        return cipherData;
    }

    public static byte[] RSADecrypt(byte[] data, PrivateKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = cipher.doFinal(data);

        return plainText;
    }

    public static byte[] RSADecrypt(byte[] data, PublicKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = cipher.doFinal(data);

        return plainText;
    }

    public static byte[] RSASign(byte[] data, PrivateKey key)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        Security.addProvider(new BouncyCastleProvider());
        Signature signature = Signature.getInstance("SHA256withRSA", "BC");
        signature.initSign(key, new SecureRandom());
        signature.update(data);
        byte[] sigBytes = signature.sign();

        return sigBytes;
    }


    public static boolean RSAVerify(byte[] data, byte[] signature, PublicKey key)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Security.addProvider(new BouncyCastleProvider());
        Signature sig = Signature.getInstance("SHA256withRSA", "BC");
        sig.initVerify(key);
        sig.update(data);

        boolean result = sig.verify(signature);

        return result;
    }

    public static String RSAtoPemString(PublicKey key)
            throws IOException {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
        pemWriter.writeObject(key);
        pemWriter.close();

        return stringWriter.toString();
    }

    public static PrivateKey RSAStringToPrivateKey(String key)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.decode(key, Base64.NO_WRAP);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = factory.generatePrivate(keySpec);

        return privateKey;
    }

    public static PublicKey RSAStrigToPublicKey(String key)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        key = key.replaceAll("(-+BEGIN PUBLIC KEY-+\\r?\\n|-+END PUBLIC KEY-+\\r?\\n?)", "");


        byte[] decoded = Base64.decode(key, Base64.NO_WRAP);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey pkey = factory.generatePublic(keySpec);

        return pkey;
    }


    //endregion

}

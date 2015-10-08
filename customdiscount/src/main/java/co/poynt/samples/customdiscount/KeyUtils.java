package co.poynt.samples.customdiscount;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Base64;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openssl.PEMException;
import org.spongycastle.openssl.PEMKeyPair;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by sathyaiyer on 7/28/15.
 */
public class KeyUtils {

    static final String TAG = "KeyUtils";

    public static synchronized KeyPair getKeyPair(Context context, String fileName) {
        BufferedReader br = null;
        try {
            AssetManager mngr = context.getAssets();
            InputStream is2 = mngr.open(fileName);
            br = new BufferedReader(new InputStreamReader(is2));
//                br = new BufferedReader(new FileReader(file));
            // Read text from file
            Security.addProvider(new BouncyCastleProvider());
            PEMParser pp = new PEMParser(br);
            PEMKeyPair pemKeyPair = (PEMKeyPair) pp.readObject();
            KeyPair pair = new JcaPEMKeyConverter().getKeyPair(pemKeyPair);
            pp.close();
            Log.d("M", "Loaded keypair from file " + pair.toString());
            return pair;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (PEMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static PublicKey getPublicKey(String key) {
        try {
            byte[] keyBytes = Base64.decode(key, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static PrivateKey getPrivateKey(String key) {
        try {
            BufferedReader br = new BufferedReader(new StringReader(key));

//            byte[] keyBytes = Base64.decode(key, Base64.DEFAULT);
            Security.addProvider(new BouncyCastleProvider());
            PEMParser pemParser = new PEMParser( new StringReader(key));
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
            return kp.getPrivate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


     static String signMessage(String message, PrivateKey privateKey)  {
        try {
            // Create RSA-signer with the private key
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);

            byte[] buffer = message.getBytes("utf-8");
            signer.update(buffer, 0, buffer.length);
            byte[] signedData =  signer.sign();
            return Base64.encodeToString(signedData,Base64.NO_WRAP);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    static boolean verify(PublicKey publicKey, byte[] signedData, String clearData ){
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initVerify(publicKey);
            byte[] buffer = clearData.getBytes("utf-8");
            signer.update(buffer, 0, buffer.length);
            return signer.verify(signedData);
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}

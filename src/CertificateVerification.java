import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import sun.misc.BASE64Decoder;


public class CertificateVerification {

    public static void main(String[] args) {
        KeyFactory kf = null;
        File keyFile = null;
        byte[] data;
        PublicKey certPubKey = null;
        
        CertificateFactory cf = null;
        File certificateFile = null;
        InputStream is = null;
        Certificate generatedCertificate = null;
        
        PublicKey servPubKey = null;
        
        try{
        cf = CertificateFactory.getInstance("X.509");
        
        certificateFile = new File("CA-certificate.crt");
        is = new FileInputStream(certificateFile);           
        generatedCertificate = cf.generateCertificate(is);
        
        kf = KeyFactory.getInstance("RSA");
        
        keyFile = new File("ashkan-public.key");
        data = Files.readAllBytes(keyFile.toPath()); 
        
        String temp = new String(data);
        String publicKeyPEM = temp.replace("-----BEGIN PUBLIC KEY-----\n", "");
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");

        BASE64Decoder b64=new BASE64Decoder();
        byte[] decoded = b64.decodeBuffer(publicKeyPEM);
          
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        certPubKey = kf.generatePublic(keySpec); 
        System.out.println(certPubKey);
        
        generatedCertificate.verify(certPubKey);
        
       }catch (Exception e) {
            e.printStackTrace();
	}
        
        servPubKey = generatedCertificate.getPublicKey();
        
        System.out.println(servPubKey);
    }
    
}

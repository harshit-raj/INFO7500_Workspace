package hw2;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;

public class GenerateDigitalSignature {
	private static final String SIGNATURE_ALGORITHM     = "SHA256withECDSA";
	private SecureRandom        random;

	public void run(String keyname, String password) throws Exception
	{
		keyname = keyname + ".pem";
		Security.addProvider(new BouncyCastleProvider());

		KeyPair kp = getKeyPair(keyname,password);
		PublicKey publicKey = kp.getPublic(); //"pk" == "public key"
		PrivateKey secretKey = kp.getPrivate(); //"sk" == "secret key" == "private key"

		
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initSign(secretKey, random);
		String messageStr1 = "Pay 3 bitcoins to Alice";
		byte[] message1 = messageStr1.getBytes(StandardCharsets.UTF_8);
		signature.update(message1);
		byte[] sigBytes1 = signature.sign();
		System.out.println("Signature: msg=" + messageStr1 + " sig.len=" + sigBytes1.length + " sig=" + DatatypeConverter.printHexBinary(sigBytes1));

		
		signature.initVerify(publicKey);
		signature.update(message1);

		if (signature.verify(sigBytes1)) {
			System.out.println("SUCCESS: signature verification succeeded.");
		} else {
			System.out.println("FAILURE: signature verification failed.");
		}

		
	}
	
	public static KeyPair getKeyPair(String filename, String password) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, PKCSException, OperatorCreationException {
		File secretKeyFile = new File(filename); // private key file in PEM format
		PEMParser pemParser = new PEMParser(new FileReader(secretKeyFile));
		Object object = pemParser.readObject();
		pemParser.close();
		PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
		JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
		KeyPair kp = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
		return kp;
	}
	
	
	public static void main(String[] args) throws Exception {
		new GenerateDigitalSignature().run("scrooge_sk", "scroogepass@123");
	}
}

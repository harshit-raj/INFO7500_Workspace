package hw2;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMEncryptor;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;

public class GenerateScroogeKeyPair {
	
	private static final String KEY_ALGORITHM           = "ECDSA";
	private static final String PROVIDER                = "BC";
	private static final String CURVE_NAME              = "secp256k1";
	
	
	private ECGenParameterSpec  ecGenSpec;
	private KeyPairGenerator    keyGen_;
	private SecureRandom        random;

	
	public void run(String keyname, String password) throws Exception
	{
		Security.addProvider(new BouncyCastleProvider());

		random = SecureRandom.getInstanceStrong();
		ecGenSpec = new ECGenParameterSpec(CURVE_NAME);
		keyGen_ = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER);

		keyGen_.initialize(ecGenSpec, random);

		KeyPair kp = keyGen_.generateKeyPair();
		//KeyPair kp = getKeyPair(filename, password)
		PublicKey publicKey = kp.getPublic(); //"pk" == "public key"
		PrivateKey secretKey = kp.getPrivate(); //"sk" == "secret key" == "private key"

		{
			String pkFilename = keyname + "_pk.pem";

			StringWriter sw = new StringWriter();
			JcaPEMWriter wr = new JcaPEMWriter(sw);
			wr.writeObject(publicKey);
			wr.close();
			Writer fw = new FileWriter(pkFilename);
			fw.write(sw.toString());
			fw.close();
			System.out.println("Public Key:\n" + sw.toString());
		}

		String skFilename = keyname + "_sk.pem";

		storeSecretKeyToEncrypted(secretKey, skFilename, password);
		PrivateKey recoveredKey = loadSecretKeyFromEncrypted(skFilename, password);

		System.out.println("secretKey=" + secretKey);
		System.out.println("secretKey.getAlgorithm()=" + secretKey.getAlgorithm());
		System.out.println("recoveredKey=" + secretKey);
		System.out.println("recoveredKey.getAlgorithm()=" + recoveredKey.getAlgorithm());
		System.out.println();

		if(secretKey.equals(recoveredKey))
			System.out.println("Key recovery ok");
		else
			System.err.println("Private key recovery failed");

		if(secretKey.getAlgorithm().equals(recoveredKey.getAlgorithm()))
			System.out.println("Key algorithm ok");
		else
			System.err.println("Key algorithms do not match");

	}

	
	public String storeSecretKeyToEncrypted(PrivateKey sk, String filename, String password) throws Exception {
		JcaPEMWriter privWriter = new JcaPEMWriter(new FileWriter(filename));
		PEMEncryptor penc = (new JcePEMEncryptorBuilder("AES-256-CFB"))
					.build(password.toCharArray());
		privWriter.writeObject(sk, penc);
		privWriter.close();
		return null;
	}
	public static PrivateKey loadSecretKeyFromEncrypted(String filename, String password) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, PKCSException, OperatorCreationException {
		File secretKeyFile = new File(filename); // private key file in PEM format
		PEMParser pemParser = new PEMParser(new FileReader(secretKeyFile));
		Object object = pemParser.readObject();
		pemParser.close();
		PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
		JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
		KeyPair kp = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
		return kp.getPrivate();
	}
	
	

	public static void main(String[] args) throws Exception {
		//Generate ECDSA keys for Scrooge
		//Set the keyname to "scrooge".
		
		new GenerateScroogeKeyPair().run("scrooge", "scroogepass@123");
		
			
	}
}

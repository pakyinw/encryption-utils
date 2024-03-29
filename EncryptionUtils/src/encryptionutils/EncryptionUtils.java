package encryptionutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.binary.Base64;

public class EncryptionUtils {
	int BLOCK_SIZE = 16;
	int KEY_LENGTH = 128;
	
	public String encodeToBase64String(byte[] bArr){
		Base64 base64 = new Base64();
		return base64.encodeToString(bArr);
	}
	
	public byte[] decodeBase64String(String str){
		Base64 base64 = new Base64();
		return base64.decode(str);
	}
	
	public SecretKey genSecretKey() throws NoSuchAlgorithmException{
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(KEY_LENGTH , new SecureRandom());
		SecretKey secretKey = keyGen.generateKey();
		return secretKey;
	}
	
	public byte[] genIV(){
		byte[] iv = new byte[128 / 8]; 
		SecureRandom prng = new SecureRandom();
		prng.nextBytes(iv);
		return iv;
	}
	
	public File getPlainTextFile(String path){
		File file = new File(path);
		return file;
	}
	
	public void encrypt(SecretKey secretKey, byte[] iv, File plainTextFile, File encryptedFile) throws Exception{
		Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5PADDING"); 
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));    
		byte buf[] = new byte[4096];
		try (
				InputStream in = new FileInputStream(plainTextFile);
				OutputStream out = new FileOutputStream(encryptedFile);
			){
				int readBytes = in.read(buf);   
				while(readBytes > 0){
				byte[] cipherBytes = cipher.update(buf, 0 , readBytes);
				out.write(cipherBytes);
				readBytes = in.read(buf);
			}
			cipher.doFinal();
		}
	}
 
	public void decrypt(SecretKey secretKey, byte[] iv, File cipherTextFile, File decryptedFile) throws Exception{
		Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5PADDING"); 
		cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));    
		if(!decryptedFile.exists()){
			decryptedFile.createNewFile(); //: Here, it may be fail if ...
		}
		  
		byte buf[] = new byte[4096];
		try (
				InputStream in = new FileInputStream(cipherTextFile);
			    OutputStream out = new FileOutputStream(decryptedFile);
			)
		{
			int readBytes = in.read(buf);   
			while(readBytes > 0){
				byte[] decryptedBytes = cipher.update(buf, 0 , readBytes);
				out.write(decryptedBytes);
				readBytes = in.read(buf);
			}
			cipher.doFinal();
		}
	}
		  
	public byte[] DecryptPartial(SecretKey secretKey, byte[] iv, File cipherTextFile, int blockIndex, int blockCount ) throws Exception{
		final int offset = blockIndex * BLOCK_SIZE;
		final int bufSize = blockCount * BLOCK_SIZE;

		Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5PADDING"); 
		cipher.init(Cipher.DECRYPT_MODE, secretKey, calculateIVForBlock(new IvParameterSpec(iv), blockIndex ));

		byte[] decryptedBytes = new byte[bufSize];
		try (FileInputStream in = new FileInputStream(cipherTextFile)){
			byte inputBuf[] = new byte[bufSize];
			in.skip(offset);
			int readBytes = in.read(inputBuf);
			decryptedBytes = cipher.update(inputBuf, 0, readBytes);
		}
		return decryptedBytes;
	} 

	public IvParameterSpec calculateIVForBlock(final IvParameterSpec iv, final long blockIndex) {  
		final BigInteger biginIV = new BigInteger(1, iv.getIV());
		final BigInteger blockIV = biginIV.add(BigInteger.valueOf(blockIndex));
		final byte[] blockIVBytes = blockIV.toByteArray();

		// Normalize the blockIVBytes as 16 bytes for IV
		if(blockIVBytes.length == BLOCK_SIZE){
			return new IvParameterSpec(blockIVBytes);
		}
		
		if(blockIVBytes.length > BLOCK_SIZE ){
			// For example: if the blockIVBytes length is 18, blockIVBytes is [0],[1],...[16],[17]
		    // We have to remove [0],[1] , so we change the offset = 2
		    int offset = blockIVBytes.length - BLOCK_SIZE;
		    return new IvParameterSpec(blockIVBytes, offset, BLOCK_SIZE);
		}else{
		    // For example: if the blockIVBytes length is 14, blockIVBytes is [0],[1],...[12],[13]
		    // We have to insert 2 bytes at head
		    final byte[] newBlockIV = new byte[BLOCK_SIZE]; //: default set to 0 for 16 bytes
		    int offset = blockIVBytes.length - BLOCK_SIZE;
		    System.arraycopy(blockIVBytes, 0, newBlockIV, offset, blockIVBytes.length);
		    return new IvParameterSpec(newBlockIV);
		}
	}
		 
	public void createTestFile(String path) throws Exception{
		File test = new File(path);  
		try(FileOutputStream out = new FileOutputStream(test)){
			StringBuffer buf = new StringBuffer(16);

			int blockCount = 100000;
			for(int i = 0 ; i < blockCount ; i ++){
				buf.append(i);
				int size = buf.length();
				for(int j = 0; j < (14-size); j++ ){
					buf.append('#');
				}
				out.write(buf.toString().getBytes());
				out.write("\r\n".getBytes());
				buf.delete(0, 16);
		   }   
		}  
	}
}

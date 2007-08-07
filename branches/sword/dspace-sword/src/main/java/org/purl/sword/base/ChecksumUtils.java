package org.purl.sword.base;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class that holds Checksum related methods. 
 * 
 * @author Neil Taylor
 */
public class ChecksumUtils
{
   /**
    * Generate an MD5 hash for the file that is specified in the 
    * filepath. The hash is returned as a String representation. 
    * 
    * @param filepath The path to the file to load. 
    * @return A string hash of the file. 
    * @throws NoSuchAlgorithmException If the MD5 algorithm is 
    * not supported by the installed virtual machine. 
    * 
    * @throws IOException If there is an error accessing the file. 
    */
   public static String generateMD5(String filepath)
   throws NoSuchAlgorithmException, IOException 
   {
      FileInputStream md5Stream = null; 
      String md5 = null; 
      
      try
      {
         md5Stream = new FileInputStream(filepath);

         MessageDigest md = MessageDigest.getInstance("MD5");
         md.reset();
         
         byte[] bytes = new byte[1024];
         int count = 0; 
         while( (count = md5Stream.read(bytes)) != -1 )
         {
            md.update(bytes, 0, count);
         }

         byte[] md5Digest = md.digest();

         StringBuffer buffer = new StringBuffer(); 
         for( byte b : md5Digest )
         {
            // 0xFF is used to handle the issue of negative numbers in the bytes
            String hex = Integer.toHexString(b & 0xFF);
            if( hex.length() == 1 )
            {
               buffer.append("0");
            }
            buffer.append(hex);
         }

         md5 = buffer.toString();
      }
      catch(NoSuchAlgorithmException ex )
      {
          InfoLogger.getLogger().writeInfo("Error accessing string");
          throw ex; // rethrow 
      }
      finally
      {
         if( md5Stream != null )
         {
            md5Stream.close();
         }
      }
      
      System.out.println("the string is: " + md5);
      return md5; 
   }
   
   public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
	   System.out.println(ChecksumUtils.generateMD5(args[0]));
   }
}

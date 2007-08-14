package org.purl.sword.base;

import java.io.File;

import org.apache.commons.fileupload.FileItem;

/**
 * Represents a deposit. 
 * 
 * @author Stuart Lewis 
 * 
 */
public class Deposit 
{
   
   /** The File deposited */
   private FileItem file;
   
   private String contentType;
   
   private int contentLength;
   
   private String username;
   
   private String password;
   
   private String onBehalfOf;
   
   private String slug;
   
   private String md5;
   
   private boolean verbose;
   
   private boolean noOp;
   
   private String formatNamespace;
   
   private String depositID;
   
   private String IPAddress;

   /**
    * @return the authenticatedUserName
    */
   public String getUsername() {
      return username;
   }

   /**
    * @param authenticatedUserName the authenticatedUserName to set
    */
   public void setUsername(String username) {
      this.username = username;
   }

   /**
    * @return the authenticatedUserPassword
    */
   public String getPassword() {
      return password;
   }

   /**
    * @param password the password to set
    */
   public void setPassword(String password) {
      this.password = password;
   }

   /**
    * @return the contentLength
    */
   public int getContentLength() {
      return contentLength;
   }

   /**
    * @param contentLength the contentLength to set
    */
   public void setContentLength(int contentLength) {
      this.contentLength = contentLength;
   }

   /**
    * @return the contentType
    */
   public String getContentType() {
      return contentType;
   }

   /**
    * @param contentType the contentType to set
    */
   public void setContentType(String contentType) {
      this.contentType = contentType;
   }

   /**
    * @return the depositID
    */
   public String getDepositID() {
      return depositID;
   }

   /**
    * @param depositID the depositID to set
    */
   public void setDepositID(String depositID) {
      this.depositID = depositID;
   }

   /**
    * @return the file
    */
   public FileItem getFile() {
      return file;
   }

   /**
    * @param file the file to set
    */
   public void setFile(FileItem file) {
      this.file = file;
   }

   /**
    * @return the formatNamespace
    */
   public String getFormatNamespace() {
      return formatNamespace;
   }

   /**
    * @param formatNamespace the formatNamespace to set
    */
   public void setFormatNamespace(String formatNamespace) {
      this.formatNamespace = formatNamespace;
   }

   /**
    * @return the md5
    */
   public String getMd5() {
      return md5;
   }

   /**
    * @param md5 the md5 to set
    */
   public void setMd5(String md5) {
      this.md5 = md5;
   }

   /**
    * @return the noOp
    */
   public boolean isNoOp() {
      return noOp;
   }

   /**
    * @param noOp the noOp to set
    */
   public void setNoOp(boolean noOp) {
      this.noOp = noOp;
   }

   /**
    * @return the onBehalfOf
    */
   public String getOnBehalfOf() {
      return onBehalfOf;
   }

   /**
    * @param onBehalfOf the onBehalfOf to set
    */
   public void setOnBehalfOf(String onBehalfOf) {
      this.onBehalfOf = onBehalfOf;
   }

   /**
    * @return the slug
    */
   public String getSlug() {
      return slug;
   }

   /**
    * @param slug the slug to set
    */
   public void setSlug(String slug) {
      this.slug = slug;
   }

   /**
    * @return the verbose
    */
   public boolean isVerbose() {
      return verbose;
   }

   /**
    * @param verbose the verbose to set
    */
   public void setVerbose(boolean verbose) {
      this.verbose = verbose;
   }
   
   /**
    * Get the IP address of the user
    * 
    * @return the the IP address
    */
   public String getIPAddress() {
	   return IPAddress;
   }
   
   /**
    * Set the IP address of the user
    *
    * @param String the IP address
    */
   public void setIPAddress(String IPAddress) {
	   this.IPAddress = IPAddress;
   }
}

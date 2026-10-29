package com.pocketmusic.android;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Account material stays in no-backup internal storage, encrypted with a device key. */
final class YouTubeSession {
 private static final String ALIAS="PocketMusic.YouTube.v1";
 private static File saved(Context c){return new File(c.getNoBackupFilesDir(),"youtube-session.bin");}
 static boolean connected(Context c){return saved(c).isFile();}
 // Presentation metadata must not invalidate streams or another account's feed scope.
 private static final java.util.LinkedHashMap<String,String[]> scopes=new java.util.LinkedHashMap<String,String[]>(16,.75f,true){protected boolean removeEldestEntry(java.util.Map.Entry<String,String[]> entry){return size()>16;}};
 private static String hash(byte[] bytes)throws Exception{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder result=new StringBuilder();for(byte b:digest)result.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return result.toString();}
 static String revision(Context c){if(!connected(c))return "guest";try{return hash(Files.readAllBytes(saved(c).toPath()));}catch(Exception error){return "unreadable";}}
 static synchronized String fingerprint(Context c){if(!connected(c))return "guest";try{byte[] stored=Files.readAllBytes(saved(c).toPath());String cipherHash=hash(stored),path=saved(c).getAbsolutePath();String[] cached=scopes.get(path);if(cached!=null&&cached[0].equals(cipherHash))return cached[1];byte[] plain=decryptBytes(stored);SessionAuth auth;try{auth=SessionAuth.decode(new String(plain,StandardCharsets.UTF_8));}finally{java.util.Arrays.fill(plain,(byte)0);}byte[] material=(YouTubeCookies.netscape(auth.cookie)+"\u0000"+auth.authUser+"\u0000"+auth.dataSyncId+"\u0000"+auth.visitorData).getBytes(StandardCharsets.UTF_8);String scope;try{scope=hash(material);}finally{java.util.Arrays.fill(material,(byte)0);}scopes.put(path,new String[]{cipherHash,scope});return scope;}catch(Exception error){return "unreadable";}}
 private static javax.crypto.SecretKey key() throws Exception {
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(store.containsAlias(ALIAS))return (javax.crypto.SecretKey)store.getKey(ALIAS,null);
  KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return generator.generateKey();
 }
 private static byte[] decrypt(Context c)throws Exception{return decryptBytes(Files.readAllBytes(saved(c).toPath()));}
 private static byte[] decryptBytes(byte[] stored)throws Exception{if(stored.length<30||stored.length>131072||stored[0]!=12)throw new IOException("Reconnect YouTube from the menu.");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,stored,1,12));return cipher.doFinal(stored,13,stored.length-13);}
 static synchronized SessionAuth read(Context c)throws Exception{if(!connected(c))return null;byte[] plain=decrypt(c);try{return SessionAuth.decode(new String(plain,StandardCharsets.UTF_8));}finally{java.util.Arrays.fill(plain,(byte)0);}}
 private static final class Guard implements AutoCloseable {private final RandomAccessFile file;private final java.nio.channels.FileLock lock;Guard(Context c)throws Exception{file=new RandomAccessFile(new File(c.getNoBackupFilesDir(),"youtube-session.lock"),"rw");try{lock=file.getChannel().lock();}catch(Exception error){file.close();throw error;}}public void close()throws IOException{try{lock.release();}finally{file.close();}}}
 private static void writeEncrypted(Context c,byte[] bytes)throws Exception{AtomicFile target=new AtomicFile(saved(c));FileOutputStream stream=target.startWrite();try{stream.write(bytes);target.finishWrite(stream);}catch(Exception e){target.failWrite(stream);throw e;}}
 private static void writePlain(Context c,byte[] plain)throws Exception{try{if(plain.length>120000)throw new IOException("Account session is too large to store safely.");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());byte[] encrypted=cipher.doFinal(plain);ByteArrayOutputStream bytes=new ByteArrayOutputStream();bytes.write(cipher.getIV().length);bytes.write(cipher.getIV());bytes.write(encrypted);writeEncrypted(c,bytes.toByteArray());}finally{java.util.Arrays.fill(plain,(byte)0);}}
 static synchronized void save(Context c,String header)throws Exception{try(Guard ignored=new Guard(c)){writePlain(c,YouTubeCookies.netscape(header).getBytes(StandardCharsets.UTF_8));}}
 static synchronized void save(Context c,SessionAuth auth)throws Exception{try(Guard ignored=new Guard(c)){saveUnlocked(c,auth);}}
 static synchronized String updateProfile(Context c,String expected,SessionAuth auth)throws Exception{try(Guard ignored=new Guard(c)){if(!expected.equals(fingerprint(c)))return null;SessionAuth current=read(c);if(current==null||!YouTubeCookies.netscape(current.cookie).equals(YouTubeCookies.netscape(auth.cookie))||!current.authUser.equals(auth.authUser)||!current.dataSyncId.equals(auth.dataSyncId))return null;if(!current.sameIdentity(auth))saveUnlocked(c,auth);return fingerprint(c);}}
 private static void saveUnlocked(Context c,SessionAuth auth)throws Exception{
  if(!auth.complete()||auth.accountName.isBlank())throw new IOException("Account validation is required before saving.");byte[] previous=connected(c)?Files.readAllBytes(saved(c).toPath()):null;
  try{writePlain(c,auth.encode().getBytes(StandardCharsets.UTF_8));if(!auth.sameIdentity(read(c)))throw new IOException("Saved account could not be read back.");}catch(Exception failure){try{if(previous!=null)writeEncrypted(c,previous);else new AtomicFile(saved(c)).delete();}catch(Exception recovery){failure.addSuppressed(recovery);}throw failure;}
 }
 static synchronized File lease(Context c)throws Exception{
  SessionAuth auth=read(c);if(auth==null)return null;byte[] plain=YouTubeCookies.netscape(auth.cookie).getBytes(StandardCharsets.UTF_8);File file=File.createTempFile("youtube-request-"+android.os.Process.myPid()+"-",".txt",c.getCacheDir());try{Files.write(file.toPath(),plain);return file;}catch(Exception e){file.delete();throw e;}finally{java.util.Arrays.fill(plain,(byte)0);}
 }
 static String label(Context c){try{SessionAuth auth=read(c);return auth!=null&&!auth.accountName.isBlank()?"Connected as "+auth.accountName:"Cookie session only. Reconnect to verify and complete your account connection.";}catch(Exception error){return "Saved account could not be read. Reconnect YouTube.";}}
 static synchronized boolean disconnect(Context c){try(Guard ignored=new Guard(c)){new AtomicFile(saved(c)).delete();return !connected(c);}catch(Exception error){return false;}}
 static void cleanAbandoned(Context c){String live="youtube-request-"+android.os.Process.myPid()+"-";File[] files=c.getCacheDir().listFiles((d,n)->n.startsWith("youtube-request-")&&!n.startsWith(live)&&n.endsWith(".txt"));if(files!=null)for(File f:files)f.delete();}
}

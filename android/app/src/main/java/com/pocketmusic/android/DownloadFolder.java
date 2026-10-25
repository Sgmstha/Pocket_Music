package com.pocketmusic.android;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** User-owned SAF destination. App audio stays available if the provider goes offline. */
final class DownloadFolder {
    static final String KEY="downloadFolder";
    static Uri selected(Context c){String value=c.getSharedPreferences("music",0).getString(KEY,"");return value.isEmpty()?null:Uri.parse(value);}
    static String label(Context c){return c.getSharedPreferences("music",0).getString("downloadFolderLabel","Choose a download folder");}
    static boolean hasAccess(Context c){
        Uri tree=selected(c);if(tree==null)return false;
        for(UriPermission p:c.getContentResolver().getPersistedUriPermissions())if(p.getUri().equals(tree)&&p.isWritePermission()&&p.isReadPermission())return true;
        return false;
    }
    static void remember(Context c,Uri tree,int flags)throws IOException{
        if(tree==null||!DocumentsContract.isTreeUri(tree))throw new IOException("Choose a folder, not a file.");
        int grant=flags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if(grant!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))throw new IOException("Read and write folder access is required.");
        c.getContentResolver().takePersistableUriPermission(tree,grant);
        Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        String name="Selected folder";
        try(Cursor cursor=c.getContentResolver().query(root,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
            if(cursor==null||!cursor.moveToFirst())throw new IOException("This folder is unavailable.");
            name=cursor.getString(0);
        }
        c.getSharedPreferences("music",0).edit().putString(KEY,tree.toString()).putString("downloadFolderLabel",name).apply();
    }
    private static String key(Uri tree,File f)throws Exception{
        byte[] digest=MessageDigest.getInstance("SHA-256").digest((tree+"\n"+f.getName()+"\n"+f.length()+"\n"+f.lastModified()).getBytes(StandardCharsets.UTF_8));
        StringBuilder key=new StringBuilder("saved.");for(byte b:digest)key.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return key.toString();
    }
    static Uri savedUri(Context c,File file){
        try{Uri tree=selected(c);if(tree==null)return null;String value=c.getSharedPreferences("music",0).getString(key(tree,file),"");return value.isEmpty()?null:Uri.parse(value);}catch(Exception ignored){return null;}
    }
    static synchronized Uri save(Context c,File file)throws Exception{
        if(!file.isFile()||file.length()==0)throw new IOException("The completed download could not be found.");
        if(!hasAccess(c))throw new IOException("Choose your download folder again in Settings.");
        Uri tree=selected(c);String key=key(tree,file);
        String previous=c.getSharedPreferences("music",0).getString(key,"");
        ContentResolver resolver=c.getContentResolver();
        if(!previous.isEmpty()){
            Uri existing=Uri.parse(previous);
            try(Cursor cursor=resolver.query(existing,new String[]{DocumentsContract.Document.COLUMN_SIZE},null,null,null)){
                if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0)&&cursor.getLong(0)==file.length())return existing;
            }catch(IllegalArgumentException ignored){}
        }
        Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        Uri output=DocumentsContract.createDocument(resolver,root,mime(file),file.getName());
        if(output==null)throw new IOException("This folder could not create a file.");
        boolean complete=false;
        try{
            try(InputStream in=new FileInputStream(file);OutputStream out=resolver.openOutputStream(output,"wt")){
                if(out==null)throw new IOException("This folder is not writable.");
                byte[] buffer=new byte[32768];int count;long total=0;
                while((count=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Saving cancelled");out.write(buffer,0,count);total+=count;}
                out.flush();if(total!=file.length())throw new IOException("The saved file is incomplete.");
            }
            complete=true;c.getSharedPreferences("music",0).edit().putString(key,output.toString()).apply();return output;
        }finally{if(!complete)try{DocumentsContract.deleteDocument(resolver,output);}catch(Exception ignored){}}
    }
    static String mime(File f){String n=f.getName().toLowerCase(java.util.Locale.ROOT);return n.endsWith(".mp4")?"video/mp4":n.endsWith(".wav")?"audio/wav":n.endsWith(".flac")?"audio/flac":n.endsWith(".m4a")?"audio/mp4":n.endsWith(".opus")?"audio/ogg":"audio/mpeg";}
}

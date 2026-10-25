package com.pocketmusic.android;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import static org.junit.Assert.*;

/** Opt-in reproducible visual fixtures. Neither fixtures nor screenshots ship in the app. */
public class ReplicaScreensTest {
 private Instrumentation instrumentation;private Context context;private final List<File> fixtures=new ArrayList<>();private Activity activity;
 @Test public void captureScopedScreens()throws Exception{
  Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("captureReplica")));
  instrumentation=InstrumentationRegistry.getInstrumentation();context=instrumentation.getTargetContext();
  try{
   String[] names={"Blue hour.wav","Open windows.wav","Quiet roads.wav","Soft piano.wav"};int samples=22050*30;
   for(String name:names){File f=new File(MusicRepository.folder(context),name);assertFalse("Refuse to overwrite existing user file",f.exists());ByteBuffer b=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes()).putInt(36+samples*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(22050).putInt(44100).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(samples*2);try(FileOutputStream out=new FileOutputStream(f)){out.write(b.array());}fixtures.add(f);}
   activity=instrumentation.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));Thread.sleep(600);capture("S01");
   clickText("Search");capture("S02-empty");AccessibilityNodeInfo field=description(root(),"Search songs, artists or a YouTube link");assertNotNull(field);Bundle text=new Bundle();text.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"https://www.youtube.com/watch?v=M7lc1UVf-VE");assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,text));click(description(root(),"Run search"));Thread.sleep(800);capture("S02");
   clickText("Library");capture("S03");clickText("Blue hour");long deadline=System.currentTimeMillis()+7000;while((LocalPlayerService.active==null||!LocalPlayerService.active.playing())&&System.currentTimeMillis()<deadline)Thread.sleep(100);assertNotNull(LocalPlayerService.active);instrumentation.runOnMainSync(()->LocalPlayerService.active.pause());Thread.sleep(1100);click(description(root(),"Open now playing"));capture("S04");
  }finally{if(context!=null)instrumentation.runOnMainSync(()->{context.stopService(new Intent(context,LocalPlayerService.class));if(activity!=null)activity.finish();});for(File f:fixtures)f.delete();}
 }
 private void capture(String name)throws Exception{Thread.sleep(400);Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(bitmap);File folder=new File(context.getExternalFilesDir(null),"replica");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();}
 private AccessibilityNodeInfo root(){return instrumentation.getUiAutomation().getRootInActiveWindow();}
 private AccessibilityNodeInfo description(AccessibilityNodeInfo n,String value){if(n==null)return null;if(value.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=description(n.getChild(i),value);if(found!=null)return found;}return null;}
 private void clickText(String value)throws Exception{AccessibilityNodeInfo r=root();assertNotNull(r);for(AccessibilityNodeInfo n:r.findAccessibilityNodeInfosByText(value))if(value.contentEquals(n.getText()==null?"":n.getText())){click(n);return;}fail("Missing UI text "+value);}
 private void click(AccessibilityNodeInfo n)throws Exception{assertNotNull(n);while(!n.isClickable()&&n.getParent()!=null)n=n.getParent();assertTrue(n.performAction(AccessibilityNodeInfo.ACTION_CLICK));Thread.sleep(500);}
}

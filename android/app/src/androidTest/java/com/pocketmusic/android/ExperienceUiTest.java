package com.pocketmusic.android;

import android.app.*;
import android.content.*;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import static org.junit.Assert.*;

public class ExperienceUiTest {
 private Instrumentation instrumentation;private Context context;private File fixture;private Activity activity;
 @Before public void setup()throws Exception{
  instrumentation=InstrumentationRegistry.getInstrumentation();context=instrumentation.getTargetContext();
  int samples=22050*30;ByteBuffer b=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN);
  b.put("RIFF".getBytes()).putInt(36+samples*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(22050).putInt(44100).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(samples*2);
  fixture=new File(MusicRepository.folder(context),"UI Journey.wav");try(FileOutputStream out=new FileOutputStream(fixture)){out.write(b.array());}
  instrumentation.runOnMainSync(()->context.stopService(new Intent(context,LocalPlayerService.class)));instrumentation.waitForIdleSync();
  activity=instrumentation.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
 }
 @After public void cleanup(){instrumentation.runOnMainSync(()->{context.stopService(new Intent(context,LocalPlayerService.class));if(activity!=null)activity.finish();});if(fixture!=null)fixture.delete();}
 @Test public void homeSearchLibraryAndPlayerHaveSeparateFlows()throws Exception{
  awaitText("Find your flow");assertNotNull(findText("Jump back in"));assertNull(findText("Song, artist or YouTube link"));assertNull(findText("Import"));
  click("Search");awaitText("Try something new");assertNotNull(findDescription("Search songs, artists or a YouTube link"));
  click("Library");awaitText("Your library");assertNotNull(findText("UI Journey"));assertNull(findText("Play local copy"));assertNull(findText("Export"));
  click("UI Journey");long limit=System.currentTimeMillis()+7000;while((LocalPlayerService.active==null||!LocalPlayerService.active.playing())&&System.currentTimeMillis()<limit)Thread.sleep(100);assertNotNull(LocalPlayerService.active);assertTrue(LocalPlayerService.active.playing());
  long uiLimit=System.currentTimeMillis()+4000;while(findDescription("Open now playing")==null&&System.currentTimeMillis()<uiLimit)Thread.sleep(100);clickNode(findDescription("Open now playing"));awaitText("NOW PLAYING");assertNotNull(findText("Shuffle"));assertNotNull(findText("Repeat"));assertNotNull(findText("Queue"));click("Close");
  clickNode(findDescription("More options"));awaitText("Import audio");assertNotNull(findText("Settings & download folder"));
 }
 private AccessibilityNodeInfo root(){return instrumentation.getUiAutomation().getRootInActiveWindow();}
 private AccessibilityNodeInfo findText(String text){AccessibilityNodeInfo r=root();if(r==null)return null;for(AccessibilityNodeInfo n:r.findAccessibilityNodeInfosByText(text))if(text.contentEquals(n.getText()==null?"":n.getText()))return n;return null;}
 private AccessibilityNodeInfo findDescription(String value){return description(root(),value);}
 private AccessibilityNodeInfo description(AccessibilityNodeInfo n,String value){if(n==null)return null;if(value.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=description(n.getChild(i),value);if(child!=null)return child;}return null;}
 private void awaitText(String value)throws Exception{long limit=System.currentTimeMillis()+7000;while(findText(value)==null&&System.currentTimeMillis()<limit)Thread.sleep(100);assertNotNull("Expected UI text: "+value,findText(value));}
 private void click(String text)throws Exception{awaitText(text);clickNode(findText(text));}
 private void clickNode(AccessibilityNodeInfo node)throws Exception{assertNotNull(node);while(!node.isClickable()&&node.getParent()!=null)node=node.getParent();assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK));Thread.sleep(400);}
}

package com.pocketmusic.android;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.webkit.WebViewAssetLoader;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private LinearLayout content,results,preview,previewMedia;private TextView status,now,time;private EditText query;private Button search,toggle,modeAudio,modeVideo;private SeekBar seek,volume;
 private ImageView thumbnail; private WebView web;private boolean streaming,playing,visible,seeking,videoMode;private String token="";private Track current;private double position,duration;private File exportFile;
 private List<Track> queue=new ArrayList<>();private int queueIndex=-1;private String page="search",lastDownload="";
 private final Runnable tick=new Runnable(){public void run(){if(!visible)return;LocalPlayerService p=LocalPlayerService.active;
  if(!streaming && p!=null){now.setText(p.title);playing=p.playing();position=p.position()/1000.0;duration=p.duration()/1000.0;updateBar();}
  String download=DownloadService.busy?DownloadService.status:getSharedPreferences("music",0).getString("downloadStatus","");
  if(!download.equals(lastDownload)){lastDownload=download;if(!download.isEmpty())status.setText(download);if("library".equals(page) && !DownloadService.busy)showLibrary();}
  handler.postDelayed(this,1000);
 }};
 public void onCreate(Bundle saved){super.onCreate(saved);videoMode=getSharedPreferences("music",0).getBoolean("video",false);
  LinearLayout root=column();root.setBackgroundColor(Color.rgb(8,8,9));root.setPadding(dp(16),dp(8),dp(16),0);setContentView(root);
  root.setOnApplyWindowInsetsListener((v,in)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(dp(16),bars.top+dp(8),dp(16),bars.bottom);}else{v.setPadding(dp(16),in.getSystemWindowInsetTop()+dp(8),dp(16),in.getSystemWindowInsetBottom());}return in;});
  TextView heading=text("Pocket Music",26);root.addView(heading);root.addView(text("ANDROID TEST · YOUR MUSIC, YOUR FILES",11));
  LinearLayout tabs=row();tabs.addView(button("Search",()->{page="search";showResults(queue);}),weight());tabs.addView(button("Library",()->{page="library";showLibrary();}),weight());tabs.addView(button("Import",this::importAudio),weight());root.addView(tabs);
  LinearLayout input=row();query=new EditText(this);query.setTextColor(Color.WHITE);query.setTextSize(15);query.setSingleLine(true);query.setHint("Song, artist or YouTube link");query.setHintTextColor(Color.LTGRAY);input.addView(query,weight());search=button("Search",this::startSearch);input.addView(search);root.addView(input);
  status=text("Search or paste a YouTube link. Import audio to listen offline.",13);status.setPadding(0,dp(8),0,dp(8));root.addView(status);
  ScrollView scroll=new ScrollView(this);content=column();scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  preview=column();preview.setVisibility(View.GONE);preview.setPadding(dp(8),dp(8),dp(8),dp(8));preview.setBackgroundColor(Color.rgb(23,19,29));content.addView(preview);
  LinearLayout modes=row();modeAudio=button("Audio",()->setMode(false));modeVideo=button("Video",()->setMode(true));modes.addView(modeAudio,weight());modes.addView(modeVideo,weight());modes.addView(button("Close",this::closePreview),weight());preview.addView(modes);previewMedia=row();preview.addView(previewMedia);
  results=column();content.addView(results);
  LinearLayout bar=column();bar.setPadding(dp(8),dp(6),dp(8),dp(6));bar.setBackgroundColor(Color.rgb(23,19,29));root.addView(bar);now=text("Nothing playing",14);now.setMaxLines(1);now.setEllipsize(android.text.TextUtils.TruncateAt.END);bar.addView(now);
  LinearLayout controls=row();controls.addView(button("Previous",()->step(-1)),weight());toggle=button("Play",this::togglePlay);controls.addView(toggle,weight());controls.addView(button("Next",()->step(1)),weight());bar.addView(controls);
  seek=new SeekBar(this);seek.setMax(1000);seek.setContentDescription("Playback position");bar.addView(seek);time=text("0:00 / 0:00",12);bar.addView(time);
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){seeking=true;}public void onStopTrackingTouch(SeekBar s){double n=duration*s.getProgress()/1000; if(streaming)command("seek",n);else if(LocalPlayerService.active!=null)LocalPlayerService.active.seek((int)(n*1000));seeking=false;}public void onProgressChanged(SeekBar s,int p,boolean u){}});
  LinearLayout vol=row();vol.addView(text("Volume",12));volume=new SeekBar(this);volume.setMax(100);volume.setProgress(70);volume.setContentDescription("Playback volume");vol.addView(volume,weight());bar.addView(vol);
  volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int v,boolean user){if(user){if(streaming)command("volume",v);else if(LocalPlayerService.active!=null)LocalPlayerService.active.volume(v/100f);}}});
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},3);
  if(Intent.ACTION_SEND.equals(getIntent().getAction()))query.setText(getIntent().getStringExtra(Intent.EXTRA_TEXT));
 }
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1);}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.rgb(230,225,235));t.setTextSize(size);return t;}
 private Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setTextSize(12);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());return b;}
 private void startSearch(){String q=query.getText().toString().trim();search.setEnabled(false);status.setText("Searching YouTube…");page="search";worker.execute(()->{try{List<Track> found=MusicRepository.search(this,q);runOnUiThread(()->{if(isDestroyed())return;queue=found;showResults(found);status.setText(found.isEmpty()?"No results. Try another search.":found.size()+" results · preview or download");});}catch(Exception e){runOnUiThread(()->status.setText("Search failed: "+DownloadService.message(e)));}finally{runOnUiThread(()->search.setEnabled(true));}});}
 private void showResults(List<Track> tracks){results.removeAllViews();for(Track t:tracks){LinearLayout card=column();card.setPadding(0,dp(12),0,dp(12));card.addView(text(t.title,17));LinearLayout actions=row();actions.addView(button("Preview",()->startPreview(t)),weight());actions.addView(button("Download",()->chooseDownload(t)),weight());card.addView(actions);Button local=button("Play local copy",()->{File f=MusicRepository.local(this,t.id);if(f!=null)playLocal(f);else status.setText("Local copy is no longer available.");});local.setEnabled(MusicRepository.local(this,t.id)!=null);card.addView(local);results.addView(card);}}
 private void chooseDownload(Track t){if(DownloadService.busy){status.setText("A download is already running. Cancel it from its notification first.");return;}
  LinearLayout box=column();box.setPadding(dp(20),dp(8),dp(20),0);box.addView(text(t.title,16));Spinner formats=new Spinner(this);formats.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"MP3 audio","MP4 video (up to 720p)"}));box.addView(formats);CheckBox consent=new CheckBox(this);consent.setText("I own this content or have permission to download it.");box.addView(consent);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Download format").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Download",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{if(!consent.isChecked()){consent.setError("Confirm permission first");return;}startForegroundService(new Intent(this,DownloadService.class).putExtra("id",t.id).putExtra("format",formats.getSelectedItemPosition()==0?"mp3":"mp4"));status.setText("Starting download…");dialog.dismiss();}));dialog.show();
 }
 private void showLibrary(){results.removeAllViews();List<File> files=MusicRepository.files(this);if(files.isEmpty())results.addView(text("No saved tracks yet. Download or import audio. Files are kept in this app until you Export them.",15));for(File f:files){results.addView(text(f.getName(),15));LinearLayout actions=row();Button play=button("Play local copy",()->playLocal(f));play.setEnabled(!f.getName().endsWith(".mp4"));actions.addView(play,weight());actions.addView(button("Export",()->export(f)),weight());results.addView(actions);}}
 private void playLocal(File f){closePreview();startForegroundService(new Intent(this,LocalPlayerService.class).setAction("open").putExtra("path",f.getAbsolutePath()));now.setText(f.getName());status.setText("Local audio · background playback enabled");}
 private void startPreview(Track track){stopService(new Intent(this,LocalPlayerService.class));closePreview();current=track;queueIndex=queue.indexOf(track);streaming=true;token=UUID.randomUUID().toString();position=duration=0;playing=false;now.setText(track.title);status.setText("Loading YouTube…");preview.setVisibility(View.VISIBLE);
  web=new WebView(this);web.setBackgroundColor(Color.BLACK);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setMediaPlaybackRequiresUserGesture(false);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);web.addJavascriptInterface(new Bridge(),"Pocket");
  WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
  web.setWebViewClient(new WebViewClient(){public WebResourceResponse shouldInterceptRequest(WebView w,WebResourceRequest r){return loader.shouldInterceptRequest(r.getUrl());}public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){if(!r.isForMainFrame())return false;return !"appassets.androidplatform.net".equals(r.getUrl().getHost());}public void onReceivedError(WebView w,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())status.setText("Preview unavailable. Check your connection.");}});
  thumbnail=new ImageView(this);thumbnail.setContentDescription(track.title);thumbnail.setImageResource(R.drawable.ic_music);previewMedia.addView(thumbnail,new LinearLayout.LayoutParams(dp(72),dp(72)));loadThumbnail(track.id,token);previewMedia.addView(web);setMode(videoMode);web.loadUrl("https://appassets.androidplatform.net/assets/preview.html?id="+track.id+"&token="+token+"&volume="+volume.getProgress());
 }
 private void setMode(boolean video){videoMode=video;getSharedPreferences("music",0).edit().putBoolean("video",video).apply();modeAudio.setEnabled(video);modeVideo.setEnabled(!video);
  if(thumbnail!=null)thumbnail.setVisibility(video?View.GONE:View.VISIBLE);if(web!=null){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(video?-1:dp(200),video?Math.max(dp(200),(getResources().getDisplayMetrics().widthPixels-dp(48))*9/16):dp(200));web.setLayoutParams(p);}
 }
 private void closePreview(){token="";streaming=false;playing=false;if(web!=null){previewMedia.removeView(web);web.stopLoading();web.removeJavascriptInterface("Pocket");web.destroy();web=null;}previewMedia.removeAllViews();thumbnail=null;preview.setVisibility(View.GONE);updateBar();}
 private void command(String action,double value){if(web!=null)web.evaluateJavascript("command("+JSONObject.quote(action)+","+value+")",null);}
 private void togglePlay(){if(streaming)command(playing?"pause":"play",0);else if(LocalPlayerService.active!=null){if(LocalPlayerService.active.playing())LocalPlayerService.active.pause();else LocalPlayerService.active.play();}}
 private void step(int direction){if(streaming){if(direction<0 && position>3){command("seek",0);return;}int next=queueIndex+direction;if(next>=0 && next<queue.size())startPreview(queue.get(next));}
  else if(LocalPlayerService.active!=null){List<File> files=MusicRepository.files(this);files.removeIf(f->f.getName().endsWith(".mp4"));int idx=-1;for(int i=0;i<files.size();i++)if(files.get(i).getName().equals(LocalPlayerService.active.title))idx=i;if(direction<0 && LocalPlayerService.active.position()>3000)LocalPlayerService.active.seek(0);else if(idx+direction>=0 && idx+direction<files.size())playLocal(files.get(idx+direction));}}
 private void updateBar(){toggle.setText(playing?"Pause":"Play");if(!seeking)seek.setProgress(duration>0?(int)(1000*position/duration):0);time.setText(clock(position)+" / "+clock(duration));}
 private String clock(double seconds){int s=(int)Math.max(0,seconds);return String.format(Locale.ROOT,"%d:%02d",s/60,s%60);}
 private final class Bridge {@JavascriptInterface public void message(String raw){if(raw.length()>2000)return;handler.post(()->{try{JSONObject m=new JSONObject(raw);if(!streaming || !token.equals(m.optString("token")))return;String type=m.optString("type");if("status".equals(type)){position=m.optDouble("position",0);duration=m.optDouble("duration",0);int state=m.optInt("state");playing=state==1;status.setText(state==3?"Buffering YouTube…":playing?"YouTube preview":"Preview paused · tap Play");updateBar();}else if("error".equals(type))status.setText("Preview unavailable: "+m.optString("message"));else if("blocked".equals(type))status.setText("Tap Play inside the YouTube player.");}catch(JSONException ignored){}});}}
 private void loadThumbnail(String id,String expectedToken){worker.execute(()->{java.net.HttpURLConnection connection=null;try{connection=(java.net.HttpURLConnection)new java.net.URL("https://i.ytimg.com/vi/"+id+"/mqdefault.jpg").openConnection();connection.setConnectTimeout(8000);connection.setReadTimeout(8000);try(InputStream in=connection.getInputStream()){android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeStream(in);runOnUiThread(()->{if(token.equals(expectedToken)&&thumbnail!=null)thumbnail.setImageBitmap(bitmap);});}}catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}});}
 private static void copy(InputStream in,OutputStream out)throws IOException{byte[] buffer=new byte[32768];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);}
 private void importAudio(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,40);}
 private void export(File f){exportFile=f;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(f.getName().endsWith(".mp4")?"video/mp4":"audio/*").putExtra(Intent.EXTRA_TITLE,f.getName()).addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,41);}
 protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK || data==null || data.getData()==null)return;Uri uri=data.getData();File source=exportFile;status.setText("Copying file…");worker.execute(()->{try{if(request==40){String name="Imported audio "+System.currentTimeMillis()+".mp3";try(android.database.Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null && c.moveToFirst())name=c.getString(0).replaceAll("[^a-zA-Z0-9 ._()\\[\\]-]","_");}File target=new File(MusicRepository.folder(this),name);if(target.exists())throw new IOException("A file with this name already exists.");File temporary=new File(target.getPath()+".part");try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temporary)){if(in==null)throw new IOException("Cannot open audio");copy(in,out);}if(!temporary.renameTo(target))throw new IOException("Cannot finish import");}else if(request==41 && source!=null){try(InputStream in=new FileInputStream(source);OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("Cannot open export destination");copy(in,out);}}runOnUiThread(()->{status.setText("File saved.");page="library";showLibrary();});}catch(Exception e){runOnUiThread(()->status.setText("File copy failed: "+DownloadService.message(e)));}});}
 protected void onStart(){super.onStart();visible=true;handler.post(tick);if(web!=null)web.onResume();}
 protected void onStop(){visible=false;handler.removeCallbacks(tick);if(web!=null){command("pause",0);web.onPause();}super.onStop();}
 protected void onDestroy(){closePreview();worker.shutdownNow();new Thread(()->com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById("search")).start();handler.removeCallbacksAndMessages(null);super.onDestroy();}
}

package com.pocketmusic.android;

import android.app.*;
import android.os.*;
import android.net.Uri;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.*;
import java.util.concurrent.*;

/** Separate visible Google/YouTube login. Never reads page content or intercepts credentials. */
public final class YouTubeLoginActivity extends Activity {
 private static boolean profileInitialized;
 private static final String YOUTUBE="https://www.youtube.com/";
 private WebView web;private TextView status,address;private Button save;private ProgressBar progress;
 private boolean saving,visible,stalled,initializing=true;private final Handler handler=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Runnable poll=new Runnable(){public void run(){if(!visible||isFinishing()||saving)return;updateReady();handler.postDelayed(this,1000);}};
 private final Runnable timeout=()->{if(visible&&!saving&&web!=null&&web.getProgress()<100){stalled=true;updateReady();if(!save.isEnabled())status.setText("This page is taking a while. After completing verification, try Open YouTube. You can also Reload; do not restart an unfinished verification.");}};
 @Override public void onCreate(Bundle state){super.onCreate(state);if(!profileInitialized){if(Build.VERSION.SDK_INT>=28)WebView.setDataDirectorySuffix("youtube-login");profileInitialized=true;}getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  if(getIntent().getBooleanExtra("disconnect",false)){worker.execute(()->{YouTubeSession.disconnect(this);runOnUiThread(()->clearProfile(()->{Toast.makeText(this,"YouTube disconnected from Pocket Music.",Toast.LENGTH_LONG).show();finish();}));});return;}
  int padding=Math.round(16*getResources().getDisplayMetrics().density);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(padding,padding,padding,padding);root.setBackgroundColor(GlassTheme.BG);root.setOnApplyWindowInsetsListener((v,in)->{v.setPadding(padding,in.getSystemWindowInsetTop()+padding,padding,in.getSystemWindowInsetBottom()+padding);return in;});
  address=new TextView(this);address.setTextColor(GlassTheme.TEXT);address.setText("Connect YouTube");root.addView(address);status=new TextView(this);status.setTextColor(GlassTheme.MUTED);status.setText("Finish Google's sign-in and verification. When the YouTube session is ready, tap Save account. If the redirect stalls afterwards, tap Open YouTube.");root.addView(status);
  LinearLayout actions=new LinearLayout(this);save=new Button(this);save.setText("Save account");save.setEnabled(false);save.setOnClickListener(v->capture());actions.addView(save,new LinearLayout.LayoutParams(0,-2,1));Button close=new Button(this);close.setText("Cancel");close.setOnClickListener(v->finish());actions.addView(close);root.addView(actions);
  LinearLayout recovery=new LinearLayout(this);Button youtube=new Button(this);youtube.setText("Open YouTube");youtube.setOnClickListener(v->{if(!saving){web.stopLoading();web.loadUrl(YOUTUBE);}});recovery.addView(youtube,new LinearLayout.LayoutParams(0,-2,1));Button reload=new Button(this);reload.setText("Reload");reload.setOnClickListener(v->{if(!saving)web.reload();});recovery.addView(reload,new LinearLayout.LayoutParams(0,-2,1));root.addView(recovery);progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);root.addView(progress,new LinearLayout.LayoutParams(-1,Math.round(3*getResources().getDisplayMetrics().density)));
  web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);CookieManager.getInstance().setAcceptCookie(true);
  // Only this isolated, user-visible login view accepts cross-site sign-in cookies.
  CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
  web.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView view,int value){progress.setProgress(value);progress.setVisibility(value>=100?android.view.View.GONE:android.view.View.VISIBLE);}});
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){if(!request.isForMainFrame())return false;if(LoginPolicy.allowed(request.getUrl().toString()))return false;status.setText("This destination isn't part of the supported Google/YouTube sign-in flow. Try Open YouTube after verification.");return true;}
   @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon){stalled=false;address.setText(LoginPolicy.allowed(url)?"Secure page · "+Uri.parse(url).getHost():"Connect YouTube");handler.removeCallbacks(timeout);handler.postDelayed(timeout,30000);updateReady();}
   @Override public void onPageFinished(WebView view,String url){CookieManager.getInstance().flush();handler.removeCallbacks(timeout);updateReady();}
   @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame()){handler.removeCallbacks(timeout);stalled=true;updateReady();if(!save.isEnabled())status.setText("The page couldn't load. Check your connection, then Reload or Open YouTube after verification. If Google rejects embedded sign-in, Pocket Music cannot bypass it.");}}
  });root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  if(state!=null&&web.restoreState(state)!=null){initializing=false;}else clearProfile(()->{if(isDestroyed()||isFinishing())return;initializing=false;web.loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fwww.youtube.com%2F");});
 }
 private String readyCookie(){CookieManager cookies=CookieManager.getInstance();for(String url:new String[]{"https://music.youtube.com/",YOUTUBE,"https://youtube.com/"}){String header=cookies.getCookie(url);if(LoginPolicy.ready(header))return header;}return null;}
 private void updateReady(){if(save==null||saving||initializing)return;boolean ready=readyCookie()!=null;save.setEnabled(ready);if(ready)status.setText("YouTube session ready. Tap Save account to connect downloads and account recommendations. You don't need to wait for the page to finish loading.");else if(!stalled)status.setText("Complete Google's sign-in and two-step verification. Then Open YouTube if the redirect doesn't finish. Save account becomes available once YouTube has received your session.");}
 private void capture(){if(saving||initializing)return;String cookie=readyCookie();if(cookie==null){updateReady();return;}saving=true;save.setEnabled(false);handler.removeCallbacks(poll);status.setText("Saving account securely…");worker.execute(()->{try{YouTubeSession.save(this,cookie);if(!YouTubeSession.connected(this))throw new IllegalStateException();runOnUiThread(()->{if(isDestroyed())return;Toast.makeText(this,"YouTube account saved. Home recommendations can now refresh.",Toast.LENGTH_LONG).show();setResult(RESULT_OK);finish();});}catch(Exception error){runOnUiThread(()->{if(isDestroyed())return;saving=false;save.setEnabled(true);status.setText("Could not save the session securely. Tap Save account to retry.");});}});}
 private static void clearProfile(Runnable done){CookieManager.getInstance().removeAllCookies(removed->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();done.run();});}
 @Override protected void onSaveInstanceState(Bundle state){if(web!=null)web.saveState(state);super.onSaveInstanceState(state);}
 @Override protected void onPause(){visible=false;handler.removeCallbacks(poll);handler.removeCallbacks(timeout);if(web!=null){CookieManager.getInstance().flush();web.onPause();}super.onPause();}
 @Override protected void onResume(){super.onResume();visible=true;if(web!=null){web.onResume();handler.removeCallbacks(poll);handler.post(poll);handler.postDelayed(timeout,30000);}}
 @Override protected void onDestroy(){visible=false;handler.removeCallbacksAndMessages(null);if(web!=null){web.stopLoading();if(isFinishing()){web.clearCache(true);web.clearHistory();}web.destroy();}if(isFinishing())clearProfile(()->{});worker.shutdown();super.onDestroy();}
}

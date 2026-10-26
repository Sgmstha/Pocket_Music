package com.pocketmusic.android;

import android.app.*;
import android.os.Bundle;
import android.net.Uri;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Separate, user-visible Google/YouTube web login; no password or JavaScript bridge. */
public final class YouTubeLoginActivity extends Activity {
 private static boolean profileInitialized;
 private WebView web;private TextView status,address;private Button save;private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static boolean allowed(String url){Uri u=Uri.parse(url);String h=u.getHost();return "https".equals(u.getScheme())&&h!=null&&(h.equals("youtube.com")||h.endsWith(".youtube.com")||h.equals("accounts.google.com"));}
 @Override public void onCreate(Bundle state){super.onCreate(state);if(!profileInitialized){if(android.os.Build.VERSION.SDK_INT>=28)WebView.setDataDirectorySuffix("youtube-login");profileInitialized=true;}getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  if(getIntent().getBooleanExtra("disconnect",false)){worker.execute(()->{YouTubeSession.disconnect(this);runOnUiThread(()->CookieManager.getInstance().removeAllCookies(done->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();Toast.makeText(this,"YouTube disconnected from Pocket Music.",Toast.LENGTH_LONG).show();finish();}));});return;}
  final int padding=Math.round(16*getResources().getDisplayMetrics().density);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(padding,padding,padding,padding);root.setBackgroundColor(GlassTheme.BG);
  root.setOnApplyWindowInsetsListener((v,in)->{v.setPadding(padding,in.getSystemWindowInsetTop()+padding,padding,in.getSystemWindowInsetBottom()+padding);return in;});
  address=new TextView(this);address.setTextColor(GlassTheme.TEXT);address.setText("Connect YouTube");root.addView(address);
  status=new TextView(this);status.setTextColor(GlassTheme.MUTED);status.setText("Sign in on Google's page, return to YouTube Music, then tap Use this session. Session access stays on this phone; no cookie files to manage.");root.addView(status);
  LinearLayout actions=new LinearLayout(this);save=new Button(this);save.setText("Use this session");save.setOnClickListener(v->capture());actions.addView(save,new LinearLayout.LayoutParams(0,-2,1));Button close=new Button(this);close.setText("Cancel");close.setOnClickListener(v->finish());actions.addView(close);root.addView(actions);
  web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){if(!request.isForMainFrame())return false;if(allowed(request.getUrl().toString()))return false;status.setText("Only Google sign-in and YouTube pages can open here.");return true;}
   @Override public void onPageFinished(WebView view,String url){address.setText(allowed(url)?"Secure page · "+Uri.parse(url).getHost():"Connect YouTube");}
   @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())status.setText("Could not load sign-in. Check your connection. If Google rejects this browser, cancel; Pocket Music cannot bypass that restriction.");}
  });root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);web.loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com");
 }
 private void capture(){String url=web.getUrl();if(url==null||!allowed(url)||!Uri.parse(url).getHost().endsWith("youtube.com")){status.setText("Finish signing in, then return to YouTube Music first.");return;}
  final String cookie=CookieManager.getInstance().getCookie("https://music.youtube.com");
  try{YouTubeCookies.netscape(cookie);}catch(Exception e){status.setText("No signed-in YouTube session found. Sign in using the account button on the page, then try again.");return;}
  save.setEnabled(false);status.setText("Saving session securely…");worker.execute(()->{try{YouTubeSession.save(this,cookie);runOnUiThread(()->{Toast.makeText(this,"YouTube session saved. Retry your download.",Toast.LENGTH_LONG).show();finish();});}catch(Exception e){runOnUiThread(()->{save.setEnabled(true);status.setText("Could not save the session securely. Please try again.");});}});
 }
 @Override protected void onPause(){if(web!=null)web.onPause();super.onPause();}
 @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();}
 @Override protected void onDestroy(){if(web!=null){web.stopLoading();web.clearCache(true);web.clearHistory();web.destroy();}CookieManager.getInstance().removeAllCookies(done->CookieManager.getInstance().flush());WebStorage.getInstance().deleteAllData();worker.shutdown();super.onDestroy();}
}

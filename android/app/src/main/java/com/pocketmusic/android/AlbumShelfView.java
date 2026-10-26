package com.pocketmusic.android;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import java.util.*;
import static com.pocketmusic.android.DesignTokens.*;

/** Recycle off-screen artwork cards instead of eagerly decoding an entire shelf. */
final class AlbumShelfView extends RecyclerView {
 interface Actions {void play(Track track,List<Track> source);void options(Track track,List<Track> source);}
 AlbumShelfView(Context c,List<Track> tracks,Actions actions){super(c);com.bumptech.glide.RequestManager artworkRequests=Glide.with(c);LinearLayoutManager layout=new LinearLayoutManager(c,HORIZONTAL,false);layout.setRecycleChildrenOnDetach(true);setLayoutManager(layout);setItemAnimator(null);setHasFixedSize(true);setItemViewCacheSize(1);setNestedScrollingEnabled(false);setOverScrollMode(OVER_SCROLL_NEVER);setClipToPadding(false);float scale=c.getResources().getConfiguration().fontScale;int height=GlassTheme.dp(c,SIZE_ART_LARGE+SPACE_16)+(int)((TYPE_BODY+TYPE_CAPTION)*1.5f*c.getResources().getDisplayMetrics().scaledDensity);setLayoutParams(new LinearLayout.LayoutParams(-1,height));setAdapter(new Adapter<Holder>(){
  @Override public Holder onCreateViewHolder(ViewGroup parent,int type){LinearLayout root=new LinearLayout(c);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(0,0,GlassTheme.dp(c,SPACE_12),0);root.setLayoutParams(new RecyclerView.LayoutParams(GlassTheme.dp(c,SIZE_ART_LARGE+SPACE_12),-1));ImageView art=new ImageView(c);art.setScaleType(ImageView.ScaleType.CENTER_CROP);art.setBackground(GlassTheme.coverFrame(c));art.setClipToOutline(true);root.addView(art,new LinearLayout.LayoutParams(GlassTheme.dp(c,SIZE_ART_LARGE),GlassTheme.dp(c,SIZE_ART_LARGE)));TextView name=text(c,TYPE_BODY);name.setTypeface(null,Typeface.BOLD);name.setPadding(0,GlassTheme.dp(c,SPACE_8),0,0);root.addView(name);TextView artist=text(c,TYPE_CAPTION);artist.setTextColor(COLOR_TEXT_MUTED);root.addView(artist);return new Holder(root,art,name,artist);}
  @Override public void onBindViewHolder(Holder holder,int position){Track track=tracks.get(position);holder.title.setText(track.title);holder.artist.setText(track.artist.isEmpty()?"Song":track.artist);holder.art.setContentDescription(track.title+" artwork");holder.itemView.setContentDescription("Play suggested "+track.title);holder.itemView.setOnClickListener(v->actions.play(track,tracks));holder.itemView.setOnLongClickListener(v->{actions.options(track,tracks);return true;});Artwork.load(c,track,holder.art,GlassTheme.dp(c,SIZE_ART_LARGE));}
  @Override public int getItemCount(){return Math.min(8,tracks.size());}
  @Override public void onViewRecycled(Holder holder){artworkRequests.clear(holder.art);super.onViewRecycled(holder);}
 });}
 private static TextView text(Context c,int size){TextView t=new TextView(c);t.setTextSize(size);t.setTextColor(COLOR_TEXT);t.setIncludeFontPadding(false);t.setMaxLines(1);t.setEllipsize(TextUtils.TruncateAt.END);return t;}
 private static final class Holder extends ViewHolder {final ImageView art;final TextView title,artist;Holder(View root,ImageView art,TextView title,TextView artist){super(root);this.art=art;this.title=title;this.artist=artist;}}
}

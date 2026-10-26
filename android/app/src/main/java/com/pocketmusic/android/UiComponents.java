package com.pocketmusic.android;
import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import static com.pocketmusic.android.DesignTokens.*;

/** Thin configuration of licensed Material components, not a separate UI framework. */
final class UiComponents {
 static ImageButton icon(Context c,int drawable,String label,Runnable action){ImageButton b=new ImageButton(c);b.setImageResource(drawable);b.setImageTintList(ColorStateList.valueOf(COLOR_TEXT));b.setBackground(GlassTheme.row(c));int pad=GlassTheme.dp(c,SPACE_12);b.setPadding(pad,pad,pad,pad);b.setContentDescription(label);b.setOnClickListener(v->action.run());return b;}
 static MaterialButton action(Context c,String label,int icon,boolean filled,Runnable action){MaterialButton b=new MaterialButton(c);b.setText(label);b.setAllCaps(false);b.setTextSize(TYPE_BODY);b.setMinHeight(GlassTheme.dp(c,SIZE_TOUCH));b.setCornerRadius(GlassTheme.dp(c,RADIUS_PILL));b.setTextColor(filled?COLOR_ON_ACCENT:COLOR_TEXT);b.setBackgroundTintList(ColorStateList.valueOf(filled?COLOR_ACCENT:COLOR_SURFACE_RAISED));if(icon!=0){b.setIconResource(icon);b.setIconTint(ColorStateList.valueOf(filled?COLOR_ON_ACCENT:COLOR_TEXT));b.setIconSize(GlassTheme.dp(c,SPACE_20));}b.setOnClickListener(v->action.run());return b;}
 static MaterialCardView card(Context c,int radius){MaterialCardView card=new MaterialCardView(c);card.setRadius(GlassTheme.dp(c,radius));card.setCardElevation(0);card.setCardBackgroundColor(COLOR_SURFACE);card.setStrokeWidth(GlassTheme.dp(c,1));card.setStrokeColor(COLOR_BORDER);card.setUseCompatPadding(false);return card;}
 static Chip chip(Context c,String text,boolean checked,Runnable action){Chip chip=new Chip(c);chip.setText(text);chip.setTextSize(TYPE_CAPTION);chip.setCheckable(true);chip.setChecked(checked);chip.setEnsureMinTouchTargetSize(true);chip.setChipMinHeight(GlassTheme.dp(c,40));chip.setCheckedIconVisible(false);chip.setTextColor(checked?COLOR_ON_ACCENT:COLOR_TEXT);chip.setChipBackgroundColor(ColorStateList.valueOf(checked?COLOR_ACCENT:COLOR_SURFACE_RAISED));chip.setChipStrokeWidth(0);chip.setOnClickListener(v->action.run());return chip;}
}

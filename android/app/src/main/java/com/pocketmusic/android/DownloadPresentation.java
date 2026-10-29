package com.pocketmusic.android;
import java.util.*;
import java.util.regex.*;
final class DownloadPresentation {
 private static final Pattern SPEED=Pattern.compile("[0-9]+(?:\\.[0-9]+)?\\s*(?:KiB|MiB|GiB|KB|MB|GB)/s");
 static String group(DownloadQueue.Job job){if(job.pending())return "active";if(List.of("failed","saveFailed","interrupted").contains(job.state))return "attention";return "finished";}
 static String summary(List<DownloadQueue.Job> jobs){int active=0,waiting=0,done=0,attention=0;for(DownloadQueue.Job j:jobs){if("running".equals(j.state))active++;else if("queued".equals(j.state))waiting++;else if("done".equals(j.state))done++;else if(group(j).equals("attention"))attention++;}List<String> parts=new ArrayList<>();if(active>0)parts.add(active+" downloading");if(waiting>0)parts.add(waiting+" queued");if(done>0)parts.add(done+" ready");if(attention>0)parts.add(attention+" need attention");return parts.isEmpty()?"No active downloads":String.join(" · ",parts);}
 static int progress(List<DownloadQueue.Job> jobs){int sum=0,count=0;for(DownloadQueue.Job j:jobs)if(j.pending()){sum+=Math.max(0,j.progress);count++;}return count==0?0:sum/count;}
 static boolean unknown(List<DownloadQueue.Job> jobs){boolean known=false;for(DownloadQueue.Job j:jobs)if("running".equals(j.state)){if(j.progress<0)return true;known=true;}return !known;}
 static String state(DownloadQueue.Job job){switch(job.state){case "queued":return "Waiting in queue";case "done":return "Saved and ready to play";case "saveFailed":return "Downloaded · folder save needs attention";case "failed":return job.message.isBlank()?"Download failed · retry when ready":job.message;case "interrupted":return "Interrupted · retry to continue";case "cancelled":return "Cancelled";default:if(job.message.startsWith("Saving to"))return "Saving to your selected folder…";if(job.progress>=100)return job.format.equals("mp4")?"Finishing video…":"Converting audio…";if(job.progress<0)return job.message.isBlank()?"Preparing…":job.message;return job.progress+"% downloaded";}}
 static String speed(String line){if(line==null)return "";Matcher matcher=SPEED.matcher(line);return matcher.find()?matcher.group():"";}
 static String transfer(DownloadQueue.Job job){if(!"running".equals(job.state)||job.progress<0||job.progress>=100)return "";String eta=job.etaSeconds<0?"":job.etaSeconds<60?job.etaSeconds+"s left":((job.etaSeconds+59)/60)+" min left";return job.speed.isEmpty()?eta:eta.isEmpty()?job.speed:job.speed+" · "+eta;}
}

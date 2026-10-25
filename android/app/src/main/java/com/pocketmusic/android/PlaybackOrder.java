package com.pocketmusic.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Queue policy shared by native audio and foreground YouTube previews. */
final class PlaybackOrder {
    static final int OFF=0, ALL=1, ONE=2;
    private final Random random;
    private final List<Integer> order=new ArrayList<>();
    private int current=-1;
    private boolean shuffled;
    PlaybackOrder(){this(new Random());}
    PlaybackOrder(Random random){this.random=random;}
    void reset(int count,int selected,boolean shuffle){
        order.clear();for(int i=0;i<count;i++)order.add(i);
        current=selected>=0&&selected<count?selected:-1;
        shuffled=shuffle;
        if(shuffle)shuffleRemaining();
    }
    void setShuffle(boolean value){
        if(shuffled==value)return;
        shuffled=value;
        if(value)shuffleRemaining();else Collections.sort(order);
    }
    private void shuffleRemaining(){
        order.remove(Integer.valueOf(current));Collections.shuffle(order,random);
        if(current>=0)order.add(0,current);
    }
    int current(){return current;}
    int advance(int direction,int repeat,boolean automatic){
        if(current<0||order.isEmpty())return -1;
        if(automatic&&repeat==ONE)return current;
        int target=order.indexOf(current)+direction;
        if(target<0||target>=order.size()){
            if(repeat!=ALL)return -1;
            target=(target+order.size())%order.size();
        }
        current=order.get(target);return current;
    }
}

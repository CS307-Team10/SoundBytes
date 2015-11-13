package com.soundbytes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.andexert.expandablelayout.library.ExpandableLayoutItem;
import com.andexert.expandablelayout.library.ExpandableLayoutListView;
import com.soundbytes.db.DBHandlerResponse;
import com.soundbytes.db.FeedDatabaseHandler;
import com.soundbytes.views.AudioTrackView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Olumide on 11/4/2015.
 */
public class NewsFeedFragment extends TitledFragment implements DBHandlerResponse, AudioTrackController, SwipeRefreshLayout.OnRefreshListener{
    private String title;
    private View viewLayout;
    private ExpandableLayoutListView expListView;
    private NewsFeedAdapter adapter;
    private FeedDatabaseHandler dbHandler;
    private int currentlyPlaying = -1;
    private SwipeRefreshLayout refresher;
    private DatabaseUpdatedReceiver receiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflate the fragment
        viewLayout = inflater.inflate(R.layout.fragment_newsfeed, container, false);
        expListView = (ExpandableLayoutListView)viewLayout.findViewById(R.id.feed_exp_list_view);
        refresher = (SwipeRefreshLayout)viewLayout.findViewById(R.id.swipe_refresh);
        refresher.setOnRefreshListener(this);
        dbHandler = FeedDatabaseHandler.getInstance(getContext(), this);
        expListView.setOnItemClickListener(getItemClickListener());
        receiver = new DatabaseUpdatedReceiver();
        DatabaseUpdatedReceiver.frag = this;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(receiver, filter);
        return viewLayout;
    }

    @Override
    /**
     * This returns the title of the fragment
     */
    public String getTitle(){
        //This is a work around since, the fragment isn't attached to the activity yet when this method is called
        if(title == null)
            title = MainActivity.getMainActivity()
                    .getResources().getString(R.string.newsfeed_fragment_title);
        return title;
    }

    public boolean isTrackCurrentlyPlaying(int trackId){
        return currentlyPlaying == trackId;
    }

    /**
     *
     * @param trackId this is currently just the id in the database
     */
    @Override
    public void playTrack(int trackId){
        pauseAllAudio();
        currentlyPlaying = trackId;
        dbHandler.markAsRead(trackId);
        //TODO play audio track here
        adapter.notifyDataSetChanged();
    }

    /**
     * This method should pause all audio playing and call resetAudioButton() on all audioTrackViews
     * currently in the layout.
     * TODO iterate through the audioTrackViews and call resetAudioButton()
     */
    @Override
    public void pauseAllAudio(){
        //Since only one audio can play at a time, it only has to pause one audio
        currentlyPlaying = -1;
    }

    /**
     * This method is called when the pause button on an audioTrackView is pressed.
     * The user expects the track to stop playing
     * It might be beneficial to stop all other tracks that are playing
     * @param trackId this is the index of the track that a new filter needs to be applied to.
     *                It's the id that's assigned to an audioTrackView on controller registration.
     */
    @Override
    public void pauseTrack(int trackId){
        //TODO
    }

    @Override
    public void deleteTrack(AudioTrackView track, int trackId){
        //Not used in this fragment
    }

    @Override
    public void applyFilter(int trackId, int filterIndex){
        //Not used in this fragment
    }

    public void onDBReady(){
        adapter = new NewsFeedAdapter(getContext(), dbHandler, this);
        expListView.setAdapter(adapter);
        expListView.invalidate();
    }

    public void onRefresh() {
        refresher.setRefreshing(true);
        adapter.notifyDataSetChanged();
        //TODO make request to server to check for new messages
        refresher.setRefreshing(false);
    }

    public NewsFeedAdapter getAdapter(){
        return adapter;
    }

    public static class DatabaseUpdatedReceiver extends BroadcastReceiver{
        public static NewsFeedFragment frag;
        public void onReceive(final Context context, Intent intent) {
            if(intent.getAction().equals(SoundByteConstants.dbUpdateBroadcast)){
                frag.getAdapter().notifyDataSetChanged();
            }
        }
    }

    private AdapterView.OnItemClickListener getItemClickListener(){
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final SoundByteFeedObject feedObject = dbHandler.getFeedObject(dbHandler.getCount() -1 - position);
                ExpandableLayoutItem mEplItem = (ExpandableLayoutItem) view.findViewById(R.id.expandableLayout);
                Log.v("Position", "Position: "+position);
                Log.v("time", DateUtils.getRelativeDateTimeString(getContext(), feedObject.getDate().getTime(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.DAY_IN_MILLIS * 30, DateUtils.FORMAT_ABBREV_RELATIVE).toString());
                if(feedObject.getIsSent()) {
                    mEplItem.hideNow();
                    Log.v("is sent", "Is sent");
                    return;
                }
                //Check if audio file is present, if not //show loading and download it
                final AudioTrackView track = (AudioTrackView)mEplItem.getContentLayout().findViewById(R.id.feed_trackview);
                try{
                    Log.v("audio path", "File name is:"+feedObject.getAudioPath());
                    track.autoUpdateRecordPreview(new File(feedObject.getAudioPath()));
                    return;
                }catch (Exception e){
                    //Do nothing
                }
                if(feedObject.getAudioPath() == null || feedObject.getAudioPath().equals("")) {
                    mEplItem.hideNow();
                    //make spinner visible
                    mEplItem.getHeaderLayout().findViewById(R.id.loading_progress_bar).setVisibility(View.VISIBLE);
                    mEplItem.getHeaderLayout().findViewById(R.id.feed_type_image).setVisibility(View.INVISIBLE);

                    //Start async task to download audio file
                    ServerRequests serverRequests = new ServerRequests(getContext());
                    serverRequests.fetchAudioFileInBackground(new UserLocalStore(getContext()).getLoggedInUser(),
                            feedObject.getAudioID(), new OnAudioDownloadCallback() {
                                @Override
                                public void onAudioDownloaded(String base64) {
                                    //Save the audio content to file
                                    if(base64 != null) {
                                        byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                                        try {
                                            File file = createAudioFile(feedObject.getFriend(), feedObject.getIsSent());
                                            FileOutputStream os = new FileOutputStream(file, true);
                                            os.write(decoded);
                                            os.close();
                                            dbHandler.updateFilePath(feedObject.getId(), file);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                            });
                }
            }
        };
    }

    private File createAudioFile(String friend, boolean isSent){
        String suffix = ".3gp";
        String prefix = "SB_";
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName;
        if(isSent)
            imageFileName = prefix + "s" + timeStamp + friend;
        else
            imageFileName = prefix + "r" + timeStamp + friend;
        File storageDir = new File(Environment.getExternalStorageDirectory(), "SoundBytes" + File.separatorChar);
        if (!storageDir.exists())
            storageDir.mkdir();
        return new File(storageDir, imageFileName+suffix);
    }
}

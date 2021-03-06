package com.soundbytes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.ContextMenu;

import android.view.LayoutInflater;
import android.view.MenuInflater;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.soundbytes.views.AudioTrackView;

import com.soundbytes.views.PlayButton;
import com.soundbytes.views.RecordButton;

import java.io.File;

/**
 * Created by Olumide on 10/3/2015.
 */
public class ComposeFragment extends TitledFragment implements RecordButtonListeners, View.OnClickListener, AudioTrackController{
    private String title;

    private AudioTrackView selectedTrack, onlyTrack;
    private int trackCount = 0;

    private RecordButton r;
    private MediaRecorder mRecorder = null;
    UserLocalStore userLocalStore;
    private PlayButton p = null;
    private MediaPlayer mPlayer = null;

    private static String mFileName = null;
    private LinearLayout layout;
    private int currentSelectedFilterId = 0;
    private ImageButton bAddFriend;
    private ImageButton bSend;
    private boolean isPlaying;
    EditText etAddFriend;

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.person_add_button:
                System.out.println("add friend clicked");
                String friendName = etAddFriend.getText().toString();
                User cUser = userLocalStore.getLoggedInUser();
                String currentUser = cUser.username;
                User user = new User(friendName, currentUser, -2);
                authenticate(user);
                break;
            case R.id.send_button:
                System.out.println("send button clicked");
                User cuUser = userLocalStore.getLoggedInUser();
                String uName = cuUser.username;
                if(onlyTrack != null) {
                    Intent intent = new Intent(getContext(), SendActivity.class);
                    intent.putExtra("uName", uName);
                    intent.putExtra("filter", currentSelectedFilterId);
                    System.out.println("uName" + uName);
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Record something to send!", Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    private void authenticate(User user){
        ServerRequests serverRequests = new ServerRequests(getContext());
        serverRequests.addFriendDataInBackground(user, new GetUserCallBack() {
            @Override
            public void done(User returnedUser) {
                if (returnedUser == null) {
                    showErrorMessage();
                } else {
                    System.out.println("name:" + returnedUser.name);
                    System.out.println("name:" + returnedUser.age);
                    System.out.println("added friend successfully!");
                    showSuccessMessage();
                }
            }
        });
    }
    private void showErrorMessage(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setMessage("nonexistent friend");
        dialogBuilder.setPositiveButton("ok", null);
        dialogBuilder.show();
    }
    private void showSuccessMessage(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setMessage("Friend added successfully");
        dialogBuilder.setPositiveButton("ok", null);
        dialogBuilder.show();
    }
    @Override
    /**
     * This returns the title of the fragment
     */
    public String getTitle(){
        //This is a work around since, the fragment isn't attached to the activity yet when this method is called
        if(title == null)
            title = MainActivity.getMainActivity()
                    .getResources().getString(R.string.record_fragment_title);
        return title;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflate the fragment
        View view = inflater.inflate(R.layout.fragment_compose_audio, container, false);
        r = (RecordButton) view.findViewById(R.id.mic_button);
        r.setRecordListener(r);
        r.setSecondaryRecordListener(this);

        // set up MediaRecorder and outFileName for the RecordButton
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";
        r.SetOutFileName(mFileName);
        r.SetAudioRecorder(mRecorder);

        // get PlayButton
        p = (PlayButton) view.findViewById(R.id.play_button);
        p.SetOutFileName(mFileName);
        p.SetMediaPlayer(mPlayer);

        //Find the layout
        layout = (LinearLayout) view.findViewById(R.id.track_layout);

        view.findViewById(R.id.pause_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseAllAudio();
            }
        });
        userLocalStore = ((MainActivity)getActivity()).userLocalStore;
        bAddFriend = (ImageButton)view.findViewById(R.id.person_add_button);
        bAddFriend.setOnClickListener(this);
        etAddFriend = (EditText)view.findViewById(R.id.add_person_text);
        bSend = (ImageButton)view.findViewById(R.id.send_button);
        bSend.setOnClickListener(this);
        if(savedInstanceState != null && savedInstanceState.getInt(SoundByteConstants.TRACK_COUNT) > 0){
            onStartRecording();
            if(savedInstanceState.getBoolean(SoundByteConstants.IS_PLAYING))
                onlyTrack.setPauseButton();
            onlyTrack.setFilter(savedInstanceState.getInt(SoundByteConstants.FILTER));
            onStopRecording();
        }else{
            deleteRecordFile();
        }
        return view;
    }

    private void deleteRecordFile(){
        File tempFile = new File(mFileName);
        if(tempFile.exists())
            tempFile.delete();
    }

    private void setAudioPlaybackFinishedCallback(){
        FilterManager.setAudioDoneCallback(new FilterManager.OnAudioDoneCallback() {
            public void audioFinished() {
                pauseAllAudio();
            }

            public float getPlaybackSpeed(){
                switch(currentSelectedFilterId){
                    case 3:
                        return FilterManager.speedPlaybackSpeed;
                    case 4:
                        return FilterManager.slowPlaybackSpeed;
                    case 1:
                        return FilterManager.highPlaybackSpeed;
                    case 2:
                        return FilterManager.lowPlaybackSpeed;
                    default:
                        return FilterManager.regularPlaybackSpeed;
                }
            }

            public Context getContext() {
                return ComposeFragment.this.getContext();
            }
        });
    }

    @Override
    public void onVisible(){
        setAudioPlaybackFinishedCallback();
    }

    @Override
    /**
     * This method is called when the recordButton is help down,
     * the user expects audio to begin recording, this method adds a new audioTrackView to the layout
     */
    public void onStartRecording(){
        setAudioPlaybackFinishedCallback();
        AudioTrackView track = createTrack();
        //Add the new audioTrack to layout
        addTrackToLayout(track);
        //Register this fragment with the AudioTrack in order to receive onPlay
        //onPause e.t.c callbacks
        track.registerController(this, 1);
        currentSelectedFilterId = 0;
    }

    private AudioTrackView createTrack(){
        //Return new AudioTrack
        AudioTrackView track = new AudioTrackView(getContext());
        onlyTrack = track;
        track.setHeightInDP(54);
        return track;
    }

    private void addTrackToLayout(AudioTrackView trackView){
        //Add the trackView to layout
        layout.addView(trackView);
        //Setup the long press stuff
        registerForContextMenu(trackView);
        //TODO replace the counter with an actual model
        //Check if the limit has been reached then disable record button if so
        if(++trackCount >= SoundByteConstants.MAX_TRACK_COUNT)
            r.setEnabled(false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        selectedTrack = (AudioTrackView)v;
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.track_floating_context_menu, menu);
    }

    @Override
    /**
     * This method is called when the user lifts their hand off from the recordButton,
     * note that this gets called even if the user lifts their hand off well after the time limit has passed
     */
    public void onStopRecording(){
        if(onlyTrack != null)
            onlyTrack.autoUpdateRecordPreview(new File(mFileName));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.track_delete:
                selectedTrack.delete();
                selectedTrack = null;
                onlyTrack = null;
                currentSelectedFilterId = 0;
                FilterManager.setAudioDoneCallback(null);
                //delete file from disk
                deleteRecordFile();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override

    /**
     * This method is called when an audioTracks playButton is pressed.
     * When this method is called, the user expects the track to play
     * @param trackId this is the index of the track that a new filter needs to be applied to.
     *                It's the id that's assigned to an audioTrackView on controller registration.
     */
    public void playTrack(int trackId)
    {
        pauseAllAudio();
        final FilterManager fm = new FilterManager(mFileName,getContext());
        setAudioPlaybackFinishedCallback();
        isPlaying = true;
        switch(currentSelectedFilterId)
        {
            case 0:
                fm.Regular();
                break;
            case 3://1:
                fm.Speedup();
                break;
            case 4://2:
                fm.Slowdown();
                break;
            case 1://3:
                fm.HighPitch();
                break;
            case 2://4:
                fm.LowPitch();
                break;
        }
    }

    @Override
    /**
     * This method should pause all audio playing and call resetAudioButton() on all audioTrackViews
     * currently in the layout.
     * TODO iterate through the audioTrackViews and call resetAudioButton()
     */
    public void pauseAllAudio(){
        isPlaying = false;
        FilterManager.stopAudio();
        //right now there's just one trackID which is 1
        if(onlyTrack != null)
          pauseTrack(1);
    }

    @Override
    /**
     * This method is called when the pause button on an audioTrackView is pressed.
     * The user expects the track to stop playing
     * It might be beneficial to stop all other tracks that are playing
     * @param trackId this is the index of the track that a new filter needs to be applied to.
     *                It's the id that's assigned to an audioTrackView on controller registration.
     */
    public void pauseTrack(int trackId){
        isPlaying = false;
        FilterManager.stopAudio();
        if(onlyTrack != null)
            onlyTrack.resetPlayButton();
    }

    @Override
    /**
     * This only removes the audioTrackView from layout as of right now.
     * @param track this is the AudioTrackView that should be removed from the layout
     * @param trackId this is the index of the track that a new filter needs to be applied to.
     *                It's the id that's assigned to an audioTrackView on controller registration.
     */
    public void deleteTrack(AudioTrackView track, int trackId){
        LinearLayout layout = (LinearLayout) getView().findViewById(R.id.track_layout);
        //unregister the long press stuff
        unregisterForContextMenu(track);
        //remove audioTrack from the layout
        layout.removeView(track);
        //enable the record button, since the number of tracks is definitely less than the limit right now
        r.setEnabled(true);
        trackCount--;
    }

    @Override
    /**
     * This is called when the track is swiped on, and the user expects to hear
     * the new filter when play is pressed
     * @param trackId this is the index of the track that a new filter needs to be applied to.
     *                It's the id that's assigned to an audioTrackView on controller registration.
     * @param filterIndex this is what would be used to identify the filter that should be applied
     *                    the name of the filer it corresponds to can be gotten from SoundByteConstants.FILTER_NAMES
     */
    public void applyFilter(int trackId, int filterIndex)
    {
        currentSelectedFilterId = filterIndex;
//        if(filterIndex == 1)
//            currentSelectedFilterId = 3;
//        else if(filterIndex == 2)
//            currentSelectedFilterId = 4;
//        else if(filterIndex == 3)
//            currentSelectedFilterId = 1;
//        else if(filterIndex == 4)
//            currentSelectedFilterId = 2;

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SoundByteConstants.FILTER, currentSelectedFilterId);
        outState.putInt(SoundByteConstants.TRACK_COUNT, trackCount);
        outState.putBoolean(SoundByteConstants.IS_PLAYING, isPlaying);
    }
}

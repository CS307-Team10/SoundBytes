package com.soundbytes;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;

import com.soundbytes.views.InfoLinearLayout;

import java.util.Arrays;

/**
 * Created by Olumide on 10/21/2015.
 */
public class DrawerAdapter extends BaseAdapter {
    private Context mContext;
    private String[] infoName;
    private String[] infoContent;
    private User user;
    private View.OnClickListener onClick;

    /**
     * Constructor
     * @param context The activity context
     */
    public DrawerAdapter(Context context, View.OnClickListener onClick){
        mContext = context;
        this.onClick = onClick;
        //Initialize the object
        init();
    }

    /**
     * Initialization method to avoid copying the same thing in every constructor
     */
    private void init(){
        UserLocalStore store = new UserLocalStore(mContext);
        user = store.getLoggedInUser();
        infoName = mContext.getResources().getStringArray(R.array.nav_drawer);
        infoContent = new String[]{"", user.name, "@"+user.username, user.age+"", mContext.getResources().getString(R.string.logout)};
    }

    @Override
    public Object getItem(int position){
        return null;
    }

    /**
     * @param position index of view whichs id is wanted
     * @return 0
     */
    @Override
    public long getItemId(int position){
        return 0;
    }

    /**
     * @return the number of items
     */
    @Override
    public int getCount() {
        return infoName.length;
    }

    /**
     * This method is used by the navigation drawer listView for view recycling
     * @param position the index of the view requested
     * @param convertView an old view which is now offscreen and can be reused, Note that this could also be null
     * @param parent the parent ViewGroup
     * @return the requested view
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        //Check if it's the first item, since this is an image
        if(position == 0) {
            ImageView profilePicture;
            //Check if view can't be reused
            if ((convertView == null) || !(convertView instanceof ImageView)) {
                // if it's not recycled, initialize some attributes
                profilePicture = new ImageView(mContext);
                Resources r = mContext.getResources();
                profilePicture.setImageDrawable(r.getDrawable(R.drawable.ic_person_black));
                float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 75, r.getDisplayMetrics());
            }else {
                //Reuse view
                profilePicture = (ImageView)convertView;
            }
            profilePicture.setAdjustViewBounds(true);
            view = profilePicture;
        }
        //Check if the item requested ids an InfoLinearLayout view
        else if(position != (getCount() - 1)){
            InfoLinearLayout infoLayout;
            //Check if view can't be reused
            if (!(convertView instanceof InfoLinearLayout)){
                infoLayout = new InfoLinearLayout(mContext, infoName[position], infoContent[position]);
            } else {
                //Reuse view
                infoLayout = (InfoLinearLayout) convertView;
                infoLayout.setDetails(infoName[position], infoContent[position]);
            }
            view = infoLayout;
        }else{
            Button logout;
            //Check if view can't be reused
            if (!(convertView instanceof Button)){
                logout = new Button(mContext);
            } else {
                //Reuse view
                logout = (Button)convertView;
            }
            logout.setText(infoName[position]);
            logout.setOnClickListener(onClick);
            view = logout;
        }
        return view;
    }
}

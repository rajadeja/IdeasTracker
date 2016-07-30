package appbox.ideastracker.recycler;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.util.Random;

import appbox.ideastracker.MainActivity;
import appbox.ideastracker.R;
import appbox.ideastracker.database.DatabaseHelper;
import appbox.ideastracker.database.TinyDB;

/**
 * Created by Nicklos on 13/07/2016.
 *
 * Custom RecyclerView to handle swipe left and right
 * for quick actions.
 * Every MyRecyclerView holds an idea's text.
 */
public class MyRecyclerView extends RecyclerView {


    static final int ANIMATION_DURATION = 200; // For the deletion animation

    private boolean isActivated; // If we activated one of the quick actions

    private HorizontalAdapter mAdapter;
    private LinearLayoutManager mManager;
    private DatabaseHelper mDbHelper;

    private TinyDB mTinyDb;

    private MainActivity mainActivity;


    public MyRecyclerView(Context context) {
        super(context);
        constructor(context);
    }

    public MyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        constructor(context);
    }

    public MyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        constructor(context);
    }

    /**
     * Common code to all constructors
     * @param c
     */
    public void constructor(Context c){
        isActivated = false;
        mTinyDb = new TinyDB(c);
        mainActivity = MainActivity.getActivity(c);
        mDbHelper = DatabaseHelper.getInstance(c);
    }

    public void reboot(){
        isActivated = false;
    }

    public void setUp() {
        mManager = (LinearLayoutManager) this.getLayoutManager();
        mAdapter = (HorizontalAdapter) this.getAdapter();
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        velocityX *= 0.8;
        return super.fling(velocityX, velocityY);
    }

    @Override
    public void onScrollStateChanged(int state) {

        int tab = mAdapter.getTabNumber();

        switch (tab) {
            case 1: //Tab NOW
                stateChangedIdea(state);
                break;

            case 2: //Tab LATER
                stateChangeOther(state);
                break;

            case 3: //Tab DONE
                stateChangeOther(state);
                break;
        }

    }

    /**
     * Call when state changed in the "Idea" tab
     * @param state
     */
    private void stateChangedIdea(int state) {

        // Define limits to trigger quick action
        int width = mManager.getChildAt(0).getWidth();
        double limLeft = 0.4d * width;
        double limRight = 0.6d * width;

        if (state != RecyclerView.SCROLL_STATE_DRAGGING && !isActivated) {

            View child;
            int first = mManager.findFirstVisibleItemPosition();
            int last = mManager.findLastVisibleItemPosition();
            int left, right;

            if ((child = mManager.getChildAt(0)) != null && first == 0) {//We are going towards LATER
                right = child.getRight();
                if (right > limLeft) {
                    isActivated = true;
                    smoothScrollToPosition(0);
                } else smoothScrollToPosition(1);

            } else if ((child = mManager.getChildAt(1)) != null && last == 2) {//We are going towards DONE
                left = child.getLeft();
                if (left < limRight) {
                    isActivated = true;
                    smoothScrollToPosition(2);
                } else smoothScrollToPosition(1);
            }
        } else if (isActivated && state == RecyclerView.SCROLL_STATE_IDLE) { //Finished scrolling to one of the end
            int first = mManager.findFirstVisibleItemPosition();
            if ((mManager.getChildAt(0)) != null && first == 0) { //move to LATER
                sendCellToLater();
            } else { //move to DONE
                cheerSnackmessage();
                sendCellToDone();
            }
        }
    }

    /**
     * Call when state changed in the "Later" or "Done" tab
     * @param state
     */
    private void stateChangeOther(int state) {

        // Define limits to trigger quick action
        int width = mManager.getChildAt(0).getWidth();
        double limLeft = 0.4d * width;
        double limRight = 0.6d * width;

        if (state != RecyclerView.SCROLL_STATE_DRAGGING && !isActivated) {

            View child;
            int first = mManager.findFirstVisibleItemPosition();
            int last = mManager.findLastVisibleItemPosition();
            int left, right;

            if ((child = mManager.getChildAt(0)) != null && first == 0) {//We are going towards DELETE
                right = child.getRight();
                if (right > limLeft) {
                    isActivated = true;
                    smoothScrollToPosition(0);
                } else smoothScrollToPosition(1);

            } else if ((child = mManager.getChildAt(1)) != null && last == 2) {//We are going towards NOW
                left = child.getLeft();
                if (left < limRight) {
                    isActivated = true;
                    smoothScrollToPosition(2);
                } else smoothScrollToPosition(1);
            }
        } else if (isActivated && state == RecyclerView.SCROLL_STATE_IDLE) { //Wait for animation to finish
            int first = mManager.findFirstVisibleItemPosition();
            if ((mManager.getChildAt(0)) != null && first == 0) { //DELETE
                sendCellToDelete();
            } else { //NOW
                sendCellToNow();
            }
        }
    }

    /**
     * Display a message in a snackbar when a task
     * has been completed
     */
    private void cheerSnackmessage() {

        if (mTinyDb.getBoolean("cheerSwitch")) {
            String[] array = getContext().getResources().getStringArray(R.array.done_cheers);
            String randomStr = array[new Random().nextInt(array.length)];
            Snackbar.make(mainActivity.findViewById(R.id.main_content), randomStr, Snackbar.LENGTH_LONG).show();
        }
    }

    public void sendCellToNow() {

        final View v = this;
        Animation.AnimationListener al = new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                int tagId = (int) v.getTag();
                mDbHelper.moveToTab(1, tagId);
                DatabaseHelper.notifyAllLists();

                mainActivity.displayIdeasCount();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        };

        collapse(v, al);
    }

    public void sendCellToDelete() {

        final View v = this;
        Animation.AnimationListener al = new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                int tagId = (int) v.getTag();
                mDbHelper.deleteEntryWithSnack(v, tagId);
                DatabaseHelper.notifyAllLists();

                mainActivity.displayIdeasCount();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        };

        collapse(v, al);
    }

    public void sendCellToLater() {

        final View v = this;
        Animation.AnimationListener al = new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                int tagId = (int) v.getTag();
                mDbHelper.moveToTab(2, tagId);
                DatabaseHelper.notifyAllLists();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        };

        collapse(v, al);
    }

    public void sendCellToDone() {

        final View v = this;
        Animation.AnimationListener al = new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                int tagId = (int) v.getTag();

                mDbHelper.moveToTab(3, tagId);
                DatabaseHelper.notifyAllLists();
                mainActivity.displayIdeasCount();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        };

        collapse(v, al);
    }

    /**
     * Animate the view to shrink vertically
     * @param v
     * @param al
     */
    private void collapse(final View v, Animation.AnimationListener al) {

        final int initialHeight = v.getMeasuredHeight();

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.getLayoutParams().height = initialHeight;
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        if (al != null) {
            anim.setAnimationListener(al);
        }
        anim.setDuration(ANIMATION_DURATION);
        v.startAnimation(anim);
    }




}

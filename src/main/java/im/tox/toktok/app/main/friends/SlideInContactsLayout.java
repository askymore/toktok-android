package im.tox.toktok.app.main.friends;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.*;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.*;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.*;
import android.support.v7.widget.*;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.animation.*;
import android.view.*;
import android.widget.*;

import im.tox.toktok.app.call.CallActivity;
import im.tox.toktok.app.contacts.FileSendActivity;
import im.tox.toktok.app.domain.Friend;
import im.tox.toktok.app.message_activity.*;
import im.tox.toktok.app.simple_dialogs.*;
import im.tox.toktok.*;
import im.tox.toktok.app.video_call.VideoCallActivity;

import org.slf4j.*;

import static im.tox.toktok.TypedBundleKey.SBundle;

public final class SlideInContactsLayout extends ViewGroup {

    public SlideInContactsLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface AfterFinish {
        void run();
    }

    private Logger logger = LoggerFactory.getLogger(SlideInContactsLayout.class);

    private Activity activity = null;

    private ViewDragHelper mDragHelper = ViewDragHelper.create(this, 1f, new DragHelperCallback());
    private View mCoordinator = null;
    private CollapsingToolbarLayout mCollapsingToolbarLayout = null;
    private FloatingActionButton mFloatingActionButton = null;
    private ImageView mUserImage = null;
    private TextView mSubtitle = null;
    private TextView mTitle = null;
    private TextView mSettingsTitle = null;
    private Toolbar mToolbar = null;
    private View mStatusBar = null;
    private RelativeLayout mEditNameButton = null;
    private double mInitialMotionY = .0;
    private int mDragRange = 0;
    private int mTop = 0;
    private Boolean scrollActive = false;
    private double mDragOffset = .0;
    private TransitionDrawable backgroundTransition = null;
    private TextView mVoiceCall = null;
    private TextView mVideoCall = null;
    private CardView mMessage = null;
    private CardView mSaveProfile = null;
    private CardView mFilesSend = null;
    private RelativeLayout mDeleteFriend = null;
    private RelativeLayout mBlockFriend = null;
    private RelativeLayout mChangeColor = null;
    private int scrollTop = 0;

    private int[] icons = {
            R.id.contacts_icon_call,
            R.id.contacts_icon_message,
            R.id.contacts_icon_image,
            R.id.contacts_icon_download,
            R.id.contacts_icon_palette,
            R.id.contacts_icon_edit,
            R.id.contacts_icon_trash,
            R.id.contacts_icon_lock
    };

    public SlideInContactsLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideInContactsLayout(Context context) {
        this(context, null);
    }

    protected void onFinishInflate() {
        mCoordinator = this.findViewById(R.id.contacts_coordinator_layout);
        mCollapsingToolbarLayout = this.findViewById(R.id.contacts_collapsing_toolbar);
        mFloatingActionButton = this.findViewById(R.id.contacts_FAB);
        mUserImage = this.findViewById(R.id.contact_image);
        mTitle = this.findViewById(R.id.contact_title);
        mSubtitle = this.findViewById(R.id.contact_subtitle);
        mSettingsTitle = this.findViewById(R.id.contacts_other_title);
        mToolbar = this.findViewById(R.id.contacts_toolbar);
        mVoiceCall = this.findViewById(R.id.contacts_item_voice_call);
        mVideoCall = this.findViewById(R.id.contacts_item_video_call);
        mEditNameButton = this.findViewById(R.id.contacts_edit_alias);
        mStatusBar = this.findViewById(R.id.contacts_status_bar_color);
        mStatusBar.getLayoutParams().height = getStatusBarHeight();
        mMessage = this.findViewById(R.id.contacts_message);
        mSaveProfile = this.findViewById(R.id.contacts_save_photo);
        mFilesSend = this.findViewById(R.id.contacts_file_download);
        mDeleteFriend = this.findViewById(R.id.contacts_delete);
        mBlockFriend = this.findViewById(R.id.contacts_block_friend);
        mChangeColor = this.findViewById(R.id.contacts_edit_color);
        super.onFinishInflate();
    }

    public void start(Activity activity, Friend friend, int actionBarHeight) {
        this.activity = activity;
        mTitle.setText(friend.userName());

        mCollapsingToolbarLayout.setBackgroundColor(friend.color());
        mCollapsingToolbarLayout.setContentScrimColor(friend.color());
        mCollapsingToolbarLayout.setStatusBarScrimColor(friend.secondColor());

        mUserImage.setImageResource(friend.photoReference());

        mFloatingActionButton.setBackgroundTintList(ColorStateList.valueOf(friend.color()));

        mSubtitle.setText(friend.userMessage());

        mSettingsTitle.setTextColor(ColorStateList.valueOf(friend.color()));

        CollapsingToolbarLayout.LayoutParams b = (CollapsingToolbarLayout.LayoutParams) mToolbar.getLayoutParams();
        b.height = actionBarHeight + getStatusBarHeight();
        mToolbar.setLayoutParams(b);
        mToolbar.setPadding(0, getStatusBarHeight(), 0, 0);

        for (int item : icons) {
            ImageView icon = this.findViewById(item);
            icon.setImageTintList(ColorStateList.valueOf(friend.color()));
        }

        initListeners(friend);
        setVisibility(View.VISIBLE);

        backgroundTransition = (TransitionDrawable) getBackground();
        backgroundTransition.startTransition(500);

        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_bottom);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mCoordinator.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mCoordinator.startAnimation(animation);
    }

    private boolean smoothSlideTo(Float slideOffset) {
        int topBound = getPaddingTop();
        int y = (int) (topBound + slideOffset * mDragRange);
        if (mDragHelper.smoothSlideViewTo(mCoordinator, mCoordinator.getLeft(), y)) {
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (scrollActive) {
            mDragHelper.cancel();
            return false;
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                logger.debug("Intercept Touch DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
                logger.debug("Intercept Touch MOVE");
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                logger.debug("Intercept Touch UP");
                break;
        }

        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        try {
            mDragHelper.processTouchEvent(ev);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        Float y = ev.getY();
        NestedScrollView v = this.findViewById(R.id.contacts_nested);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mInitialMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                double dy = mInitialMotionY - y;
                if (dy > 0) {
                    if (mDragOffset < 0.5 && !scrollActive) {
                        smoothSlideTo(0.f);
                        scrollActive = true;
                        mStatusBar.setVisibility(View.VISIBLE);
                        mStatusBar.bringToFront();
                        scrollTop = v.getBottom();
                    }
                } else {
                    if (!scrollActive && Math.abs(dy) > 20) {
                        if (mDragOffset > 0.5f) {
                            finish();
                        } else {
                            smoothSlideTo(0.5f);
                            mStatusBar.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        if (v.getBottom() >= scrollTop) {
                            scrollActive = false;
                        }
                    }
                }
        }
        return super.dispatchTouchEvent(ev);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(
                View.resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                View.resolveSizeAndState(maxHeight, heightMeasureSpec, 0)
        );
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mDragRange = getHeight();
        if (changed) {
            mTop = getHeight() / 2;
            mCoordinator.layout(0, getHeight() / 2, right, mTop + mCoordinator.getMeasuredHeight());
        } else {
            mCoordinator.layout(0, mTop, right, mTop + mCoordinator.getMeasuredHeight());
        }
    }

    private final AfterFinish DoNothing = new AfterFinish() {
        @Override
        public void run() {
        }
    };

    void finish() {
        finish(DoNothing);
    }

    public void finish(final AfterFinish after) {
        smoothSlideTo(1f);
        backgroundTransition.reverseTransition(500);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                mCoordinator.setVisibility(View.INVISIBLE);
                setVisibility(View.GONE);
                after.run();
            }
        }, 500);
    }

    private final class DragHelperCallback extends ViewDragHelper.Callback {
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child == mCoordinator;
        }

        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            mTop = top;
            mDragOffset = (float) top / mDragRange;
            requestLayout();
        }

        public int getViewVerticalDragRange(@NonNull View child) {
            return mDragRange;
        }

        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            int topBound = 0;
            int bottomBound = getHeight();
            return Math.min(Math.max(top, topBound), bottomBound);
        }
    }

    private void initListeners(final Friend friend) {
        mEditNameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SimpleTextDialogDesign dial = new SimpleTextDialogDesign(
                        activity,
                        getResources().getString(R.string.contact_popup_edit_alias),
                        friend.color(),
                        R.drawable.ic_person_black_48dp,
                        friend.userName(),
                        null
                );
                dial.show();
            }
        });

        mVoiceCall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                activity.startActivity(new Intent(activity, CallActivity.class).putExtras(SBundle(
                        BundleKey.contactName().$minus$greater(friend.userName()),
                        BundleKey.contactColorPrimary().$minus$greater(friend.color()),
                        BundleKey.contactPhotoReference().$minus$greater(friend.photoReference())
                )));
            }
        });

        mVideoCall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                activity.overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
                activity.startActivity(new Intent(activity, VideoCallActivity.class).putExtras(SBundle(
                        BundleKey.contactPhotoReference().$minus$greater(friend.photoReference())
                )));
            }
        });

        mMessage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                activity.startActivity(new Intent(activity, MessageActivity.class).putExtras(SBundle(
                        BundleKey.messageTitle().$minus$greater(friend.userName()),
                        BundleKey.contactColorPrimary().$minus$greater(friend.color()),
                        BundleKey.contactColorStatus().$minus$greater(friend.secondColor()),
                        BundleKey.imgResource().$minus$greater(friend.photoReference()),
                        BundleKey.messageType().$minus$greater(0)
                )));
            }
        });

        mSaveProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Snackbar snack = Snackbar.make(
                        mCoordinator,
                        getResources().getString(R.string.contact_save_photo_snackbar),
                        Snackbar.LENGTH_LONG
                );
                View snackView = snack.getView();
                snackView.setBackgroundResource(R.color.snackBarColor);
                TextView snackText = snackView.findViewById(android.support.design.R.id.snackbar_text);
                snackText.setTextColor(getResources().getColor(R.color.textDarkColor, null));
                snack.show();
            }
        });

        mFilesSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                activity.startActivity(new Intent(activity, FileSendActivity.class).putExtras(SBundle(
                        BundleKey.contactName().$minus$greater(friend.userName()),
                        BundleKey.contactColorPrimary().$minus$greater(friend.color()),
                        BundleKey.contactColorStatus().$minus$greater(friend.secondColor())
                )));
            }
        });

        mDeleteFriend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SimpleDialogDesign dial = new SimpleDialogDesign(
                        activity,
                        getResources().getString(R.string.dialog_delete_friend) + " " +
                                friend.userName() + " " +
                                getResources().getString(R.string.dialog_delete_friend_end),
                        friend.color(), R.drawable.ic_person_black_48dp, null
                );
                dial.show();
            }
        });

        mBlockFriend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Snackbar snack = Snackbar.make(
                        mCoordinator,
                        getResources().getString(R.string.contact_blocked),
                        Snackbar.LENGTH_LONG
                );
                View snackView = snack.getView();
                snackView.setBackgroundResource(R.color.snackBarColor);
                TextView snackText = snackView.findViewById(android.support.design.R.id.snackbar_text);
                snackText.setTextColor(getResources().getColor(R.color.textDarkColor, null));
                snack.show();
            }
        });

        mChangeColor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SimpleColorDialogDesign dial = new SimpleColorDialogDesign(
                        activity,
                        getResources().getString(R.string.dialog_change_color) + " " +
                                friend.userName() + " " +
                                getResources().getString(R.string.dialog_change_color_end),
                        friend.color(), R.drawable.ic_image_color_lens, 0, null
                );
                dial.show();
            }
        });
    }

    int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
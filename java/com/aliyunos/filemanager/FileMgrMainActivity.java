package com.aliyunos.filemanager;

import android.animation.Animator;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.aliyunos.filemanager.core.FileMgrAccount;
import com.aliyunos.filemanager.core.FileMgrClipboard;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.core.FileMgrThumbnailLoader;
import com.aliyunos.filemanager.core.FileMgrTracker;
import com.aliyunos.filemanager.ui.category.CategoryFragment;
import com.aliyunos.filemanager.ui.cloud.CloudFragment;
import com.aliyunos.filemanager.ui.local.LocalFragment;
import com.aliyunos.filemanager.ui.view.ScrollableViewPager;
import com.aliyunos.filemanager.ui.view.TabFragment;
import com.aliyunos.filemanager.ui.view.TabsAdapter;
import com.aliyunos.filemanager.util.PreferenceUtil;

import java.lang.ref.WeakReference;

import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.indicator.HWTabPageSimpleIndicator;
import yunos.support.v4.app.FragmentActivity;
import yunos.support.v4.view.ViewPager;
import android.os.Build;

public class FileMgrMainActivity extends FragmentActivity {
    private static final String TAG = "FileMgrMainActivity";
    private ScrollableViewPager mViewPager;
    private ActionBarView mActionBarView;
    private TabsAdapter mTabsAdapter;
    private HWTabPageSimpleIndicator mIndicator;
    private static WeakReference<FileMgrMainActivity> sActivity = null;
    private boolean mIsSDBusyMsgShown = false;
    private Handler mHander = new Handler();

    private static final String SHAREDPREFERENCES_NAME = "guide_pref";
    private static final String SHAREDPREFERENCES_ISGUIDE = "isGuided";
    private static final String SHAREDPREFERENCES_GUIDETIME = "showTime";
    private static final String ALIYUN_ACTION_CLOUD = "android.intent.action.filemanager.cloud";
    private RelativeLayout mGuideLayout;
    private static boolean mIsShowingGuide = false;
    private Button obtainButton;
    private Button skipButton;
    private static int mOffsetForHeaderAnimation = 0;
    private static final int ANIMATION_DURATION = 300;

    public static enum FragmentTab {
        CategoryTab,
        LocalTab,
        CloudTab
    };
    private FragmentTab mCurrentTab;
    private TabFragment[] mTabFragments = new TabFragment[FragmentTab.values().length];

    public FileMgrMainActivity() {
        super();
        sActivity = new WeakReference<FileMgrMainActivity>(this);
    }

    @Override
    protected void finalize() throws Throwable {
        Log.d(TAG, "finalize");
        super.finalize();
    }

    public FragmentTab getCurrentTab() {
        return mCurrentTab;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appmain_frame);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);

            mActionBarView = new ActionBarView(this);
        }

        mTabsAdapter = new TabsAdapter(this);
        mTabsAdapter.addTab(getResources().getString(R.string.CategoryBrowse), CategoryFragment.class, 0);
        mTabsAdapter.addTab(getResources().getString(R.string.AllFiles), LocalFragment.class, 1);
        /* YUNOS PBASE BEGIN */
        // ##modules(AliFileBrowser): [k3#8153012]
        // ##date:2016-04-15 ##author:jessie.yj
        // ##desc: do not add online tab for cmcc auth.
        // ##topic:cmcc auth
        if(!Build.YUNOS_CARRIER_CMCC) {
            mTabsAdapter.addTab(getResources().getString(R.string.OnlineFiles), CloudFragment.class, 2);
        }
        /* YUNOS PBASE END */

        mViewPager = (ScrollableViewPager)findViewById(R.id.appmain_viewpager);
        mViewPager.setAdapter(mTabsAdapter);

        mIndicator = new HWTabPageSimpleIndicator(this);
        mIndicator.setTabTextSelectedColor(getResources().getColor(R.color.tab_text_select_color));
        mIndicator.setTabTextUnselectColor(getResources().getColor(R.color.tab_text_unselect_color));
        mIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mCurrentTab = FragmentTab.values()[position];
                FileMgrTracker.TA_EnterPage(position + 1, 0);

                TabFragment fragment = (TabFragment) mTabsAdapter.getItem(position);
                fragment.onPageScrolled();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mIndicator.setViewPager(mViewPager);
        mActionBarView.setTabPageIndicator(mIndicator);
        mActionBarView.setBackgroundResource(R.drawable.setting_head);
        mActionBarView.setTitleColor(getResources().getColor(R.color.select_text_color));
        mOffsetForHeaderAnimation = getResources().getDimensionPixelSize(R.dimen.offset_header_animation);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        mCurrentTab = FragmentTab.values()[0];
        refreshActionBarView();

        if (!checkToCloudFile()) {
            checkGuide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FileMgrTracker.TA_ActivityStart(this);
        if (!checkMediaStatus()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mIsSDBusyMsgShown) {// mIsFirstLevel &&
                        if (sActivity.get() != null) {
                            Toast.makeText(sActivity.get(), R.string.sdcard_busy_message,
                                    Toast.LENGTH_SHORT).show();
                            mIsSDBusyMsgShown = true;
                        }
                    }
                    finish();
                }
            }, 500);
        }

        for (int i = 0; i < mTabsAdapter.getCount(); i++) {

            //系统会自动销毁UIView，重新注册一下。
            if(mTabFragments[i] != null) {
                mTabsAdapter.registerItem(mTabFragments[i],i);
            }

            TabFragment fragment = (TabFragment) mTabsAdapter.getItem(i);
            fragment.onActivityResume();
        }
        FileMgrCore.doFirstScan(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FileMgrCore.stopScan();
        FileMgrTracker.TA_ActivityStop(this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        sActivity = null;
        FileMgrThumbnailLoader.getInstance().destroy();
        FileMgrClipboard.getInstance().finishClipboard();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            TabFragment fragment = mTabFragments[mCurrentTab.ordinal()];

            if (fragment != null) {
                if (fragment.onKeyUp(keyCode, event)) {
                    return true;
                }
            }

            this.finish();
            return false;
        }

        return super.onKeyUp(keyCode, event);
    }

    public static void setViewPagerScrollable(boolean scrollable) {
        if (sActivity != null) {
            sActivity.get().mViewPager.setScrollable(scrollable);
        }
    }

    public static void registerTabFragment(int idx, TabFragment fragment) {
        if (sActivity != null) {
            sActivity.get().mTabFragments[idx] = fragment;
        }
    }

    public static void refreshActionBarView() {
        if (sActivity != null) {
            if (!sActivity.get().mViewPager.getScrollable()) {
                return;
            }

            SystemBarColorManager systemBarMgr = new SystemBarColorManager(sActivity.get());

            /*YunOS BEGIN PB*/
            // ##module:(AliFileBrowser) ##author:baorui.br@alibaba-inc.com
            // ##BugID:(8669802) ##date:2016-08-24
            if(mIsShowingGuide) {
                systemBarMgr.setViewFitsSystemWindows(sActivity.get(), false);
                systemBarMgr.setStatusBarColor(0xFFFFFFFF);
                systemBarMgr.setStatusBarDarkMode(sActivity.get(), true);
            }else {
                systemBarMgr.setViewFitsSystemWindows(sActivity.get(), true);
                systemBarMgr.setStatusBarColor(sActivity.get().getResources().getColor(R.color.setting_header_color));
                systemBarMgr.setStatusBarDarkMode(sActivity.get(), sActivity.get().getResources().getBoolean(R.bool.status_dark_mode));
            }
            /*YUNOS END PB*/

            sActivity.get().getActionBar().setCustomView(sActivity.get().mActionBarView,
                    new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
    }

    public void scrollToTab(final FragmentTab tab) {
        mHander.post(new Runnable() {
            @Override
            public void run() {
                mIndicator.setCurrentItem(tab.ordinal());
            }
        });
    }

    private boolean checkMediaStatus() {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED)
                || status.equals(Environment.MEDIA_UNMOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (path != null && status.equals(Environment.MEDIA_UNMOUNTED)) {
                return true;
            }

            return false;
        }
        return true;
    }

    protected  void  checkGuide() {
        mIsShowingGuide = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isNeedShowGuide()) {
                    mHander.post(new Runnable() {
                        @Override
                        public void run() {
                            showGuide();
                        }
                    });
                } else {
                    mHander.post(new Runnable() {
                        @Override
                        public void run() {
                            refreshActionBarView();
                        }
                    });
                }
            }
        }).start();
    }

    protected  boolean isNeedShowGuide() {
        boolean isNeedShowGuide = false;
        SharedPreferences preferences = getSharedPreferences(
                SHAREDPREFERENCES_NAME, MODE_PRIVATE);

        boolean isExpire = false;
        boolean isGuided = preferences.getBoolean(SHAREDPREFERENCES_ISGUIDE, false);
        long prevShowTime = preferences.getLong(SHAREDPREFERENCES_GUIDETIME, System.currentTimeMillis());
        long curTime = System.currentTimeMillis();
        if(curTime- prevShowTime > 7* 24*3600 *1000) {
            isExpire = true;
        }

        boolean isLogined = FileMgrAccount.getsInstance().isSystemAccountLogined();
        /* YUNOS PBASE BEGIN */
        // ##modules(AliFileBrowser): [k3#8153012]
        // ##date:2016-04-15 ##author:jessie.yj
        // ##desc: do not show guide for cmcc auth.
        // ##topic:cmcc auth
        if(!isLogined &&  (!isGuided || (isGuided &&  isExpire)) && !Build.YUNOS_CARRIER_CMCC) {
            isNeedShowGuide = true;
        }
        /* YUNOS PBASE END */

        return isNeedShowGuide;
    }

    protected  void showGuide() {
        FileMgrTracker.TA_EnterPage(4, 0);
        mIsShowingGuide = true;
        mGuideLayout = (RelativeLayout)findViewById(R.id.guide_view);
        mViewPager.setVisibility(View.INVISIBLE);
        mGuideLayout.setVisibility(View.VISIBLE);
        skipButton = (Button)findViewById(R.id.skip_button);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsShowingGuide = false;
                ActionBar actionBar = getActionBar();
                if(null != actionBar) {
                    actionBar.show();
                }

                refreshActionBarView();
                mViewPager.setVisibility(View.VISIBLE);
                mGuideLayout.setVisibility(View.INVISIBLE);
            }
        });

        obtainButton = (Button)findViewById(R.id.obtain_button);
        obtainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsShowingGuide = false;
                ActionBar actionBar = getActionBar();
                if(null != actionBar) {
                    actionBar.show();
                }

                mViewPager.setVisibility(View.VISIBLE);
                mGuideLayout.setVisibility(View.INVISIBLE);
                scrollToTab(FragmentTab.CloudTab);
            }
        });

        ActionBar actionBar = getActionBar();
        if (null != actionBar) {
            actionBar.hide();
        }

        SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        systemBarManager.setViewFitsSystemWindows(this, false);
        systemBarManager.setStatusBarColor(this.getResources().getColor(R.color.setting_header_color));
        /*YunOS BEGIN PB*/
        // ##module:(AliFileBrowser) ##author:baorui.br@alibaba-inc.com
        // ##BugID:(8669802) ##date:2016-08-24
        systemBarManager.setStatusBarDarkMode(this, true);
        /*YunOS END PB*/

        SharedPreferences preferences = getSharedPreferences(
                SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        long curTime = System.currentTimeMillis();
        editor.putBoolean(SHAREDPREFERENCES_ISGUIDE, true);
        editor.putLong(SHAREDPREFERENCES_GUIDETIME, curTime);
        editor.commit();

        PreferenceUtil.setBoolean(PreferenceUtil.Setting.SETTINGS_SHOW_TIPS,true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent !=null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ALIYUN_ACTION_CLOUD)) {
            if (mTabsAdapter.getCount() >=2 && mTabsAdapter.getItem(1) instanceof LocalFragment) {
                LocalFragment fragment = (LocalFragment)mTabsAdapter.getItem(1);
                if (!fragment.checkHasUnFinishTask()) {
                    scrollToTab(FragmentTab.CloudTab);
                }
            }
        }
    }

    private boolean checkToCloudFile() {
        Intent intent = getIntent();
        if (intent!=null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ALIYUN_ACTION_CLOUD) && !Build.YUNOS_CARRIER_CMCC) {
            scrollToTab(FragmentTab.CloudTab);
            return true;
        }

        return false;
    }

    public  static void AnimationActionBarView() {
        if (sActivity != null) {
            if (!sActivity.get().mViewPager.getScrollable()) {
                return;
            }

            SystemBarColorManager systemBarMgr = new SystemBarColorManager(sActivity.get());
            systemBarMgr.setViewFitsSystemWindows(sActivity.get(), true);
            systemBarMgr.setStatusBarColor(sActivity.get().getResources().getColor(R.color.setting_header_color));
            systemBarMgr.setStatusBarDarkMode(sActivity.get(), sActivity.get().getResources().getBoolean(R.bool.status_dark_mode));

            sActivity.get().getActionBar().setCustomView(sActivity.get().mActionBarView,
                    new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            sActivity.get().mActionBarView.setVisibility(View.VISIBLE);
            sActivity.get().mActionBarView.setAlpha(0);
            sActivity.get().mActionBarView.setBackgroundResource(R.color.setting_header_color);
            sActivity.get().mActionBarView.setY(sActivity.get().mActionBarView.getY() - mOffsetForHeaderAnimation);
            sActivity.get().mActionBarView.animate().alpha(1).translationYBy(mOffsetForHeaderAnimation)
                    .setInterpolator(new DecelerateInterpolator())
                    .setDuration(ANIMATION_DURATION/2)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            sActivity.get().getActionBar().setBackgroundDrawable(null);
                            sActivity.get().mActionBarView.setBackgroundResource(R.drawable.setting_head);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    }).start();
        }
    }

}

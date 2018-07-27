package com.android.launcher3.snail.custompage;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.launcher3.CellLayout;
import com.android.launcher3.R;
import com.android.launcher3.snail.custompage.ImagePagerAdapter.OnChildClickListener;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.util.ArrayList;
import java.util.List;

public class CustomPage extends CellLayout implements OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String ACTION_FREE_STORE = "com.ireadygo.app.freestore.STORE_DETAIL";
    private static final String EXTRA_OUTSIDE_TAG = "SELECT_NO";
    private static final String ACTION_BANNER_UPDATE = "com.ireadygo.app.muchnotes.ACTION_INFO_BANNER_CHANGE";
    private static final String ACTION_BANNER_CLICK = "com.ireadygo.app.muchnotes.ACTION_BANNER_CLICK";
    private static final String EXTRA_POSITION = "EXTRA_POSITION";
    private static final String EXTRA_INDEX = "EXTRA_INDEX";
    private static final String ACTION_GET_BANNER = "com.ireadygo.app.muchnotes.ACTION_GET_BANNER";
    private static final String EXTRA_MANNUAL = "EXTRA_MANNUAL";
    private static final int BANNER_LEFT = 1;
    private static final int BANNER_RIGHT = 2;
    private static final String URL_DIVIDER = ",";
    private static final String EXTRA_AUTO_URL = "EXTRA_AUTO_URL";
    private static final String EXTRA_STATIC_URL = "EXTRA_STATIC_URL";
    private static final long BANNER_SCROLL_DELAY = 8 * 1000;
    private static final int MSG_FETCH_BANNER_MANNUAL_TIME_OUT = 100;
    private static final int MSG_FETCH_BANNER_MANNUAL = 101;
    private static final long DELAY_FETCH_BANNER_TIME_OUT = 6 * 1000;// 6s
    private static final long DELAY_FETCH_BANNER = 2 * 1000;// 6s
    private static final int MSG_CLICK_LEFT = 102;
    private static final int MSG_CLICK_RIGHT = 103;
    private static final long DELAY_SET_ALPHA = 2000;
    private AutoScrollViewPager mAutoScrollViewPager;
    private ImageView mAdImageRT;
    private ImageView mAdImageRB;
    private ViewGroup mDotsLayout;
    private List<String> mAdLeftBannerUrlList = new ArrayList<String>();
    private List<String> mAdRightBannerUrlList = new ArrayList<String>();
    private int mCurrentSelectedIndex = 0;
    private ArrayList<ImageView> mDotViewList = new ArrayList<ImageView>();
    private ImagePagerAdapter mImagePagerAdapter;
    private ImageLoader mImageLoader;

    private Drawable mDefaultDrawable;
    private Drawable mEmptyDrawable;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ImageView mLeftArrow;
    private ImageView mRightArrow;
    private TextView mBadgeView;
    private static final String CONTENT_URI = "content://com.ireadygo.app.muchnotes.gifts.provider/UNREADCOUNT";
    private static final String ACTION_UNREAD_MESSAGE = "com.ireadygo.app.gamelauncher.action.unread.message";
    private static final String KEY_UNREAD_COUNT = "key_unread_count";

    public CustomPage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomPage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomPage(Context context) {
        super(context);
    }

    //add by linmq 2018-6-26 custompage cannot find the position
    @Override
    public int[] performReorder(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY, View dragView,
            int[] result, int[] resultSpan, int mode) {
        return new int[] { -1, -1 };
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.store_recommend:
            jumpToFreeStoreMenu(MuchAction.ACTION_RECOMM_AND_RANK, 0);
            break;
        case R.id.store_category:
            jumpToFreeStoreMenu(MuchAction.ACTION_RECOMM_AND_RANK,1);
            break;
        case R.id.store_collection:
            jumpToFreeStoreMenu(MuchAction.ACTION_MUCH_CIRCLE);
            break;
        case R.id.store_search:
            jumpToFreeStoreMenu(MuchAction.ACTION_SEARCH);
            break;
        case R.id.store_manager:
            jumpToFreeStoreMenu(MuchAction.ACTION_DOWNLOAD_MANAGER);
            break;
        case R.id.store_user:
            jumpToFreeStoreMenu(MuchAction.ACTION_GIFT);
            break;
        case R.id.store_ad_right_1:
            clickFreeStoreBanner(BANNER_RIGHT, 0);
            break;
        case R.id.store_ad_right_2:
            clickFreeStoreBanner(BANNER_RIGHT, 1);
            break;
        default:
            break;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mImageLoader = ImageLoader.getInstance();
        configImageLoader();
        mDefaultDrawable = getContext().getResources().getDrawable(R.drawable.store_banner_default);
        mEmptyDrawable = getContext().getResources().getDrawable(R.drawable.store_ad_large);
        initView();
        initData();
    }

    private void initData() {
        int unReadCount = queryUnReadCount();
        updateUnReadMesCount(unReadCount);
    }

    private int queryUnReadCount() {
        Uri uri = Uri.parse(CONTENT_URI);
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = getContext().getContentResolver().query(uri,
                    null,
                    null,
                    null,
                    null);
            if (cursor != null) {
                count = cursor.getCount();
            }
        } catch (Exception e) {
            Log.e("lmq", "e = " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    private void initView() {
        View recommendBtn = (View) findViewById(R.id.store_recommend);
        initItem(recommendBtn, R.string.much_store_recommend_title, R.drawable.store_recommend_selector);

        View cotegoryBtn = (View) findViewById(R.id.store_category);
        initItem(cotegoryBtn, R.string.much_store_rank_title, R.drawable.store_rank_selector);

        View collectionBtn = (View) findViewById(R.id.store_collection);
        initItem(collectionBtn, R.string.much_store_circle_title, R.drawable.store_circle_selector);

        View searchBtn = (View) findViewById(R.id.store_search);
        initItem(searchBtn, R.string.much_store_search_title, R.drawable.store_search_selector);

        View managerBtn = (View) findViewById(R.id.store_manager);
        initItem(managerBtn, R.string.much_store_manage_title, R.drawable.store_manage_selector);

        View userBtn = (View) findViewById(R.id.store_user);
        initItem(userBtn, R.string.much_store_gift_bag_title, R.drawable.store_giftbag_selector);

        mDotsLayout = (ViewGroup) findViewById(R.id.storeRecommendDots);
        mAdImageRT = (ImageView) findViewById(R.id.store_ad_right_1);
        mAdImageRB = (ImageView) findViewById(R.id.store_ad_right_2);
        mAdImageRT.setOnClickListener(this);
        mAdImageRB.setOnClickListener(this);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorScheme(android.R.color.holo_orange_light,
                android.R.color.holo_orange_dark,
                android.R.color.holo_orange_light,
                android.R.color.holo_orange_dark);
        mLeftArrow = (ImageView)findViewById(R.id.left);
        mRightArrow = (ImageView)findViewById(R.id.right);
        mLeftArrow.setOnClickListener(mOnClickListener);
        mRightArrow.setOnClickListener(mOnClickListener);
        
    }

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.left:
                mLeftArrow.setAlpha(1.0f);
                if (mAutoScrollViewPager != null) {
                    mAutoScrollViewPager.stopAutoScroll();
                    mAutoScrollViewPager.scrollLeft();
                }
                postMsg(MSG_CLICK_LEFT, DELAY_SET_ALPHA);
                break;
            case R.id.right:
                mRightArrow.setAlpha(1.0f);
                if (mAutoScrollViewPager != null) {
                    mAutoScrollViewPager.stopAutoScroll();
                    mAutoScrollViewPager.scrollRight();
                }
                postMsg(MSG_CLICK_RIGHT, DELAY_SET_ALPHA);
                break;

            default:
                break;
            }
        }
    };

    @SuppressLint("ResourceType")
    private void initItem(View item, int titleId, int iconId) {
        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) item
                    .getLayoutParams();
            params.rightMargin = 57;//modify by linmq 2017-6-6 垂直方向item距离
            item.setLayoutParams(params);
        }
        item.setOnClickListener(this);
        TextView title = (TextView) item.findViewById(R.id.store_title);
        title.setText(titleId);
        ImageView icon = (ImageView) item.findViewById(R.id.store_icon);
        icon.setImageResource(iconId);
        if (item.getId() == R.id.store_user) {
            mBadgeView = (TextView) item.findViewById(R.id.store_mes_badge);
//            mBadgeView.setVisibility(View.VISIBLE);
        } else {
            //隐藏其他的角标
            TextView otherBadge = (TextView) item.findViewById(R.id.store_mes_badge);
            otherBadge.setVisibility(View.GONE);
        }
    }

    private void updateAutoViewPaper() {
        if (!mAdLeftBannerUrlList.isEmpty()) {
            mAutoScrollViewPager = (AutoScrollViewPager) findViewById(R.id.storeRecommendViewPager);
            mImagePagerAdapter = new ImagePagerAdapter(getContext(), mAdLeftBannerUrlList, mImageLoader);
            mImagePagerAdapter.setOnClickListener(mLeftBannerOnClickListener);
            mImagePagerAdapter.setInfiniteLoop(false);
            mAutoScrollViewPager.setOnPageChangeListener(mAutoScrollListener);
            mAutoScrollViewPager.setAdapter(mImagePagerAdapter);
            mAutoScrollViewPager.setCurrentItem(0);
            mAutoScrollViewPager.setBorderAnimation(true);
            mAutoScrollViewPager.setScrollDurationFactor(10.0);
            mAutoScrollViewPager.setInterval(BANNER_SCROLL_DELAY);
            mAutoScrollViewPager.startAutoScroll();
        }
    }

    private void initBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_BANNER_UPDATE);
        intentFilter.addAction(ACTION_UNREAD_MESSAGE);
        getContext().registerReceiver(mReceiver, intentFilter);
    }

    private void updateLeftAdBanner() {
        updateAutoViewPaper();
        if (mImagePagerAdapter != null) {
            mImagePagerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initBroadcast();
        fetchBannerImage(false);
    };

    @Override
    protected void onDetachedFromWindow() {
        getContext().unregisterReceiver(mReceiver);
        mImageLoader.clearMemoryCache();
        if (mAutoScrollViewPager != null) {
            mAutoScrollViewPager.stopAutoScroll();
        }
        super.onDetachedFromWindow();
    }

    private void updateRightAdBanner() {
        if (mAdRightBannerUrlList.size() >= 2) {
            mImageLoader.displayImage(mAdRightBannerUrlList.get(0), mAdImageRT, getDisplayImageOptions());
            mImageLoader.displayImage(mAdRightBannerUrlList.get(1), mAdImageRB, getDisplayImageOptions());
        }
    }

    private void updateDotLayout() {
        if (!mAdLeftBannerUrlList.isEmpty()) {
            mDotViewList.clear();
            mDotsLayout.removeAllViews();
            int count = mAdLeftBannerUrlList.size();
            for (int i = 0; i < count; i++) {
                ImageView dot = new ImageView(getContext());
                dot.setImageResource(R.drawable.snail_page_indicator_normal);
                dot.setPadding(8, 8, 8, 8);
                mDotViewList.add(dot);
                mDotsLayout.addView(dot);
            }
            mDotViewList.get(0).setImageResource(R.drawable.snail_page_indicator_selected);
        }
    }

    private OnChildClickListener mLeftBannerOnClickListener = new OnChildClickListener() {
        @Override
        public void onItemChildViewClick(View view, int index) {
            clickFreeStoreBanner(BANNER_LEFT, index);
        }
    };

    // modify by linmq 2017-12-20
    private void jumpToFreeStoreMenu(String action, int ...extra) {
        Intent intent = new Intent(action);
        if (extra.length > 0) {
            intent.putExtra(EXTRA_OUTSIDE_TAG, extra[0]);
        }
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // do nothing
        }
    }
    // add by linmq 2017-12-20
    private void showNoService() {
        Toast.makeText(getContext(), getResources().getString(R.string.no_service_tip), Toast.LENGTH_LONG).show();
    }

    private void clickFreeStoreBanner(int position, int index) {
        Intent intent = new Intent(ACTION_BANNER_CLICK);
        intent.putExtra(EXTRA_POSITION, position);
        intent.putExtra(EXTRA_INDEX, index);
        getContext().sendBroadcast(intent);
    }

    private OnPageChangeListener mAutoScrollListener = new OnPageChangeListener() {
        @Override
        public void onPageSelected(int index) {
            if (mAdLeftBannerUrlList.size() == 0) {
                return;
            }
            if (mDotViewList == null || mDotViewList.size() == 0) {
                return;
            }
            int actualIndex = index % mAdLeftBannerUrlList.size();
            // 2018-2-7 修复越界问题
            if(actualIndex < mDotViewList.size() && mCurrentSelectedIndex < mDotViewList.size()){
                mDotViewList.get(mCurrentSelectedIndex).setImageResource(R.drawable.snail_page_indicator_normal);
                mDotViewList.get(actualIndex).setImageResource(R.drawable.snail_page_indicator_selected);
                mCurrentSelectedIndex = actualIndex;
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_BANNER_UPDATE.equals(action)) {
                if (mHandler.hasMessages(MSG_FETCH_BANNER_MANNUAL_TIME_OUT)) {
                    mHandler.removeMessages(MSG_FETCH_BANNER_MANNUAL_TIME_OUT);
                }
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
                String autoUrl = intent.getStringExtra(EXTRA_AUTO_URL);
                String staticUrl = intent.getStringExtra(EXTRA_STATIC_URL);
                decodeAutoBannerUrl(autoUrl);
                decodeStaticBannerUrl(staticUrl);
                updateData();
            } else if (ACTION_UNREAD_MESSAGE.equals(action)) {
                int count = intent.getIntExtra(KEY_UNREAD_COUNT, 0);
                updateUnReadMesCount(count);
            }
        }
    };

    private void updateUnReadMesCount(int count) {
        if (mBadgeView == null) {
            return;
        }
        if (count > 0) {
            mBadgeView.setVisibility(View.VISIBLE);
            mBadgeView.setText(String.valueOf(count));
        } else {
            mBadgeView.setVisibility(View.GONE);
        }
    }
    private void updateData() {
        updateLeftAdBanner();
        updateDotLayout();
        updateRightAdBanner();
        if (mAdLeftBannerUrlList == null || mAdLeftBannerUrlList.isEmpty()) {
            if (mAutoScrollViewPager != null) {
                mAutoScrollViewPager.setVisibility(View.INVISIBLE);
            }
            mDotsLayout.setVisibility(View.INVISIBLE);
        } else {
            if (mAutoScrollViewPager != null) {
                mAutoScrollViewPager.setVisibility(View.VISIBLE);
            }
            mDotsLayout.setVisibility(View.VISIBLE);
        }
    }

    private void decodeAutoBannerUrl(String url) {
        mAdLeftBannerUrlList.clear();
        if (TextUtils.isEmpty(url)) {
            mAdLeftBannerUrlList.add("");
            return;
        }
        String[] urls = url.split(URL_DIVIDER);
        for (String bannerUrl : urls) {
            mAdLeftBannerUrlList.add(bannerUrl);
        }
    }

    private void decodeStaticBannerUrl(String url) {
        mAdRightBannerUrlList.clear();
        if (TextUtils.isEmpty(url)) {
            mAdRightBannerUrlList.add("");
            return;
        }
        String[] urls = url.split(URL_DIVIDER);
        for (String bannerUrl : urls) {
            mAdRightBannerUrlList.add(bannerUrl);
        }
    }

    private void fetchBannerImage(boolean isMannual) {
        Intent intent = new Intent(ACTION_GET_BANNER);
        intent.putExtra(EXTRA_MANNUAL, isMannual);
        getContext().sendBroadcast(intent);
    }

    private void configImageLoader() {
        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        DisplayImageOptions options = getDisplayImageOptions();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getContext())
                .memoryCacheSize(am.getMemoryClass() * 1024 * 1024 / 8).threadPoolSize(5)
                .denyCacheImageMultipleSizesInMemory().discCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO).threadPriority(Thread.MIN_PRIORITY)
                .defaultDisplayImageOptions(options).build();
        mImageLoader.init(config);
    }

    public DisplayImageOptions getDisplayImageOptions() {
        DisplayImageOptions options = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisc(true)
                .bitmapConfig(Bitmap.Config.RGB_565).showImageOnFail(mDefaultDrawable)
                .showImageOnLoading(mDefaultDrawable).showImageForEmptyUri(mEmptyDrawable).build();
        return options;
    }

    //add by linmq 2017-12-20
    public enum Destination {
        GAME_DETAIL, STORE_RECOMMEND, STORE_CATEGORY, STORE_COLLECTION, STORE_SEARCH, STORE_GAME_MANAGE,//
        STORE_SETTINGS, COLLECTION_DETAIL, CATEGORY_DETAIL, ACCOUNT_WEALTH, ACCOUNT_PERSONAL, ACCOUNT_RECHARGE, ACCOUNT_FREECARD
    }

    @Override
    public void onRefresh() {
       postMsg(MSG_FETCH_BANNER_MANNUAL, DELAY_FETCH_BANNER);
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_FETCH_BANNER_MANNUAL:
                fetchBannerImage(true);
                postMsg(MSG_FETCH_BANNER_MANNUAL_TIME_OUT, DELAY_FETCH_BANNER_TIME_OUT);
                break;
            case MSG_FETCH_BANNER_MANNUAL_TIME_OUT:
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
                break;
            case MSG_CLICK_LEFT:
                mLeftArrow.setAlpha(0.2f);
                if (mAutoScrollViewPager != null) {
                    mAutoScrollViewPager.startAutoScroll();
                }
                break;
            case MSG_CLICK_RIGHT:
                mRightArrow.setAlpha(0.2f);
                if (mAutoScrollViewPager != null) {
                    mAutoScrollViewPager.startAutoScroll();
                }
                break;
            default:
                break;
            }
        };
    };

    private void postMsg(int msgTag, long delay) {
        if (mHandler.hasMessages(msgTag)) {
            mHandler.removeMessages(msgTag);
        }
        Message msg = mHandler.obtainMessage(msgTag);
        mHandler.sendMessageDelayed(msg, delay);
    }

    //add by linmq 2017-8-7 自定义页与hotseat焦点切换问题
    public View getFocusView(int hoseatButtonIndex) {
        if (hoseatButtonIndex > 2) {
            return mAdImageRT;
        } else if (hoseatButtonIndex < 2) {
            return findViewById(R.id.store_user);
        } else {
            return mAdImageRB;
        }
    }

    // add by linmq 2017-12-20
    static final class MuchAction {
        private static final String ACTION_RECOMM_AND_RANK = "com.ireadygo.app.muchnotes.GAME";
        private static final String ACTION_SEARCH = "com.ireadygo.app.muchnotes.SEARCH";
        private static final String ACTION_DOWNLOAD_MANAGER = "com.ireadygo.app.muchnotes.DOWNLOADMANAGER";
        private static final String ACTION_GIFT = "com.ireadygo.app.muchnotes.GIFTMANAGER";
        private static final String ACTION_MUCH_CIRCLE = "com.ireadygo.app.muchnotes.MOMENT";
    }
}

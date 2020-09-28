package com.candy.capture.activity;

import android.os.Bundle;
import androidx.cardview.widget.CardView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.candy.capture.R;
import com.candy.capture.core.ConstantValues;
import com.candy.capture.core.DBHelper;
import com.candy.capture.fragment.ContentListFragment;
import com.candy.capture.util.CommonTools;
import com.candy.capture.util.DensityUtil;
import com.jaeger.library.StatusBarUtil;

import java.util.ArrayList;

public class SearchActivity extends BaseActivity implements View.OnClickListener, ContentListFragment.OnFragmentInteractionListener {

    private View mRootView;

    private ImageView mBackIv;
    private EditText mKeyWordTv;
    private ImageView mCleanIv;

    private ScrollView mScrollView;
    private LinearLayout mSearchHistoryLl;

    private LinearLayout mCategoryLl;
    private LinearLayout mCategoryTextLl;
    private LinearLayout mCategoryAudioLl;
    private LinearLayout mCategoryPhotoLl;
    private LinearLayout mCategoryVideoLl;

    private CardView mLocationCv;
    private LinearLayout mLocationLl;

    private DBHelper mDBHelper;

    private ArrayList<String> mCityList;
    private ArrayList<String> mHistoryList;

    private ContentListFragment mContentListFragment;
    /**
     * 标识是否处于编辑模式
     */
    private boolean mIsInEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        findView();
        setView();

        mDBHelper = new DBHelper(mContext);
        getData();

        if (savedInstanceState == null) {
            mContentListFragment = ContentListFragment.newInstance(true);
            getSupportFragmentManager().beginTransaction().add(R.id.fl_result, mContentListFragment, "content_list").commit();
        } else {
            mContentListFragment = (ContentListFragment) getSupportFragmentManager().findFragmentByTag("content_list");
        }

        StatusBarUtil.setLightMode(this);
        StatusBarUtil.setColor(this, getResources().getColor(R.color.colorWhite), 0);
    }

    private void findView() {
        mRootView = findViewById(R.id.activity_search);

        mBackIv = (ImageView) findViewById(R.id.iv_back);
        mKeyWordTv = (EditText) findViewById(R.id.et_key_word);
        mCleanIv = (ImageView) findViewById(R.id.iv_clean);

        mScrollView = (ScrollView) findViewById(R.id.scroll_view);

        mSearchHistoryLl = (LinearLayout) findViewById(R.id.ll_history);

        mCategoryLl = (LinearLayout) findViewById(R.id.ll_category);
        mCategoryTextLl = (LinearLayout) findViewById(R.id.ll_text);
        mCategoryAudioLl = (LinearLayout) findViewById(R.id.ll_audio);
        mCategoryPhotoLl = (LinearLayout) findViewById(R.id.ll_photo);
        mCategoryVideoLl = (LinearLayout) findViewById(R.id.ll_video);

        mLocationCv = (CardView) findViewById(R.id.cv_location);
        mLocationLl = (LinearLayout) findViewById(R.id.ll_location);
    }

    private void setView() {
        mBackIv.setOnClickListener(this);
        mKeyWordTv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (!TextUtils.isEmpty(mKeyWordTv.getText())) {
                        mCleanIv.setVisibility(View.VISIBLE);
                    }
                } else {
                    mCleanIv.setVisibility(View.GONE);
                }
            }
        });
        mKeyWordTv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mKeyWordTv.hasFocus()) {
                    if (!TextUtils.isEmpty(mKeyWordTv.getText())) {
                        mCleanIv.setVisibility(View.VISIBLE);
                    } else {
                        mCleanIv.setVisibility(View.GONE);
                    }
                }
            }
        });
        mKeyWordTv.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (EditorInfo.IME_ACTION_SEARCH == actionId) {
                    String keyword = String.valueOf(mKeyWordTv.getText());
                    if (mContentListFragment != null && !TextUtils.isEmpty(keyword) && !TextUtils.isEmpty(keyword.trim())) {
                        search(keyword);
                    }
                }
                return true;
            }
        });
        mCleanIv.setOnClickListener(this);

        mCategoryTextLl.setOnClickListener(this);
        mCategoryAudioLl.setOnClickListener(this);
        mCategoryPhotoLl.setOnClickListener(this);
        mCategoryVideoLl.setOnClickListener(this);
    }

    private void getData() {
        getHistory();
        getCities();
    }

    private void getHistory() {
        mHistoryList = mDBHelper.getSearchHistory();
        if (mHistoryList != null && !mHistoryList.isEmpty()) {
            mSearchHistoryLl.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtil.dip2px(mContext, 50));
            for (final String history : mHistoryList) {
                View view = View.inflate(this, R.layout.item_history, null);
                TextView keywordTv = (TextView) view.findViewById(R.id.tv_keyword);
                keywordTv.setText(history);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        search(history);
                    }
                });
                mSearchHistoryLl.addView(view, params);
            }
        } else {
            mSearchHistoryLl.setVisibility(View.GONE);
        }
    }

    private void getCities() {
        mCityList = mDBHelper.getAllCity();
        if (mCityList != null && !mCityList.isEmpty()) {
            mLocationCv.setVisibility(View.VISIBLE);
            mLocationLl.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtil.dip2px(mContext, 50));
            for (final String city : mCityList) {
                View view = View.inflate(this, R.layout.item_city, null);
                TextView cityTv = (TextView) view.findViewById(R.id.tv_city);
                cityTv.setText(city);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        filterCity(city);
                    }
                });
                mLocationLl.addView(view, params);
            }
        } else {
            mLocationCv.setVisibility(View.GONE);
            mLocationLl.setVisibility(View.GONE);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.iv_clean:
                mKeyWordTv.setText(null);
                break;
            case R.id.ll_text:
                mKeyWordTv.setText(getString(R.string.pure_text));
                filterCategory(ConstantValues.CONTENT_TYPE_TEXT);
                break;
            case R.id.ll_audio:
                mKeyWordTv.setText(getString(R.string.audio));
                filterCategory(ConstantValues.CONTENT_TYPE_AUDIO);
                break;
            case R.id.ll_photo:
                mKeyWordTv.setText(getString(R.string.photo));
                filterCategory(ConstantValues.CONTENT_TYPE_PHOTO);
                break;
            case R.id.ll_video:
                mKeyWordTv.setText(getString(R.string.video));
                filterCategory(ConstantValues.CONTENT_TYPE_VIDEO);
                break;
        }
    }

    private void search(String keyword) {
        CommonTools.hideSoftKeyboard(SearchActivity.this);

        mKeyWordTv.setText(keyword);
        mScrollView.setVisibility(View.GONE);
        mSearchHistoryLl.setVisibility(View.GONE);
        mCleanIv.setVisibility(View.GONE);

        mDBHelper.insertSearchHistory(keyword);
        mContentListFragment.search(keyword, ContentListFragment.SEARCH_TYPE_SEARCH_DESC);
    }

    private void filterCategory(int type) {
        CommonTools.hideSoftKeyboard(SearchActivity.this);

        mScrollView.setVisibility(View.GONE);
        mSearchHistoryLl.setVisibility(View.GONE);
        mCleanIv.setVisibility(View.GONE);

        mContentListFragment.search(String.valueOf(type), ContentListFragment.SEARCH_TYPE_SEARCH_CATEGORY);
    }

    private void filterCity(String city) {
        CommonTools.hideSoftKeyboard(SearchActivity.this);

        mKeyWordTv.setText(city);
        mScrollView.setVisibility(View.GONE);
        mSearchHistoryLl.setVisibility(View.GONE);
        mCleanIv.setVisibility(View.GONE);

        mContentListFragment.search(city, ContentListFragment.SEARCH_TYPE_SEARCH_CITY);
    }

    @Override
    public void onBackPressed() {
        if (mIsInEditMode) {
            exitEditMode();
            if (mContentListFragment != null) {
                mContentListFragment.exitEditMode(true);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void gotoEditMode() {
        mIsInEditMode = true;
        // 改变菜单项
        invalidateOptionsMenu();
    }

    @Override
    public void exitEditMode() {
        mIsInEditMode = false;
        // 改变菜单项
        invalidateOptionsMenu();
    }
}

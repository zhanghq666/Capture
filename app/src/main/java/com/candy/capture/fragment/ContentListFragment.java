package com.candy.capture.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.candy.capture.R;
import com.candy.capture.activity.VideoPlayerActivity;
import com.candy.capture.adapter.ContentListAdapter;
import com.candy.capture.core.ConstantValues;
import com.candy.capture.core.DBHelper;
import com.candy.capture.customview.ContentListDivider;
import com.candy.capture.customview.ImageViewerDialog;
import com.candy.capture.model.Content;
import com.candy.capture.model.MediaPlayState;
import com.candy.capture.util.TipsUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ContentListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ContentListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContentListFragment extends Fragment implements ContentListAdapter.ItemClickCallback, ContentListAdapter.AudioPlayCallBack {

    private static final String TAG = "ContentListFragment";

    public static final int SEARCH_TYPE_NO_SEARCH = 0;
    public static final int SEARCH_TYPE_SEARCH_DESC = 1;
    public static final int SEARCH_TYPE_SEARCH_CATEGORY = 2;
    public static final int SEARCH_TYPE_SEARCH_CITY = 3;


    // 列表获取更多时用到的提前量
    private static final int LIST_BUFFER_THRESHOLD = 3;

    private static final String ARG_IS_SEARCH = "param_is_search";

    private boolean mIsSearch;
    private String mKerWord;
    private int mSearchType;

    private OnFragmentInteractionListener mListener;

    private DBHelper mDBHelper;
    private static final int COUNT = 20;

    private RecyclerView mContentsRv;
    /**
     * 内容集合
     */
    private ArrayList<Content> mContentList;
    /**
     * 编辑状态下选中内容项集合
     */
    private ArrayList<Content> mSelectedContents;
    /**
     * RecyclerView Adapter
     */
    private ContentListAdapter mContentListAdapter;

    /**
     * 音频MediaPlayer实例
     */
    private MediaPlayer mMediaPlayer;
    /**
     * 音频MediaPlayer状态
     */
    private MediaPlayState mPlayState;
    /**
     * 正在播放音频的列表项索引
     */
    private int mCurrentPlayingItemIndex = -1;
    private boolean mIsLoadingData;
    private boolean mHasMore;
    /**
     * 标识是否处于编辑模式
     */
    private boolean mIsInEditMode;
    private ProgressDialog mProgressDialog;

    public ContentListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param isSearch 是否搜索
     * @return A new instance of fragment ContentListFragment.
     */
    public static ContentListFragment newInstance(boolean isSearch) {
        ContentListFragment fragment = new ContentListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_SEARCH, isSearch);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mIsSearch = getArguments().getBoolean(ARG_IS_SEARCH);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_content_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDBHelper = new DBHelper(getContext());
        mContentList = new ArrayList<>();
        findView(view);
        setView();

        if (!mIsSearch) {
            getContents();
        }
    }

    private void findView(View rootView) {
        mContentsRv = (RecyclerView) rootView.findViewById(R.id.rv_contents);
    }

    private void setView() {
        mContentListAdapter = new ContentListAdapter(getContext(), mContentList, this, this);
        mContentsRv.setAdapter(mContentListAdapter);
        mContentsRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mContentsRv.addItemDecoration(new ContentListDivider(getContext()));
        mContentsRv.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                if (mHasMore && !mIsLoadingData && totalItemCount < (lastVisibleItem + LIST_BUFFER_THRESHOLD)) {
                    getContents();
                }
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    //region 获取内容
    public void getNewerContents() {
        mIsLoadingData = true;

        ArrayList<Content> contents = null;
        if (mContentList.isEmpty()) {
            contents = mDBHelper.getContentNewer(-1, COUNT);
        } else {
            contents = mDBHelper.getContentNewer(mContentList.get(0).getId(), COUNT);
        }
        mContentList.addAll(0, contents);
        int count = contents.size();
        mContentListAdapter.notifyItemRangeInserted(0, count);

        mIsLoadingData = false;
    }

    public void search(String keyWord, int searchType) {
        mKerWord = keyWord;
        mSearchType = searchType;

        if (!mContentList.isEmpty()) {
            mContentList.clear();
            mContentListAdapter.notifyDataSetChanged();
        }

        getContents();
    }

    private void getContents() {
        mIsLoadingData = true;
        if (mContentList.isEmpty()) {
            if (mProgressDialog == null) {
                mProgressDialog = ProgressDialog.show(getContext(), null, null, true, true);
            } else {
                mProgressDialog.show();
            }
        }

        ArrayList<Content> contents = null;
        switch (mSearchType) {
            case SEARCH_TYPE_NO_SEARCH:
                int startId = mContentList.isEmpty() ? -1 : mContentList.get(mContentList.size() - 1).getId();
                contents = mDBHelper.getContentOlder(startId, COUNT);
                break;
            case SEARCH_TYPE_SEARCH_CATEGORY:
                try {
                    int type = Integer.parseInt(mKerWord);
                    contents = mDBHelper.searchByType(type, COUNT);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                break;
            case SEARCH_TYPE_SEARCH_CITY:
                contents = mDBHelper.searchByCity(mKerWord, COUNT);
                break;
            case SEARCH_TYPE_SEARCH_DESC:
                contents = mDBHelper.searchByDesc(mKerWord, COUNT);
                break;
        }
        if (contents != null && contents.size() > 0) {
            int startPosition = mContentList.isEmpty() ? 0 : mContentList.size() - 1;
            int count = contents.size();
            mContentList.addAll(contents);
            mContentListAdapter.notifyItemRangeInserted(startPosition, count);
        }

        mHasMore = contents != null && contents.size() == COUNT;

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mIsLoadingData = false;
    }
    //endregion

    //region 列表项事件回调
    @Override
    public void onItemClick(int position) {
        if (mIsInEditMode) {
            mContentList.get(position).setSelect(!mContentList.get(position).isSelect());
            if (mSelectedContents == null)
                mSelectedContents = new ArrayList<>();
            if (mContentList.get(position).isSelect()) {
                mSelectedContents.add(mContentList.get(position));
            } else {
                mSelectedContents.remove(mContentList.get(position));
            }
            if (mSelectedContents.isEmpty()) {
                exitEditMode(false);
            } else {
                mContentListAdapter.notifyItemChanged(position);
            }
        }
    }

    @Override
    public void onItemLongPressed(int position) {
        if (mIsSearch)
            return;
        if (!mIsInEditMode) {
            mContentList.get(position).setSelect(true);
            if (mSelectedContents == null)
                mSelectedContents = new ArrayList<>();
            mSelectedContents.add(mContentList.get(position));
            gotoEditMode();
        }
    }

    @Override
    public void onAudioViewClick(int position) {
        if (mIsInEditMode) {
            onItemClick(position);
            return;
        }
        if (mCurrentPlayingItemIndex == position) {
            if (MediaPlayState.STARTED == mPlayState) {
                stopMedia();
            } else if (MediaPlayState.COMPLETED == mPlayState || MediaPlayState.PREPARED == mPlayState) {
                playMedia();
            } else if (MediaPlayState.STOPPED == mPlayState) {
                prepareMediaPlayer();
                playMedia();
            }
        } else {
            if (mCurrentPlayingItemIndex >= 0) {
                if (MediaPlayState.STARTED == mPlayState) {
                    stopMedia();
                }

                mMediaPlayer.reset();
                mPlayState = MediaPlayState.IDLE;
            }

            mCurrentPlayingItemIndex = position;

            setMediaPlayerSource();
            prepareMediaPlayer();
            playMedia();
        }
    }

    @Override
    public void onImageViewClick(int position) {
        if (mIsInEditMode) {
            onItemClick(position);
            return;
        }
        ArrayList<String> list = new ArrayList<>();
        list.add(mContentList.get(position).getMediaFilePath());
        ImageViewerDialog.showDialog(getContext(), list, 1);
    }

    @Override
    public void onVideoViewClick(int position) {
        if (mIsInEditMode) {
            onItemClick(position);
            return;
        }

        if (MediaPlayState.STARTED == mPlayState) {
            stopMedia();
        }

        Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_MEDIA_PATH_KEY, mContentList.get(position).getMediaFilePath());
        startActivity(intent);
    }

    private void gotoEditMode() {
        mIsInEditMode = true;
        if (mListener != null) {
            mListener.gotoEditMode();
        }
        // 停止播放音频、释放资源
        releaseMedia();
        mCurrentPlayingItemIndex = -1;
        // 列表显示变化
        mContentListAdapter.setEditMode(true);
        mContentListAdapter.notifyDataSetChanged();
    }

    public void exitEditMode(boolean fromParent) {
        mIsInEditMode = false;

        if (!fromParent) {
            if (mListener != null) {
                mListener.exitEditMode();
            }
        }
        // 重新组织MediaPlayer
        initMediaPlayer();
        // 清空已选项集合
        if (mSelectedContents != null && !mSelectedContents.isEmpty()) {
            for (Content content : mSelectedContents) {
                content.setSelect(false);
            }
            mSelectedContents.clear();
        }
        // 列表显示变化
        mContentListAdapter.setEditMode(false);
        mContentListAdapter.notifyDataSetChanged();
    }
    //endregion

    //region 语音播放相关
    @Override
    public void onResume() {
        super.onResume();

        initMediaPlayer();
    }

    /**
     * 供Adapter调用获取当前音频播放进度
     *
     * @return
     */
    public double getPlayPercent() {
        double percent = 0.0;
        if (MediaPlayState.STARTED == mPlayState) {
            percent = (double) mMediaPlayer.getCurrentPosition() / mMediaPlayer.getDuration();
        }
        Log.d(TAG, "getPlayPercent percent = " + percent);
        return percent;
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mPlayState = MediaPlayState.IDLE;
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mPlayState = MediaPlayState.COMPLETED;

                    mContentList.get(mCurrentPlayingItemIndex).setPlayingAudio(false);
                    mContentListAdapter.notifyItemChanged(mCurrentPlayingItemIndex);
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e(TAG, "MediaPlayer onError");
                    mPlayState = MediaPlayState.ERROR;

                    mp.reset();
                    mPlayState = MediaPlayState.IDLE;

                    initMediaPlayer();
                    return true;
                }
            });
        }
        if (mCurrentPlayingItemIndex >= 0) {
            setMediaPlayerSource();
            prepareMediaPlayer();
        }
    }

    private void setMediaPlayerSource() {
        try {
            mMediaPlayer.setDataSource(mContentList.get(mCurrentPlayingItemIndex).getMediaFilePath());
            mPlayState = MediaPlayState.INITIALIZED;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareMediaPlayer() {
        try {
            mMediaPlayer.prepare();
            mPlayState = MediaPlayState.PREPARED;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playMedia() {
        mMediaPlayer.start();
        mPlayState = MediaPlayState.STARTED;

        mContentList.get(mCurrentPlayingItemIndex).setPlayingAudio(true);
        mContentListAdapter.notifyItemChanged(mCurrentPlayingItemIndex);
    }

    private void stopMedia() {
        mMediaPlayer.stop();
        mPlayState = MediaPlayState.STOPPED;

        mContentList.get(mCurrentPlayingItemIndex).setPlayingAudio(false);
        mContentListAdapter.notifyItemChanged(mCurrentPlayingItemIndex);
    }

    private void releaseMedia() {
        if (mMediaPlayer != null) {
            if (MediaPlayState.STARTED == mPlayState) {
                stopMedia();
            }
            mMediaPlayer.release();
            mPlayState = MediaPlayState.END;
            mMediaPlayer = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseMedia();
    }

    //endregion

    public void checkDeleteContent() {
        if (mSelectedContents != null && !mSelectedContents.isEmpty()) {
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setMessage("要删除此内容吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteContent();
                        }
                    })
                    .create();
            dialog.show();
        }
    }

    private void deleteContent() {
        if (mSelectedContents != null && !mSelectedContents.isEmpty()) {
            try {
                if (!mDBHelper.deleteContent(mSelectedContents)) {
                    TipsUtil.showToast(getContext(), "删除所选内容失败");
                    return;
                }

                mContentList.removeAll(mSelectedContents);
                for (Content content : mSelectedContents) {
                    if (!TextUtils.isEmpty(content.getMediaFilePath())) {
                        new File(content.getMediaFilePath()).delete();
                    }
                }

                exitEditMode(false);
            } catch (Exception e) {
                e.printStackTrace();
                TipsUtil.showToast(getContext(), "删除所选内容失败");
            }
        }
    }

    public interface OnFragmentInteractionListener {
        void gotoEditMode();

        void exitEditMode();
    }
}

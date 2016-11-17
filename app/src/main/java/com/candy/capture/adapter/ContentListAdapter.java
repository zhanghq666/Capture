package com.candy.capture.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TimeUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.candy.capture.R;
import com.candy.capture.activity.MainActivity;
import com.candy.capture.core.ConstantValues;
import com.candy.capture.customview.AudioView;
import com.candy.capture.model.Content;
import com.candy.capture.model.MediaPlayState;
import com.candy.capture.util.TimeUtil;

import java.util.ArrayList;

/**
 * Created by zhanghq on 2016/10/30.
 */

public class ContentListAdapter extends RecyclerView.Adapter<ContentListAdapter.BaseViewHolder> {
    private Context mContext;
    private ArrayList<Content> mContents;
    private ItemClickCallback mItemClickCallback;
    private boolean mIsEditMode;

    public ContentListAdapter(Context context, ItemClickCallback callback) {
        mContext = context;
        mItemClickCallback = callback;
    }

    public void setContents(ArrayList<Content> contents) {
        mContents = contents;
        notifyDataSetChanged();
    }

//    public void appendContentsEnd(ArrayList<Content> contents) {
//        if (mContents == null) {
//            mContents = contents;
//            notifyDataSetChanged();
//        } else {
//            int startPosition = mContents.size() - 1;
//            int count = contents.size();
//            mContents.addAll(contents);
//            notifyItemRangeInserted(startPosition, count);
//        }
//    }
//
//    public void appendContentsStart(ArrayList<Content> contents) {
//        if (mContents == null) {
//            mContents = contents;
//            notifyDataSetChanged();
//        } else {
//            int count = contents.size();
//            mContents.addAll(0, contents);
//            notifyItemRangeInserted(0, count);
//        }
//    }

    public void setEditMode(boolean isEditMode) {
        mIsEditMode = isEditMode;
    }

    @Override
    public int getItemViewType(int position) {
        return mContents.get(position).getType();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BaseViewHolder holder = null;
        switch (viewType) {
            case ConstantValues.CONTENT_TYPE_TEXT:
                holder = new TextViewHolder(View.inflate(mContext, R.layout.item_text, null));
                break;
            case ConstantValues.CONTENT_TYPE_AUDIO:
                holder = new AudioViewHolder(View.inflate(mContext, R.layout.item_audio, null));
                break;

        }
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, final int position) {
        Content content = mContents.get(position);
        holder.mRootView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mItemClickCallback != null) {
                    mItemClickCallback.onItemLongPressed(position);
                }
                return true;
            }
        });
        holder.mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemClickCallback != null) {
                    mItemClickCallback.onItemClick(position);
                }
            }
        });
        holder.mCheckBox.setVisibility(mIsEditMode ? View.VISIBLE : View.GONE);
        holder.mCheckBox.setChecked(content.isSelect());

        if (holder instanceof TextViewHolder) {
            TextViewHolder viewHolder = (TextViewHolder) holder;
            viewHolder.mDescTv.setText(content.getDesc());
        } else if (holder instanceof AudioViewHolder) {
            final AudioViewHolder viewHolder = (AudioViewHolder) holder;
            viewHolder.mDescTv.setText(content.getDesc());
            viewHolder.mDurationTv.setText(TimeUtil.formatDuration(content.getMediaDuration()));
            viewHolder.mAudioView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickCallback != null) {
                        mItemClickCallback.onAudioViewClick(position);
                    }
                }
            });
            viewHolder.mAudioView.setDuration(content.getMediaDuration());
            if (content.isPlayingAudio()) {
                if (mContext instanceof MainActivity) {
                    double percent = ((MainActivity) mContext).getPlayPercent();
                    if (percent < 0) {
                        viewHolder.mAudioView.stopAnimation();
                    } else {
                        viewHolder.mAudioView.startAnimation(percent);
                    }
                }
            } else {
                viewHolder.mAudioView.stopAnimation();
            }
        }
    }

    @Override
    public int getItemCount() {
        return mContents == null ? 0 : mContents.size();
    }

    class BaseViewHolder extends RecyclerView.ViewHolder {
        protected View mRootView;
        protected TextView mDescTv;
        protected CheckBox mCheckBox;

        public BaseViewHolder(View itemView) {
            super(itemView);

            mRootView = itemView.findViewById(R.id.rl_root);
            mDescTv = (TextView) itemView.findViewById(R.id.tv_desc);
            mCheckBox = (CheckBox) itemView.findViewById(R.id.cb_select);
        }
    }

    class TextViewHolder extends BaseViewHolder {

        public TextViewHolder(View itemView) {
            super(itemView);
        }
    }

    class AudioViewHolder extends BaseViewHolder {
        private AudioView mAudioView;
        private TextView mDurationTv;

        public AudioViewHolder(View itemView) {
            super(itemView);

            mAudioView = (AudioView) itemView.findViewById(R.id.audio_view);
            mDurationTv = (TextView) itemView.findViewById(R.id.tv_duration);
        }
    }

    public interface ItemClickCallback {
        void onItemClick(int position);

        void onItemLongPressed(int position);

        void onAudioViewClick(int position);
    }
}

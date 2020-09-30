package com.candy.capture.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.candy.capture.R
import com.candy.capture.core.ConstantValues
import com.candy.capture.customview.AudioView
import com.candy.capture.model.Content
import com.candy.capture.util.GlideImageLoader
import com.candy.capture.util.TimeUtil
import java.util.*

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:13
 */
class ContentListAdapter(val context: Context?, var contents: ArrayList<Content>?, private val audioPlayCallBack: AudioPlayCallBack?, private val callback: ItemClickCallback?): RecyclerView.Adapter<ContentListAdapter.BaseViewHolder>() {
    private var mIsEditMode = false

    fun setEditMode(isEditMode: Boolean) {
        mIsEditMode = isEditMode
    }

    override fun getItemViewType(position: Int): Int {
        return contents!![position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
           ConstantValues.CONTENT_TYPE_TEXT -> TextViewHolder(View.inflate(context, R.layout.item_text, null))
           ConstantValues.CONTENT_TYPE_AUDIO -> AudioViewHolder(View.inflate(context, R.layout.item_audio, null))
           ConstantValues.CONTENT_TYPE_PHOTO -> ImageViewHolder(View.inflate(context, R.layout.item_image, null))
           ConstantValues.CONTENT_TYPE_VIDEO -> VideoViewHolder(View.inflate(context, R.layout.item_video, null))
            else -> TextViewHolder(View.inflate(context, R.layout.item_text, null))
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val content = contents!![position]
        holder.itemView.setOnLongClickListener {
            if (callback != null) {
                callback!!.onItemLongPressed(position)
            }
            true
        }
        holder.itemView.setOnClickListener {
            if (callback != null) {
                callback!!.onItemClick(position)
            }
        }
        holder.mCheckBox.visibility = if (mIsEditMode) View.VISIBLE else View.GONE
        holder.mCheckBox.isChecked = content.isSelect
        holder.mDescTv.text = content.desc
        if (holder is AudioViewHolder) {
            holder.mDurationTv.text = TimeUtil.formatDuration(content.mediaDuration.toLong())
            holder.mAudioView.setOnClickListener {
                callback?.onAudioViewClick(position)
            }
            holder.mAudioView.setDuration(content.mediaDuration)
            if (content.isPlayingAudio) {
                if (audioPlayCallBack != null) {
                    val percent = audioPlayCallBack.getPlayPercent()
                    if (percent < 0) {
                        holder.mAudioView.stopAnimation()
                    } else {
                        holder.mAudioView.startAnimation(percent)
                    }
                }
            } else {
                holder.mAudioView.stopAnimation()
            }
        } else if (holder is ImageViewHolder) {
            holder.mImageView.setOnClickListener {
                if (callback != null) {
                    callback!!.onImageViewClick(position)
                }
            }
            GlideImageLoader.getInstance().loadImage(context, content.mediaFilePath, holder.mImageView)
        } else if (holder is VideoViewHolder) {
            GlideImageLoader.getInstance().loadVideoThumbnail(context, content.mediaFilePath, holder.mImageView)
            holder.mPlaybackMask.setOnClickListener {
                if (callback != null) {
                    callback!!.onVideoViewClick(position)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return contents?.size ?: 0
    }

    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mDescTv: TextView = itemView.findViewById<View>(R.id.tv_desc) as TextView
        var mCheckBox: CheckBox = itemView.findViewById<View>(R.id.cb_select) as CheckBox

    }

    internal class TextViewHolder(itemView: View) : BaseViewHolder(itemView)

    internal class AudioViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val mAudioView: AudioView = itemView.findViewById<View>(R.id.audio_view) as AudioView
        val mDurationTv: TextView = itemView.findViewById<View>(R.id.tv_duration) as TextView

    }

    internal class ImageViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val mImageView: ImageView = itemView.findViewById<View>(R.id.image_view) as ImageView

    }

    internal class VideoViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val mImageView: ImageView = itemView.findViewById<View>(R.id.image_view) as ImageView
        val mPlaybackMask: ImageView = itemView.findViewById<View>(R.id.iv_playback_mask) as ImageView

    }

    interface ItemClickCallback {
        fun onItemClick(position: Int)
        fun onItemLongPressed(position: Int)
        fun onAudioViewClick(position: Int)
        fun onImageViewClick(position: Int)
        fun onVideoViewClick(position: Int)
    }

    interface AudioPlayCallBack {
        fun getPlayPercent(): Double
    }
}
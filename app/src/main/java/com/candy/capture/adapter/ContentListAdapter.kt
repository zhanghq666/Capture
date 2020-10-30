package com.candy.capture.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.candy.capture.R
import com.candy.capture.core.ConstantValues
import com.candy.capture.customview.AudioView
import com.candy.capture.databinding.ItemAudioBinding
import com.candy.capture.databinding.ItemImageBinding
import com.candy.capture.databinding.ItemTextBinding
import com.candy.capture.databinding.ItemVideoBinding
import com.candy.capture.model.Content
import com.candy.commonlibrary.utils.GlideImageLoader
import java.util.*


/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:13
 */
class ContentListAdapter(val context: Context?,
                         private val audioPlayCallBack: AudioPlayCallBack?) : PagedListAdapter<Content, ContentListAdapter.BaseViewHolder>(DIFF_CALLBACK) {
    companion object {
        private val DIFF_CALLBACK = object :
                DiffUtil.ItemCallback<Content>() {
            // Content details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(oldConcert: Content,
                                         newConcert: Content) = oldConcert.id == newConcert.id

            override fun areContentsTheSame(oldConcert: Content,
                                            newConcert: Content) = oldConcert == newConcert
        }
    }

    private var mIsEditMode = false
    var callbackContent: ItemClickContentCallback? = null

    fun setEditMode(isEditMode: Boolean) {
        mIsEditMode = isEditMode
    }

    override fun getItemViewType(position: Int): Int {
        return currentList!![position]?.type!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ConstantValues.CONTENT_TYPE_TEXT -> TextViewHolder(ItemTextBinding.inflate(layoutInflater))
            ConstantValues.CONTENT_TYPE_AUDIO -> AudioViewHolder(ItemAudioBinding.inflate(layoutInflater))
            ConstantValues.CONTENT_TYPE_PHOTO -> ImageViewHolder(ItemImageBinding.inflate(layoutInflater))
            ConstantValues.CONTENT_TYPE_VIDEO -> VideoViewHolder(ItemVideoBinding.inflate(layoutInflater))
            else -> TextViewHolder(ItemTextBinding.inflate(layoutInflater))
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val content = currentList!![position]!!
        if (holder is AudioViewHolder) {
//            holder.mAudioView.setDuration(content.mediaDuration)
//            if (content.isPlayingAudio) {
//                if (audioPlayCallBack != null) {
//                    val percent = audioPlayCallBack.getPlayPercent()
//                    if (percent < 0) {
//                        holder.mAudioView.stopAnimation()
//                    } else {
//                        holder.mAudioView.startAnimation(percent)
//                    }
//                }
//            } else {
//                holder.mAudioView.stopAnimation()
//            }
        } else if (holder is ImageViewHolder) {
//            GlideImageLoader.getInstance().loadImage(context!!, content.mediaFilePath!!, holder.mImageView)
        } else if (holder is VideoViewHolder) {
            GlideImageLoader.getInstance().loadVideoThumbnail(context!!, content.mediaFilePath, holder.mImageView)
        }
        holder.bindTo(content)
    }

    open inner class BaseViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
//        var mDescTv: TextView = itemView.findViewById<View>(R.id.tv_desc) as TextView
//        var mCheckBox: CheckBox = itemView.findViewById<View>(R.id.cb_select) as CheckBox

        open fun bindTo(content: Content) {
            binding.setVariable(BR.content, content)
            binding.setVariable(BR.clickHandler, callbackContent)
            binding.setVariable(BR.editMode, mIsEditMode)
            binding.setVariable(BR.audioPlayer, audioPlayCallBack)
            binding.executePendingBindings()
        }
    }

    internal inner class TextViewHolder(private val binding: ViewDataBinding) : BaseViewHolder(binding) {
//        override fun bindTo(content: Content) {
//            super.bindTo(content)
//            binding.executePendingBindings()
//        }
    }

    internal inner class AudioViewHolder(private val binding: ViewDataBinding) : BaseViewHolder(binding) {
        val mAudioView: AudioView = itemView.findViewById<View>(R.id.audio_view) as AudioView
//        val mDurationTv: TextView = itemView.findViewById<View>(R.id.tv_duration) as TextView

//        override fun bindTo(content: Content) {
//            super.bindTo(content)
//            binding.setVariable()
//            binding.executePendingBindings()
//        }
    }

    internal inner class ImageViewHolder(private val binding: ViewDataBinding) : BaseViewHolder(binding) {
//        val mImageView: ImageView = itemView.findViewById<View>(R.id.image_view) as ImageView

    }

    internal inner class VideoViewHolder(private val binding: ViewDataBinding) : BaseViewHolder(binding) {
        val mImageView: ImageView = itemView.findViewById<View>(R.id.image_view) as ImageView
//        val mPlaybackMask: ImageView = itemView.findViewById<View>(R.id.iv_playback_mask) as ImageView

    }

    interface ItemClickContentCallback {
        fun onItemClick(content: Content)
        fun onItemLongPressed(content: Content): Boolean
        fun onAudioViewClick(content: Content)
        fun onImageViewClick(content: Content)
        fun onVideoViewClick(content: Content)
    }

    interface AudioPlayCallBack {
        fun getPlayPercent(): Double
    }
}
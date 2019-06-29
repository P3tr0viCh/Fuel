package ru.p3tr0vich.fuel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.databinding.FuelingListItemBinding
import ru.p3tr0vich.fuel.models.FuelingRecord
import java.util.*

internal class FuelingAdapter(private val onClickListener: View.OnClickListener,
                              showHeader: Boolean, showFooter: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val fuelingRecords: MutableList<FuelingRecord> = ArrayList()

    var showYear: Boolean = false

    @HeaderFooter
    private var mShowHeader = 0
    @HeaderFooter
    private var mShowFooter = 0

    var isShowHeader: Boolean
        get() = mShowHeader == HF_SHOW
        private set(showHeader) {
            mShowHeader = if (showHeader) HF_SHOW else HF_HIDE
        }

    private var isShowFooter: Boolean
        get() = mShowFooter == HF_SHOW
        set(showFooter) {
            mShowFooter = if (showFooter) HF_SHOW else HF_HIDE
        }

    @IntDef(HF_HIDE, HF_SHOW)
    annotation class HeaderFooter

    init {
        setHasStableIds(true)

        isShowHeader = showHeader
        isShowFooter = showFooter

        if (isShowHeader) {
            fuelingRecords.add(FuelingRecord())
        }
        if (isShowFooter) {
            fuelingRecords.add(FuelingRecord())
        }
    }

    fun swapRecords(records: List<FuelingRecord>?) {
        fuelingRecords.clear()

        if (isShowHeader) {
            fuelingRecords.add(FuelingRecord())
        }

        if (records != null) {
            fuelingRecords.addAll(records)
        }

        if (isShowFooter) {
            fuelingRecords.add(FuelingRecord())
        }

        notifyDataSetChanged()
    }

    fun findPositionById(id: Long): Int {
        var i = mShowHeader
        val count = fuelingRecords.size - mShowFooter
        while (i < count) {
            if (fuelingRecords[i].id == id) return i
            i++
        }

        return -1
    }

    override fun getItemCount(): Int {
        return fuelingRecords.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ITEM -> FuelingItemViewHolder(
                    FuelingListItemBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false).root)
            TYPE_HEADER -> HeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.partial_fueling_recycler_view_header, parent, false))
            TYPE_FOOTER -> HeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.partial_fueling_recycler_view_footer, parent, false))
            else -> throw RuntimeException("onCreateViewHolder: wrong viewType == $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FuelingItemViewHolder) {
            val fuelingRecord = fuelingRecords[position]

            holder.binding?.fuelingRecord = fuelingRecord

            holder.binding?.btnMenu?.tag = fuelingRecord.id
            holder.binding?.btnMenu?.setOnClickListener(onClickListener)

            holder.binding?.showYear = showYear
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (isShowHeader && position == HEADER_POSITION) return TYPE_HEADER
        return if (isShowFooter && position == fuelingRecords.size - 1) TYPE_FOOTER else TYPE_ITEM
    }

    override fun getItemId(position: Int): Long {
        if (isShowHeader && position == HEADER_POSITION) return HEADER_ID
        return if (isShowFooter && position == fuelingRecords.size - 1) FOOTER_ID else fuelingRecords[position].id

    }

    inner class FuelingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: FuelingListItemBinding? = DataBindingUtil.bind(itemView)
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        const val TYPE_FOOTER = 2

        private const val HEADER_ID = java.lang.Long.MAX_VALUE
        private const val FOOTER_ID = java.lang.Long.MIN_VALUE

        const val HEADER_POSITION = 0

        private const val HF_HIDE = 0
        private const val HF_SHOW = 1
    }
}
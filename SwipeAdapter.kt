package com.mentalstack.justdoit.screens.main.adapters

import android.content.Context
import android.os.Bundle
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.mentalstack.justdoit.model.HeaderTask
import com.mentalstack.justdoit.screens.main.TaskOperation
import kotlinx.android.synthetic.main.task_item_swipe_actions.view.*

class SwipeAdapter(ctx: Context, items: List<HeaderTask>, listener: TaskOperation) :
    HeaderAdapter(ctx, items, listener) {

    private val viewBinderHelper = ViewBinderHelper()

    override fun setData(newData: List<HeaderTask>) {
        super.setData(newData)
        this.items = newData
        notifyDataSetChanged()
    }

    init {
        viewBinderHelper.setOpenOnlyOne(true)
    }

    override fun onBindViewHolder(holder: ViewHolder?, section: Int, relativePosition: Int, absolutePosition: Int) {
        val itemId = items[section].taskList[relativePosition].id.toString()
        holder?.let {
            viewBinderHelper.bind(it.itemView.swipe_lt, itemId)
        }
        super.onBindViewHolder(holder, section, relativePosition, absolutePosition)
    }

    fun closeSwipeLayout(id: Long) {
        viewBinderHelper.closeLayout(id.toString())
    }

    fun saveStates(outState: Bundle) = viewBinderHelper.saveStates(outState)

    fun restoreStates(inState: Bundle) = viewBinderHelper.restoreStates(inState)

}

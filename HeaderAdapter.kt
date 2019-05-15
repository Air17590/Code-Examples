package com.mentalstack.justdoit.screens.main.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.mentalstack.justdoit.R
import com.mentalstack.justdoit.extensions.drw
import com.mentalstack.justdoit.extensions.setVisible
import com.mentalstack.justdoit.extensions.strTime
import com.mentalstack.justdoit.model.HeaderTask
import com.mentalstack.justdoit.model.SectionConverter
import com.mentalstack.justdoit.model.Task
import com.mentalstack.justdoit.model.TaskItem
import com.mentalstack.justdoit.screens.main.TaskOperation
import kotlinx.android.synthetic.main.task_item_header.view.*
import kotlinx.android.synthetic.main.task_item_swipe_actions.view.*

open class HeaderAdapter(ctx: Context, protected var items: List<HeaderTask>, val listener: TaskOperation) :
    SectionedRecyclerViewAdapter<HeaderAdapter.ViewHolder>() {

    private val priorityColors: IntArray = ctx.resources.getIntArray(R.array.priority_colors)
    private val openedHeaderIcon = ctx.drw(R.drawable.header_title_corners_expanded)
    private val closedHeaderIcon = ctx.drw(R.drawable.header_title_corners_collapsed)

    open fun setData(newData: List<HeaderTask>) {
        this.items = newData
        notifyDataSetChanged()
    }

    override fun getItemCount(section: Int) = items[section].taskList.size

    override fun getSectionCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder?, section: Int, relativePosition: Int, absolutePosition: Int) {
        val item = items[section].taskList[relativePosition]
        holder?.bind(item, section, absolutePosition)
    }

    override fun onBindHeaderViewHolder(holder: ViewHolder?, section: Int, expanded: Boolean) {
        holder?.bind(section)
    }

    override fun onBindFooterViewHolder(holder: ViewHolder?, section: Int) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = when (viewType) {
            SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER -> R.layout.task_item_header
            else -> R.layout.task_item_swipe_actions
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View) : SectionedViewHolder(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: Task, section: Int, absolutePosition: Int) {
            with(itemView) {
                taskTitle.text = item.title
                taskDescription.text = item.description
                val repeatDescription = item.repeat.getRepeatFullDescription(context)
                repeatDescriptionTv.text = repeatDescription
                repeatIconIv.setVisible(!repeatDescription.isEmpty())
                taskDescription.setVisible(!taskDescription.text.isEmpty())
                taskTime.text = item.date.strTime(context)
                val color = priorityColors[item.prior]
                val sectionIconRes = SectionConverter().toEnum(item.sectionId).getSectionSmallIconResId()
                itemView.sectionIconIv.setImageResource(sectionIconRes)
                itemView.sectionIconIv.setColorFilter(color)
            }
            with(itemView.taskLayout) {
                tag = item
                setOnClickListener { button ->
                    //TODO  If you want to add animation or something
                    val task = button.tag as? Task
                    if (task == null) {
                        return@setOnClickListener
                    } else {
                        listener.showTaskInfoDialog(task)
                    }
                }
            }

            itemView.taskCheckbox.isChecked = item.isDone
            with(itemView) {
                editBtn.tag = item
                editBtn.setOnClickListener {
                    val task = it.tag as? Task ?: return@setOnClickListener
                    listener.editTask(task)
                }
                val isLastInSection = items[section].taskList.size == 1
                deleteBtn.tag = TaskItem(item, isLastInSection, absolutePosition)
                deleteBtn.setOnClickListener {
                    //TODO  If you want to add animation or something
                    var deleteSection: (() -> Unit)? = null
                    val taskIt = it.tag as? TaskItem ?: return@setOnClickListener
                    if (taskIt.isLastInSection)
                        deleteSection = {
                            notifyItemRemoved(taskIt.absolutePosition)
                        }
                    listener.deleteTask(taskIt.task.id, deleteSection)
                }
            }
            itemView.taskMainFrame.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val heightCalc = itemView.taskMainFrame.measuredHeight
            itemView.frameSwipeButtons.layoutParams.height = heightCalc
        }

        fun bind(section: Int) {
            val icon = if (isSectionExpanded(section)) openedHeaderIcon else closedHeaderIcon
            itemView.tag = section
            itemView.tasksCount?.background = icon
            itemView.header.text = items[section].date
            itemView.tasksCount.text = getItemCount(section).toString()
        }

        override fun onClick(view: View) {
            val isHeader = isHeader
            val section = (view.tag as? Int) ?: 0
            if (!isHeader) return
            if (isSectionExpanded(section)) {
                collapseSection(section)
                view.tasksCount?.background = closedHeaderIcon
            } else {
                expandSection(section)
                view.tasksCount?.background = openedHeaderIcon
            }

        }
    }

}

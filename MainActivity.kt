package com.mentalstack.justdoit.screens.main

import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.mentalstack.justdoit.R
import com.mentalstack.justdoit.extensions.drw
import com.mentalstack.justdoit.extensions.setVisible
import com.mentalstack.justdoit.extensions.str
import com.mentalstack.justdoit.model.Filter
import com.mentalstack.justdoit.model.HeaderTask
import com.mentalstack.justdoit.model.Task
import com.mentalstack.justdoit.screens.base.BaseActivity
import com.mentalstack.justdoit.screens.createreminder.view.CreateEditTaskActivity
import com.mentalstack.justdoit.screens.filter.FilterActivity
import com.mentalstack.justdoit.screens.main.adapters.SwipeAdapter
import com.mentalstack.justdoit.screens.sectionchange.SectionChangeActivity
import com.mentalstack.justdoit.utils.tasksmanager.TasksManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

interface TaskOperation {
    fun showTaskInfoDialog(task: Task, execCancel: (() -> Unit)? = null)
    fun editTask(task: Task)
    fun deleteTask(taskId: Long, execYes: (() -> Unit)? = null, execNo: (() -> Unit)? = null)
}

class MainActivity : BaseActivity(), TaskOperation {
    override val layoutID = R.layout.activity_main

    private val swipeAdapter by lazy {
        SwipeAdapter(this, ArrayList(), this).apply {
            expandAllSections()
            shouldShowFooters(false)
            shouldShowHeadersForEmptySections(true)
        }
    }

    private val updateFunc: (List<HeaderTask>) -> Unit = { updatedList ->
        checkScreenMode(updatedList)
        swipeAdapter.setData(updatedList)
    }

    private val viewModel: MainViewModel
        get() = ViewModelProviders.of(this).get(MainViewModel::class.java)

    private var filterMenu: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.drawable.empty_screen)
        initToolbar()
        initRecycleView()
        newTaskBtn.setOnClickListener {
            if (isClickIgnore())
                return@setOnClickListener
            launchCreateEditTaskActivity()
        }
        val taskId = intent?.getLongExtra(TasksManager.TASK_ID, -1L) ?: -1L
        if (taskId >= 0) {
            doAsync {
                viewModel.getTaskById(taskId)?.let { task ->
                    uiThread {
                        showTaskInfoDialog(task, null)
                    }
                }
            }
        } else {
            TasksManager.instance.restartAllTasks()
        }
        viewModel.startUpdateObserve(this) { updateFunc(it) }
    }

    private fun initToolbar() {
        setSupportActionBar(toolBar)
        toolBar.overflowIcon = drw(R.drawable.ic_menu)
        supportActionBar?.title = ""
    }

    private fun initRecycleView() = with(tasksList) {
        layoutManager = LinearLayoutManager(this@MainActivity)
        adapter = swipeAdapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        swipeAdapter.saveStates(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        swipeAdapter.restoreStates(savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter -> {
                if (isClickIgnore())
                    return true

                viewModel.getMinMaxDateBorders {
                    val launch = Intent(this, FilterActivity::class.java)
                    launch.putExtra(FILTER, viewModel.getFilter())
                    startActivityForResult(launch, FILTER_ACTIVITY_CODE)
                }
                return true
            }
            R.id.action_change_tags -> {
                val intent = Intent(this, SectionChangeActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILTER_ACTIVITY_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                (data?.getSerializableExtra(FILTER) as? Filter)?.let { viewModel.setFilter(it) }
                changeFilterIcon()
                viewModel.startUpdateObserve(this@MainActivity, updateFunc)
            }
        }
    }

    private fun launchCreateEditTaskActivity(task: Task? = null) {
        val intent = CreateEditTaskActivity.newInstance(this, task?.id)
        startActivity(intent)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        filterMenu = menu.findItem(R.id.action_filter)
        changeFilterIcon()
        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
        return true
    }

    private fun changeFilterIcon() {
        filterMenu?.setIcon(if (viewModel.isActiveFilter()) R.drawable.ic_filter_active else R.drawable.ic_filter)
        viewModel.isNotEmptyDB { filterMenu?.isVisible = it }
    }

    override fun deleteTask(taskId: Long, execYes: (() -> Unit)?, execNo: (() -> Unit)?) {
        if (isClickIgnore()) return
        dialogYesNo(str(R.string.are_you_sure_want_to_delete), {
            swipeAdapter.closeSwipeLayout(taskId)
            viewModel.deleteTask(taskId)
            changeFilterIcon()
            execYes?.invoke()
        }, { execNo?.invoke() })
    }

    override fun showTaskInfoDialog(task: Task, execCancel: (() -> Unit)?) {
        if (isClickIgnore()) return
        dialogInfo(task, { viewModel.updateTaskDoneState(task) },
            { execCancel?.invoke() }
        )
    }

    override fun editTask(task: Task) {
        if (isClickIgnore()) return
        swipeAdapter.closeSwipeLayout(task.id)
        launchCreateEditTaskActivity(task)
    }

    private fun checkScreenMode(updatedList: List<HeaderTask>) {
        changeFilterIcon()
        val isNoData = updatedList.isEmpty()
        tasksList.setVisible(!isNoData)
        emptyPlaceholder.setVisible(isNoData)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAndRemoveTask()
        System.exit(0)
    }

    companion object {
        private const val FILTER_ACTIVITY_CODE = 20
        const val FILTER = "FILTER"
    }
}

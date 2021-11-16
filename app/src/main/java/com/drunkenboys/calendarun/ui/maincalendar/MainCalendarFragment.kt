package com.drunkenboys.calendarun.ui.maincalendar

import android.os.Bundle
import android.view.View
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.navigation.ui.setupWithNavController
import com.drunkenboys.calendarun.R
import com.drunkenboys.calendarun.data.calendar.entity.Calendar
import com.drunkenboys.calendarun.databinding.DrawerHeaderBinding
import com.drunkenboys.calendarun.databinding.FragmentMainCalendarBinding
import com.drunkenboys.calendarun.ui.base.BaseFragment
import com.drunkenboys.calendarun.util.extensions.sharedCollect
import com.drunkenboys.calendarun.util.extensions.stateCollect
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainCalendarFragment : BaseFragment<FragmentMainCalendarBinding>(R.layout.fragment_main_calendar) {

    private val mainCalendarViewModel by viewModels<MainCalendarViewModel>()
    private var isMonthCalendar = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainCalendarViewModel.fetchCalendarList()
        setupCalendarView()
        setupDataBinding()
        setupToolbar()
        setupMonthCalendar()
        collectFabClickEvent()
        collectCalendarList()
        collectCheckPointList()
        collectScheduleList()
    }

    private fun setupCalendarView() {
        // TODO: CalendarView 내부로 전환
        binding.calendarMonth.isVisible = isMonthCalendar
        binding.calendarYear.isVisible = !isMonthCalendar
    }

    private fun setupDataBinding() {
        binding.mainCalendarViewModel = mainCalendarViewModel
    }

    private fun setupToolbar() {
        binding.toolbarMainCalendar.setupWithNavController(navController, binding.layoutDrawer)
        setupOnMenuItemClickListener()
        setupAddDrawerListener()
    }

    private fun setupAddDrawerListener() = with(binding) {
        val drawerHeaderBinding = DrawerHeaderBinding.bind(navView.getHeaderView(FIRST_HEADER))
        layoutDrawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val menuItemOrder = this@MainCalendarFragment.mainCalendarViewModel.menuItemOrder.value
                if (binding.navView.menu.isEmpty()) return
                binding.navView.menu[menuItemOrder].isChecked = true
                drawerHeaderBinding.tvDrawerHeaderTitle.text = binding.navView.menu[menuItemOrder].title
            }

            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    private fun setupOnMenuItemClickListener() {
        binding.toolbarMainCalendar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_main_calendar_calendar -> {
                    isMonthCalendar = !isMonthCalendar
                    setupCalendarView()
                }
                R.id.menu_main_calendar_search -> navigateToSearchSchedule()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
    }

    private fun navigateToSearchSchedule() {
        val action = MainCalendarFragmentDirections.toSearchSchedule()
        navController.navigate(action)
    }

    private fun setupMonthCalendar() {
        binding.calendarMonth.setOnDaySecondClickListener { date, _ ->
            val calendarId = mainCalendarViewModel.calendar.value?.id ?: throw IllegalStateException("calendarId is null")
            val action = MainCalendarFragmentDirections.toDayScheduleDialog(calendarId, date.toString())
            navController.navigate(action)
        }
    }

    private fun collectFabClickEvent() {
        sharedCollect(mainCalendarViewModel.fabClickEvent) { calendarId ->
            val action = MainCalendarFragmentDirections.toSaveSchedule(calendarId, 0)
            navController.navigate(action)
        }
    }

    private fun collectCalendarList() {
        stateCollect(mainCalendarViewModel.calendarList) { calendarList ->
            setupNavigationView(calendarList)

            val menuItemOrder = mainCalendarViewModel.menuItemOrder.value
            if (calendarList.isEmpty()) return@stateCollect
            val calendar = calendarList[menuItemOrder]
            mainCalendarViewModel.setCalendar(calendar)
        }
    }

    private fun setupNavigationView(calendarList: List<Calendar>) {
        val menu = binding.navView.menu
        menu.clear()

        calendarList.forEachIndexed { index, calendar ->
            menu.add(calendar.name)
                .setIcon(R.drawable.ic_favorite_24)
                .setCheckable(true)
                .setOnMenuItemClickListener {
                    mainCalendarViewModel.setMenuItemOrder(index)
                    // TODO: 2021-11-11 item에 맞는 캘린더 뷰로 변경
                    mainCalendarViewModel.setCalendar(calendar)
                    binding.layoutDrawer.closeDrawer(GravityCompat.START)
                    true
                }
        }

        menu.add(getString(R.string.drawer_calendar_add))
            .setIcon(R.drawable.ic_add)
            .setOnMenuItemClickListener {
                navigateToSaveCalendar()
                true
            }

        val manageMenu = menu.addSubMenu(getString(R.string.drawer_calendar_manage))
        manageMenu.add(getString(R.string.drawer_calendar_manage))
            .setIcon(R.drawable.ic_calendar_today)
            .setOnMenuItemClickListener {
                navigateToManageCalendar()
                true
            }

        manageMenu.add(getString(R.string.drawer_theme_setting))
            .setIcon(R.drawable.ic_palette)
            .setOnMenuItemClickListener {
                // TODO: 2021-11-11 테마 설정 화면으로 이동동
                true
            }
    }

    private fun navigateToSaveCalendar() {
        val action = MainCalendarFragmentDirections.toSaveCalendar()
        navController.navigate(action)
        binding.layoutDrawer.closeDrawer(GravityCompat.START)
    }

    private fun navigateToManageCalendar() {
        val action = MainCalendarFragmentDirections.toManageCalendar()
        navController.navigate(action)
        binding.layoutDrawer.closeDrawer(GravityCompat.START)
    }

    private fun collectCheckPointList() {
        stateCollect(mainCalendarViewModel.checkPointList) { checkPointList ->
            // TODO: 2021-11-10 CheckPoint 날짜 읽어서 뷰 나누기
        }
    }

    private fun collectScheduleList() {
        stateCollect(mainCalendarViewModel.scheduleList) { scheduleList ->
            binding.calendarMonth.setSchedules(scheduleList)
            binding.calendarYear.setSchedules(scheduleList)
        }
    }

    companion object {
        private const val FIRST_HEADER = 0
    }
}

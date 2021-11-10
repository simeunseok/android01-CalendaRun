package com.drunkenboys.ckscalendar.yearcalendar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import com.drunkenboys.ckscalendar.R
import com.drunkenboys.ckscalendar.databinding.LayoutYearCalendarBinding
import com.drunkenboys.ckscalendar.FakeFactory
import com.drunkenboys.ckscalendar.data.*
import com.drunkenboys.ckscalendar.listener.OnDayClickListener
import com.drunkenboys.ckscalendar.listener.OnDaySecondClickListener
import com.drunkenboys.ckscalendar.utils.TimeUtils.dayValue
import java.time.DayOfWeek
import java.time.LocalDate

@ExperimentalAnimationApi
@ExperimentalFoundationApi
class YearCalendarView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: LayoutYearCalendarBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.layout_year_calendar,
        this,
        true
    )

    private val controller = YearCalendarController()

    private val header by lazy { YearCalendarHeader(design) }

    private var design = FakeFactory.createFakeDesign()

    private var onDateClickListener: OnDayClickListener? = null
    private var onDateSecondClickListener: OnDaySecondClickListener? = null

    private var schedules = mutableListOf<CalendarScheduleObject>()

    init {
        val yearList =  mutableListOf<List<CalendarSet>>()

        (INIT_YEAR..LAST_YEAR).forEach { year ->
            yearList.add(FakeFactory.createFakeCalendarSetList(year))
        }

        binding.composeYearCalendarViewDayOfWeek.setContent {
            header.WeekHeader()
        }

        binding.composeYearCalendarViewYearCalendar.setContent {
            CalendarLazyColumn(yearList)
        }
    }

    @ExperimentalFoundationApi
    @Composable
    private fun CalendarLazyColumn(yearList: List<List<CalendarSet>>) {
        // RecyclerView의 상태를 관찰
        val listState = rememberLazyListState()

        val today = CalendarDate(LocalDate.now(), DayType.PADDING, true) // 초기화를 위한 dummy
        var clickedDay by remember { mutableStateOf<CalendarDate?>(today) }

        // RecyclerView와 유사
        LazyColumn(state = listState) {
            yearList.forEach { year ->
                // 항상 떠있는 헤더
                stickyHeader {
                    header.YearHeader(year)
                }

                items(year) { month ->
                    controller.calendarSetToCalendarDates(month).forEach { week ->
                        val weekIds = week.map { day -> day.date.toString() }

                        ConstraintLayout(controller.dayOfWeekConstraints(weekIds), Modifier.fillMaxWidth()) {
                            if (controller.isFirstWeek(week, month.id)) Text(text = "${month.id}월")
                            val thisWeekScheduleList = Array(7) { Array<CalendarScheduleObject?>(3) { null } }
                            week.forEach { day ->

                                // TODO: 디자인 설정
                                if (day.dayType == DayType.PADDING) {
                                    PaddingText(day = day)
                                } else {
                                    Column(modifier = Modifier
                                        .layoutId(day.date.toString())
                                        .border(
                                            BorderStroke(
                                                width = 2.dp,
                                                color = if (clickedDay?.date == day.date) Color(design.selectedFrameColor)
                                                else Color.Transparent))
                                        .clickable(onClick = {
                                            clickedDay = day
                                            if (clickedDay?.date != day.date) onDateClickListener
                                            else onDateSecondClickListener
                                        }),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        DayText(day = day)
                                        ScheduleText(day = day, schedules, thisWeekScheduleList)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 뷰가 호출되면 오늘 날짜가 보이게 스크롤
        LaunchedEffect(listState) {
            listState.scrollToItem(index = LAST_YEAR - 1) // preload
            listState.scrollToItem(index = getTodayItemIndex())
        }
    }

    @Composable
    private fun DayText(day: CalendarDate) {
        val color = when (day.dayType) { // FIXME: month 와 통합
            DayType.HOLIDAY -> Color(design.holidayTextColor)
            DayType.SATURDAY -> Color(design.saturdayTextColor)
            DayType.SUNDAY -> Color(design.sundayTextColor)
            else -> Color(design.weekDayTextColor)
        }

        // FIXME: mapper 추가
        val align = when (design.textAlign) {
            -1 -> TextAlign.Start
            0 -> TextAlign.Center
            1 -> TextAlign.End
            else -> TextAlign.Center
        }

        Text(
            text = "${day.date.dayOfMonth}",
            color = color,
            modifier = Modifier.layoutId(day.date.toString()),
            textAlign = align,
            fontSize = design.textSize.dp()
        )
    }

    @Composable
    private fun Int.dp() = with(LocalDensity.current) {  Dp(this@dp.toFloat()).toSp()  }

    @Composable
    private fun PaddingText(day: CalendarDate) {
        Text(
            text = "${day.date.dayOfMonth}",
            modifier = Modifier
                .layoutId(day.date.toString())
                .alpha(0f),
            textAlign = TextAlign.Center,
            fontSize = design.textSize.dp()
        )
    }

    @Composable
    private fun ScheduleText(
        day: CalendarDate,
        scheduleList: List<CalendarScheduleObject>,
        weekScheduleList: Array<Array<CalendarScheduleObject?>>
    ) {
        val today = day.date
        val weekNum = (today.dayOfWeek.dayValue())

        with(controller) { setWeekSchedule(getStartScheduleList(today, scheduleList), weekScheduleList, today) }

        weekScheduleList[weekNum].forEach { schedule ->
            if (schedule == null) {
                // padding
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = " ",
                        modifier = Modifier.padding(2.dp),
                        fontSize = design.textSize.dp()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
            } else {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(schedule.color))) {
                    Text(
                        text = if (schedule.startDate == day.date || day.date.dayOfWeek == DayOfWeek.SUNDAY) schedule.text else "",
                        modifier = Modifier.padding(2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = design.textSize.dp()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }

    private fun getTodayItemIndex(): Int {
        val today = LocalDate.now()

        // 월 달력 12개 + 년 헤더 1개
        return (today.year - INIT_YEAR) * 13 + today.monthValue - 1
    }

    fun setOnDateClickListener(onDateClickListener: OnDayClickListener) {
        this.onDateClickListener = onDateClickListener
    }

    fun setOnDaySecondClickListener(onDateSecondClickListener: OnDaySecondClickListener) {
        this.onDateSecondClickListener = onDateSecondClickListener
    }

    fun setSchedule(schedule: CalendarScheduleObject) {
        schedules.add(schedule)
    }

    fun setSchedules(schedules: List<CalendarScheduleObject>) {
        this.schedules.addAll(schedules)
    }

    fun setTheme(designObject: CalendarDesignObject) {
        design = designObject
    }

    fun resetTheme() {
        // FIXME
        design = FakeFactory.createFakeDesign()
    }

    companion object {
        const val TAG = "YEAR_CALENDAR"
        const val INIT_YEAR = 0
        const val LAST_YEAR = 10000
    }
}
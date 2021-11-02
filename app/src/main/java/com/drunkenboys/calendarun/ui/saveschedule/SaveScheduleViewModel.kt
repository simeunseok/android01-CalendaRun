package com.drunkenboys.calendarun.ui.saveschedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drunkenboys.calendarun.data.schedule.entity.Schedule
import com.drunkenboys.calendarun.data.schedule.local.ScheduleLocalDataSource
import com.drunkenboys.calendarun.ui.saveschedule.model.BehaviorType
import com.drunkenboys.calendarun.ui.saveschedule.model.ScheduleNotificationType
import com.drunkenboys.calendarun.util.getOrThrow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SaveScheduleViewModel @Inject constructor(private val scheduleDataSource: ScheduleLocalDataSource) : ViewModel() {

    private var scheduleId: Int? = null

    private var calendarId: Int? = null

    private lateinit var behaviorType: BehaviorType

    val title = MutableLiveData("")

    private val _startDate = MutableLiveData(Date())
    val startDate: LiveData<Date> = _startDate

    private val _endDate = MutableLiveData(Date())
    val endDate: LiveData<Date> = _endDate

    val memo = MutableLiveData("")

    private val _calendarName = MutableLiveData("test")
    val calendarName: LiveData<String> = _calendarName

    private val _notification = MutableLiveData(ScheduleNotificationType.TEN_MINUTES_AGO)
    val notification: MutableLiveData<ScheduleNotificationType> = _notification

    // TODO: 2021-11-03 컬러 리소스를 뭐로 표현할 지 정해야 함.
    private val _tagColor = MutableLiveData(0)
    val tagColor: LiveData<Int> = _tagColor

    private val _saveScheduleEvent = MutableLiveData<Unit>()
    val saveScheduleEvent: LiveData<Unit> = _saveScheduleEvent

    fun init(scheduleId: Int = 0, calendarId: Int, calendarName: String, behaviorType: BehaviorType) {
        this.scheduleId = scheduleId
        this.calendarId = calendarId
        _calendarName.value = calendarName
        this.behaviorType = behaviorType
    }

    fun saveSchedule() {
        if (isInvalidInput()) return

        viewModelScope.launch {
            val schedule = createScheduleInstance()

            when (behaviorType) {
                BehaviorType.INSERT -> scheduleDataSource.insertSchedule(schedule)
                BehaviorType.UPDATE -> scheduleDataSource.updateSchedule(schedule)
            }
            _saveScheduleEvent.value = Unit
        }
    }

    private fun isInvalidInput(): Boolean {
        scheduleId ?: return true
        calendarId ?: return true
        if (!this::behaviorType.isInitialized) return true
        if (title.value.isNullOrEmpty()) return true
        startDate.value ?: return true
        endDate.value ?: return true
        if (calendarName.value.isNullOrEmpty()) return true

        return false
    }

    private fun createScheduleInstance() = Schedule(
        id = scheduleId ?: throw IllegalArgumentException(),
        calendarId = calendarId ?: throw IllegalArgumentException(),
        name = title.getOrThrow(),
        startDate = startDate.getOrThrow(),
        endDate = endDate.getOrThrow(),
        notification = getNotificationDate(),
        memo = memo.getOrThrow(),
        color = tagColor.getOrThrow()
    )

    private fun getNotificationDate(): Date? {
        val calendar = Calendar.getInstance()
        calendar.time = startDate.getOrThrow()

        when (notification.value) {
            ScheduleNotificationType.NONE -> return null
            ScheduleNotificationType.TEN_MINUTES_AGO -> calendar.add(Calendar.MINUTE, -10)
            ScheduleNotificationType.A_HOUR_AGO -> calendar.add(Calendar.HOUR_OF_DAY, -1)
            ScheduleNotificationType.A_DAY_AGO -> calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return calendar.time
    }
}

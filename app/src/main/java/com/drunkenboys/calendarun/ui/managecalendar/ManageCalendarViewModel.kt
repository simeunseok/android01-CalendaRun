package com.drunkenboys.calendarun.ui.managecalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drunkenboys.calendarun.data.calendar.local.CalendarLocalDataSource
import com.drunkenboys.calendarun.ui.managecalendar.model.CalendarItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageCalendarViewModel @Inject constructor(
    private val calendarLocalDataSource: CalendarLocalDataSource
) : ViewModel() {

    private val _calendarItemList = MutableStateFlow<List<CalendarItem>>(emptyList())
    val calendarItemList: StateFlow<List<CalendarItem>> = _calendarItemList

    private val _deleteCalendarEvent = MutableSharedFlow<Unit>()
    val deleteCalendarEvent: SharedFlow<Unit> = _deleteCalendarEvent

    fun fetchCalendarList() {
        viewModelScope.launch {
            val calendarList = calendarLocalDataSource.fetchAllCalendar()
            val newCalendarItemList = mutableListOf<CalendarItem>()
            calendarList.forEach { calendar ->
                newCalendarItemList.add(
                    CalendarItem(
                        id = calendar.id,
                        name = calendar.name,
                        startDate = calendar.startDate,
                        endDate = calendar.endDate
                    )
                )
            }
            _calendarItemList.emit(newCalendarItemList)
        }
    }

    fun deleteCalendarItem(currentCalendarItemList: List<CalendarItem>) {
        viewModelScope.launch {
            currentCalendarItemList
                .filter { calendarItem -> calendarItem.check }
                .forEach { calendarItem ->
                    calendarLocalDataSource.deleteCalendar(
                        calendarItem.toCalendar()
                    )
                }
            fetchCalendarList()
        }
    }

    fun emitDeleteCalendarEvent() {
        viewModelScope.launch {
            _deleteCalendarEvent.emit(Unit)
        }
    }
}

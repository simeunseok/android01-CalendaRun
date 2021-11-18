package com.drunkenboys.calendarun.ui.saveschedule

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.drunkenboys.calendarun.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteScheduleDialog : DialogFragment() {

    private val saveScheduleViewModel: SaveScheduleViewModel
            by navGraphViewModels(R.id.saveScheduleFragment) { defaultViewModelProviderFactory }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog = AlertDialog.Builder(requireContext())
        .setTitle(R.string.deleteScheduleDialog_title)
        .setMessage(R.string.deleteScheduleDialog_message)
        .setPositiveButton(R.string.delete) { _, _ ->
            saveScheduleViewModel.deleteSchedule()
            findNavController().navigateUp()
        }
        .setNegativeButton(R.string.cancel) { _, _ -> }
        .create()
}

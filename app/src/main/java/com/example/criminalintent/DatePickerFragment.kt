package com.example.criminalintent

import android.animation.TimeAnimator
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_DATE = "date"
private const val ARG_DIALOG_TYPE = "dialogType"

class DatePickerFragment: DialogFragment() {
    interface Callbacks {
        fun onDateSelected(date: Date)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val date = arguments?.getSerializable(ARG_DATE) as Date
        val calendar = Calendar.getInstance()
        calendar.time = date

        val initialYear = calendar.get(Calendar.YEAR)
        val initialMonth = calendar.get(Calendar.MONTH)
        val initialDay = calendar.get(Calendar.DAY_OF_MONTH)
        val initialHour = calendar.get(Calendar.HOUR)
        val initialMinutes  = calendar.get(Calendar.MINUTE)

        val isTimePicker = arguments?.getBoolean(ARG_DIALOG_TYPE) ?: false
        if (isTimePicker) {
            val timeListener = TimePickerDialog.OnTimeSetListener() {
                    _, hour, minute ->
                calendar.set(Calendar.HOUR, hour)
                calendar.set(Calendar.MINUTE, minute)
                val resultDate = calendar.time
                targetFragment?.let {
                        fragment -> (fragment as Callbacks).onDateSelected(resultDate)
                }
            }

            return TimePickerDialog(
                requireContext(),
                timeListener,
                initialHour,
                initialMinutes,
                true
            )
        } else {
            val dateListener = DatePickerDialog.OnDateSetListener {
                    _, year, month, day ->
                val resultDate = GregorianCalendar(year, month, day).time
                targetFragment?.let {
                        fragment -> (fragment as Callbacks).onDateSelected(resultDate)
                }
            }

            return DatePickerDialog(
                requireContext(),
                dateListener,
                initialYear,
                initialMonth,
                initialDay
            )

        }

    }

    companion object {
        fun newInstance(date: Date, isTimePicker: Boolean): DatePickerFragment {
            val args = Bundle().apply {
                putBoolean(ARG_DIALOG_TYPE, isTimePicker)
                putSerializable(ARG_DATE, date)
            }
            return DatePickerFragment().apply {
                arguments = args
            }
        }
    }
}
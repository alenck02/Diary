package com.example.diary

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.diary.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class HomeFragment : Fragment() {

    private var homeBinding: FragmentHomeBinding? = null
    private val binding get() = homeBinding!!

    private lateinit var selectedDate: LocalDate
    private var currentStartOfWeek: LocalDate = LocalDate.now()

    private var selectedDateTextView: TextView? = null

    private lateinit var diaryDao: DiaryDao
    private lateinit var diaryEntry: DiaryEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeBinding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val db = AppDatabase.getDatabase(requireContext())
        diaryDao = db.diaryDao()

        selectedDate = currentStartOfWeek
        setWeek(selectedDate)

        binding.preBtn.setOnClickListener {
            selectedDate = selectedDate.minusWeeks(1)
            setWeek(selectedDate)
        }

        binding.nextBtn.setOnClickListener {
            selectedDate = selectedDate.plusWeeks(1)
            setWeek(selectedDate)
        }

        binding.saveBtn.setOnClickListener {
            if (binding.content.text.isNotEmpty()) {
                saveDiaryEntry(selectedDate, binding.content.text.toString())
                updateDiaryEntry(selectedDate, binding.content.text.toString())
                Toast.makeText(context, "Save Diary", Toast.LENGTH_SHORT).show()
                loadDiaryEntry(selectedDate)
            } else {
                Toast.makeText(context, "Empty Diary", Toast.LENGTH_SHORT).show()
            }
        }

        binding.deleteBtn.setOnClickListener {
            if (binding.content.text.isNotEmpty()) {
                deleteDiaryEntry()
                Toast.makeText(context, "Delete Diary", Toast.LENGTH_SHORT).show()
                loadDiaryEntry(selectedDate)
            } else {
                Toast.makeText(context, "Empty Diary", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    private fun setWeek(startOfWeek: LocalDate) {
        loadDiaryEntry(startOfWeek)

        val nearestSunday = startOfWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

        val today = LocalDate.now()
        val yearMonth = YearMonth.from(today)

        if (startOfWeek <= today && today <= startOfWeek.plusDays(6)) {
            binding.yearMonth.text = "${yearMonth.year}년 ${yearMonth.monthValue}월"
        } else {
            val startYearMonth = YearMonth.from(nearestSunday)
            binding.yearMonth.text = "${startYearMonth.year}년 ${startYearMonth.monthValue}월"
        }

        for (i in 1..7) {
            val currentDateForDay = nearestSunday.plusDays(i.toLong() - 1)
            val dateTextView = when (i) {
                1 -> binding.date1
                2 -> binding.date2
                3 -> binding.date3
                4 -> binding.date4
                5 -> binding.date5
                6 -> binding.date6
                7 -> binding.date7
                else -> null
            }

            val dayTextView = when (i) {
                1 -> binding.day1
                2 -> binding.day2
                3 -> binding.day3
                4 -> binding.day4
                5 -> binding.day5
                6 -> binding.day6
                7 -> binding.day7
                else -> null
            }

            dateTextView?.text = formatDate(currentDateForDay)

            val startOfWeekToday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            val endOfWeek = startOfWeekToday.plusDays(6)
            val isTodayInWeek = today in startOfWeekToday..endOfWeek

            if (isTodayInWeek) {
                if (today == currentDateForDay) {
                    selectedDateTextView?.let {
                        selectedDateTextView?.let { previousTextView ->
                            binding.clickDate.visibility = View.GONE
                        }
                    }

                    loadDiaryEntry(today)
                    selectedDateTextView = dateTextView
                    binding.todayCircle.visibility = View.VISIBLE
                    binding.clickDate.visibility = View.VISIBLE

                    dateTextView?.viewTreeObserver?.addOnPreDrawListener(object :
                    ViewTreeObserver.OnPreDrawListener{
                        override fun onPreDraw() : Boolean {
                            dateTextView.viewTreeObserver.removeOnPreDrawListener(this)
                            val dateTextViewX = dateTextView.x
                            val dateTextViewWidth = dateTextView.width.toFloat()
                            val circleWidth = binding.todayCircle.width.toFloat()
                            val clickDateWidth = binding.clickDate.width.toFloat()

                            binding.todayCircle.x =
                                dateTextViewX + (dateTextViewWidth - circleWidth) / 2

                            binding.clickDate.x =
                                dateTextViewX + (dateTextViewWidth - clickDateWidth) / 2
                            return true
                        }
                    })

                    dateTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    dayTextView?.setTextColor(Color.parseColor("#1D1D1D"))
                }
            } else {
                dateTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
                dayTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
                binding.todayCircle.visibility = View.GONE
            }

            if (dateTextView?.id == binding.date6.id) {
                dayTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
            }

            if (dateTextView?.id == binding.date7.id) {
                dayTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            }

            dateTextView?.setOnClickListener {
                val currentYear = selectedDate.year
                val currentMonth = selectedDate.monthValue
                val day = dateTextView?.text.toString()

                val sundayYear = nearestSunday.year
                val sundayMonth = nearestSunday.monthValue
                val sundayDate = formatDate(nearestSunday).toInt()

                val formattedSundayYear = String.format("%02d", sundayYear.toInt())
                val formattedSundayMonth = String.format("%02d", sundayMonth.toInt())

                val formattedDay = String.format("%02d", day.toInt())
                val formattedMonth = String.format("%02d", currentMonth.toInt())
                val formattedYear = String.format("%02d", currentYear.toInt())

                val ComYear = "$formattedSundayYear".toInt()
                val selectedYear = "$formattedYear".toInt()

                val ComMonth = "$formattedSundayMonth".toInt()
                val selectedMonth = "$formattedMonth".toInt()

                val ComDate = "$sundayDate".toInt()
                val selectedDay = "$formattedDay".toInt()

                if (ComMonth == selectedMonth && ComDate > selectedDay) {
                    val plusMonth = selectedDate.plusMonths(1)

                    val Month = plusMonth.monthValue
                    val formattedMonth1 = String.format("%02d", Month)

                    val selectedDateString1 = "${selectedDate.year}-$formattedMonth1-$formattedDay"

                    selectedDate = LocalDate.parse(selectedDateString1, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))

                    loadDiaryEntry(selectedDate)
                } else if (ComMonth < selectedMonth && ComDate <= selectedDay || ComMonth > selectedMonth && ComDate <= selectedDay) {
                    val minusMonth = selectedDate.minusMonths(1)

                    val month = minusMonth.monthValue
                    val formattedMonth2 = String.format("%02d", month)

                    val selectedDateString2 = "${selectedDate.year}-$formattedMonth2-$formattedDay"

                    selectedDate = LocalDate.parse(selectedDateString2, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))

                    loadDiaryEntry(selectedDate)
                } else {
                    val selectedDateString = "${selectedDate.year}-$formattedMonth-$formattedDay"

                    selectedDate = LocalDate.parse(selectedDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))

                    loadDiaryEntry(selectedDate)
                }

                // 날짜 클릭 시 년도가 바로 바뀌는게 아니고 한 번 더 클릭해야 바뀌는 버그
                if (selectedMonth == 12 && selectedYear > ComYear) {
                    val minusYear = selectedDate.minusYears(1)

                    val year = minusYear.year
                    val selectedDateString = "$year-$formattedMonth-$formattedDay"

                    selectedDate = LocalDate.parse(selectedDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))
                }

                if (selectedMonth == 1 && selectedYear == ComYear) {
                    val plusYear = selectedDate.plusYears(1)

                    val year = plusYear.year
                    val selectedDateString = "$year-$formattedMonth-$formattedDay"

                    selectedDate = LocalDate.parse(selectedDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))
                }

                binding.yearMonth.text = "${selectedDate.year}년 ${selectedDate.monthValue}월"

                selectedDateTextView?.let {
                    selectedDateTextView?.let {
                        binding.clickDate.visibility = View.GONE
                    }
                }

                selectedDateTextView = dateTextView
                binding.clickDate.visibility = View.VISIBLE

                dateTextView?.viewTreeObserver?.addOnPreDrawListener(object :
                    ViewTreeObserver.OnPreDrawListener{
                    override fun onPreDraw(): Boolean {
                        dateTextView.viewTreeObserver.removeOnPreDrawListener(this)
                        val dateTextViewX = dateTextView.x
                        val dateTextViewWidth = dateTextView.width.toFloat()
                        val clickDateWidth = binding.clickDate.width.toFloat()

                        binding.clickDate.x =
                            dateTextViewX + (dateTextViewWidth - clickDateWidth) / 2
                        return true
                    }
                })

                binding.saveBtn.setOnClickListener {
                    if (binding.content.text.isNotEmpty()) {
                        saveDiaryEntry(selectedDate, binding.content.text.toString())
                        updateDiaryEntry(selectedDate, binding.content.text.toString())
                        Toast.makeText(context, "Save Diary", Toast.LENGTH_SHORT).show()
                        loadDiaryEntry(selectedDate)
                    } else {
                        Toast.makeText(context, "Empty Diary", Toast.LENGTH_SHORT).show()
                    }
                }

                binding.deleteBtn.setOnClickListener {
                    if (binding.content.text.isNotEmpty()) {
                        deleteDiaryEntry()
                        Toast.makeText(context, "Delete Diary", Toast.LENGTH_SHORT).show()
                        loadDiaryEntry(selectedDate)
                    } else {
                        Toast.makeText(context, "Empty Diary", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun formatDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd", Locale.getDefault())
        return date.format(formatter)
    }

    private fun formatDateForStorage(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        return date.format(formatter)
    }

    private fun saveDiaryEntry(date: LocalDate, content: String) {
        val entry = DiaryEntry(date = formatDateForStorage(date), content = content)
        CoroutineScope(Dispatchers.IO).launch {
            diaryDao.insert(entry)
        }
    }

    private fun loadDiaryEntry(date: LocalDate) {
        CoroutineScope(Dispatchers.IO).launch {
            val entry = diaryDao.getEntryByDate(formatDateForStorage(date))
            withContext(Dispatchers.Main) {
                if (entry != null) {
                    binding.content.setText(entry.content)
                    diaryEntry = entry
                } else {
                    binding.content.text.clear()
                    diaryEntry = DiaryEntry(date = formatDateForStorage(date), content = "")
                }
            }
        }
    }

    private fun updateDiaryEntry(date: LocalDate, newContent: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingEntry = diaryDao.getEntryByDate(formatDateForStorage(date))
            if (existingEntry != null) {
                existingEntry.content = newContent
                diaryDao.update(existingEntry)
            }
        }
    }

    private fun deleteDiaryEntry() {
        CoroutineScope(Dispatchers.IO).launch {
            diaryDao.delete(diaryEntry)
            withContext(Dispatchers.Main) {
                binding.content.text.clear()
            }
        }
    }
}
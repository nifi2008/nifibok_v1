package com.tiptrack.app

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

object Jalali {
    fun format(millis: Long, withTime: Boolean = false): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val g = intArrayOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        val j = toJalali(g[0], g[1], g[2])
        val date = "%04d/%02d/%02d".format(Locale.US, j[0], j[1], j[2])
        return if (withTime) "$date  %02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) else date
    }

    // Gregorian to Jalali conversion (Borkowski algorithm)
    private fun toJalali(gy0: Int, gm: Int, gd: Int): IntArray {
        val gdm = intArrayOf(0,31, if (isLeap(gy0)) 29 else 28,31,30,31,30,31,31,30,31,30,31)
        var gy = gy0 - 1600
        var gm0 = gm - 1
        val gd0 = gd - 1
        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm0) gDayNo += gdm[i + 1]
        gDayNo += gd0
        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (jDayNo < 186) {
            jm = 1 + jDayNo / 31
            jd = 1 + jDayNo % 31
        } else {
            jm = 7 + (jDayNo - 186) / 30
            jd = 1 + (jDayNo - 186) % 30
        }
        return intArrayOf(jy, jm, jd)
    }
    private fun isLeap(y: Int) = y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)
}

package com.tiptrack.app

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private val navy = Color.rgb(16, 27, 49)
    private val navy2 = Color.rgb(27, 43, 69)
    private val gold = Color.rgb(216, 182, 106)
    private val cream = Color.rgb(247, 247, 244)
    private val ink = Color.rgb(29, 39, 56)
    private lateinit var db: TipDb
    private lateinit var root: LinearLayout
    private var page = "home"
    private var selectedDate = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = TipDb(this)
        render()
    }

    private fun render() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            setBackgroundColor(cream)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        scroll.addView(root)
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(navy, 22)
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(txt("TIPTRACK", 21f, gold, true))
        brand.addView(txt("دفتر هوشمند انعام", 12f, Color.WHITE))
        header.addView(brand)
        val logo = TextView(this).apply {
            text = "TT"; textSize = 19f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setTextColor(navy); background = rounded(gold, 16)
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(header)
        root.addView(space(16))

        when (page) {
            "home" -> home()
            "history" -> history()
            else -> settings()
        }
        root.addView(space(16))
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = rounded(navy, 20)
        }
        nav.addView(navButton("خانه", "home"), LinearLayout.LayoutParams(0, dp(48), 1f))
        nav.addView(navButton("تاریخچه", "history"), LinearLayout.LayoutParams(0, dp(48), 1f))
        nav.addView(navButton("تنظیمات", "settings"), LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(nav)
    }

    private fun home() {
        val now = System.currentTimeMillis()
        val start = weekStart(now)
        val end = start + 7L * 24 * 60 * 60 * 1000
        val tips = db.tipsBetween(start, end)
        val pending = db.unsettledBetween(start, end)
        val total = tips.sumOf { it.amount }
        val pendingTotal = pending.sumOf { it.amount }

        root.addView(txt("داشبورد", 23f, ink, true))
        root.addView(txt("هفته کاری: پنجشنبه تا چهارشنبه", 13f, Color.GRAY))
        root.addView(space(12))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(navy, navy2)).apply { cornerRadius = dp(24).toFloat() }
        }
        hero.addView(txt("مجموع انعام‌های این هفته", 14f, Color.LTGRAY))
        hero.addView(txt(money(total), 30f, gold, true))
        hero.addView(txt("تعداد ثبت‌ها: ${tips.size}", 13f, Color.WHITE))
        hero.addView(space(12))
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        line.addView(metric("تسویه‌نشده", money(pendingTotal)), LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(metric("تعداد باقی‌مانده", "${pending.size} مورد"), LinearLayout.LayoutParams(0, -2, 1f))
        hero.addView(line)
        root.addView(hero)
        root.addView(space(14))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton("+ ثبت انعام", true) { showTipDialog(null) }, LinearLayout.LayoutParams(0, dp(54), 1f))
        actions.addView(spaceHorizontal(8))
        actions.addView(actionButton("ثبت تسویه", false) { settleWeek(start, end) }, LinearLayout.LayoutParams(0, dp(54), 1f))
        root.addView(actions)
        root.addView(space(18))
        root.addView(txt("آخرین انعام‌ها", 18f, ink, true))
        if (tips.isEmpty()) {
            root.addView(emptyCard("هنوز انعامی برای این هفته ثبت نشده؛ اولین مورد را اضافه کن."))
        } else {
            tips.take(5).forEach { root.addView(tipCard(it)) }
        }
    }

    private fun history() {
        root.addView(txt("تاریخچه انعام‌ها", 23f, ink, true))
        root.addView(txt("همه هفته‌ها و تمام ثبت‌های قبلی", 13f, Color.GRAY))
        root.addView(space(12))
        val all = db.allTips()
        if (all.isEmpty()) root.addView(emptyCard("هنوز رکوردی ثبت نشده است."))
        else all.forEach { root.addView(tipCard(it)) }
    }

    private fun settings() {
        root.addView(txt("تنظیمات و ابزارها", 23f, ink, true))
        root.addView(space(12))
        root.addView(infoCard("چرخه هفتگی", "هفته کاری از پنجشنبه شروع می‌شود و چهارشنبه پایان می‌یابد. شروع هفته جدید خودکار است؛ اطلاعات هفته‌های قبل در تاریخچه باقی می‌ماند."))
        root.addView(infoCard("ذخیره‌سازی", "اطلاعات در حافظه داخلی همین گوشی ذخیره می‌شود و برای ثبت و مشاهده به اینترنت نیاز نداری."))
        root.addView(infoCard("واحد پول", "واحد فعلی برنامه تومان است. مبلغ‌ها را به تومان وارد کن."))
        val export = actionButton("خروجی CSV از همه انعام‌ها", true) { exportCsv() }
        root.addView(export, LinearLayout.LayoutParams(-1, dp(52)))
        root.addView(space(8))
        root.addView(actionButton("راهنمای پشتیبان‌گیری", false) {
            android.app.AlertDialog.Builder(this).setTitle("پشتیبان‌گیری")
                .setMessage("برای نگهداری امن اطلاعات، از بخش خروجی CSV فایل گزارش را در فضای شخصی خود ذخیره کن. در نسخه بعدی امکان تهیه و بازیابی نسخه پشتیبان کامل اضافه می‌شود.")
                .setPositiveButton("متوجه شدم", null).show()
        }, LinearLayout.LayoutParams(-1, dp(52)))
    }

    private fun tipCard(t: Tip): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = rounded(Color.WHITE, 18)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val amount = txt(money(t.amount), 18f, navy, true)
        top.addView(amount, LinearLayout.LayoutParams(0, -2, 1f))
        val state = txt(if (t.settled) "تسویه‌شده" else "تسویه‌نشده", 11f, if (t.settled) Color.rgb(35,130,91) else Color.rgb(174,119,35), true)
        top.addView(state)
        card.addView(top)
        card.addView(txt(if (t.name.isBlank()) "بدون نام ثبت‌شده" else t.name, 15f, ink, true))
        card.addView(txt("${Jalali.format(t.createdAt, true)}${if (t.phone.isNotBlank()) "  •  ${t.phone}" else ""}", 12f, Color.GRAY))
        if (t.note.isNotBlank()) card.addView(txt(t.note, 12f, Color.DKGRAY))
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        val edit = smallButton("ویرایش") { showTipDialog(t) }
        val delete = smallButton("حذف") {
            android.app.AlertDialog.Builder(this).setTitle("حذف انعام")
                .setMessage("این رکورد حذف شود؟").setNegativeButton("انصراف", null)
                .setPositiveButton("حذف") { _, _ -> db.deleteTip(t.id); render() }.show()
        }
        buttons.addView(edit); buttons.addView(delete)
        card.addView(buttons)
        val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(9)
        card.layoutParams = lp
        return card
    }

    private fun showTipDialog(existing: Tip?) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val amount = input("مبلغ انعام به تومان", existing?.amount?.toString() ?: "")
        val name = input("نام انعام‌دهنده / مشتری", existing?.name ?: "")
        val phone = input("شماره تماس (اختیاری)", existing?.phone ?: "")
        val note = input("جزئیات یا توضیحات (اختیاری)", existing?.note ?: "")
        var date = existing?.createdAt ?: System.currentTimeMillis()
        val dateButton = Button(this).apply {
            isAllCaps = false
            text = "تاریخ و ساعت: ${Jalali.format(date, true)}"
            setOnClickListener {
                val c = Calendar.getInstance().apply { timeInMillis = date }
                DatePickerDialog(this@MainActivity, { _, y, m, d ->
                    val picked = Calendar.getInstance().apply {
                        timeInMillis = date
                        set(Calendar.YEAR, y); set(Calendar.MONTH, m); set(Calendar.DAY_OF_MONTH, d)
                    }
                    TimePickerDialog(this@MainActivity, { _, h, min ->
                        picked.set(Calendar.HOUR_OF_DAY, h); picked.set(Calendar.MINUTE, min)
                        date = picked.timeInMillis
                        text = "تاریخ و ساعت: ${Jalali.format(date, true)}"
                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
        }
        listOf(amount, name, phone, note).forEach { box.addView(it) }
        box.addView(dateButton)
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(if (existing == null) "ثبت انعام جدید" else "ویرایش انعام")
            .setView(box)
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ذخیره", null).create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                val value = amount.text.toString().trim().replace(",", "").toLongOrNull()
                if (value == null || value <= 0) {
                    amount.error = "مبلغ معتبر وارد کن"
                    return@setOnClickListener
                }
                if (existing == null) db.addTip(value, name.text.toString().trim(), phone.text.toString().trim(), note.text.toString().trim(), date)
                else db.updateTip(existing.id, value, name.text.toString().trim(), phone.text.toString().trim(), note.text.toString().trim(), date)
                dialog.dismiss(); render()
            }
        }
        dialog.show()
    }

    private fun settleWeek(start: Long, end: Long) {
        val pending = db.unsettledBetween(start, end)
        if (pending.isEmpty()) {
            android.app.AlertDialog.Builder(this).setMessage("انعام تسویه‌نشده‌ای برای این هفته وجود ندارد.")
                .setPositiveButton("باشه", null).show()
            return
        }
        val note = EditText(this).apply { hint = "توضیح تسویه (اختیاری)" }
        android.app.AlertDialog.Builder(this)
            .setTitle("تسویه هفتگی")
            .setMessage("مبلغ قابل تسویه: ${money(pending.sumOf { it.amount })}\nتعداد انعام‌ها: ${pending.size}\nبا تأیید، این موارد تسویه‌شده علامت می‌خورند.")
            .setView(note)
            .setNegativeButton("برگشت", null)
            .setPositiveButton("تأیید تسویه") { _, _ ->
                val result = db.settle(start, end, note.text.toString())
                android.app.AlertDialog.Builder(this)
                    .setMessage("${money(result.first)} برای ${result.second} انعام تسویه شد.")
                    .setPositiveButton("عالی", null).show()
                render()
            }.show()
    }

    private fun exportCsv() {
        val rows = db.allTips()
        val csv = buildString {
            append("id,amount_toman,name,phone,date_jalali,note,settled\n")
            rows.forEach { t ->
                fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
                append("${t.id},${t.amount},${q(t.name)},${q(t.phone)},${q(Jalali.format(t.createdAt, true))},${q(t.note)},${t.settled}\n")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "TipTrack - Tips export")
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        startActivity(Intent.createChooser(intent, "اشتراک‌گذاری گزارش انعام‌ها"))
    }

    private fun weekStart(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        // Calendar.THURSDAY is 5; Thursday is the first day of the business week.
        val back = (c.get(Calendar.DAY_OF_WEEK) - Calendar.THURSDAY + 7) % 7
        c.add(Calendar.DAY_OF_YEAR, -back)
        return c.timeInMillis
    }

    private fun metric(title: String, value: String): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(10), dp(10), dp(8)); background = rounded(Color.argb(35,255,255,255), 14) }
        box.addView(txt(title, 11f, Color.LTGRAY))
        box.addView(txt(value, 15f, Color.WHITE, true))
        return box
    }
    private fun infoCard(title: String, body: String): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)); background = rounded(Color.WHITE, 18) }
        box.addView(txt(title, 16f, navy, true)); box.addView(txt(body, 13f, Color.DKGRAY))
        val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(10); box.layoutParams = lp
        return box
    }
    private fun emptyCard(message: String): View = infoCard("هنوز چیزی ثبت نشده", message)
    private fun actionButton(label: String, primary: Boolean, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 14f
        setTextColor(if (primary) navy else Color.WHITE)
        background = rounded(if (primary) gold else navy, 16)
        setOnClickListener { action() }
    }
    private fun navButton(label: String, target: String) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 12f
        setTextColor(if (page == target) gold else Color.WHITE)
        background = rounded(Color.TRANSPARENT, 12)
        setOnClickListener { page = target; render() }
    }
    private fun smallButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 11f; setOnClickListener { action() }
    }
    private fun input(hintText: String, value: String) = EditText(this).apply {
        hint = hintText; setText(value); textSize = 14f; setSingleLine(true)
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }
    private fun txt(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(3), 0, dp(3))
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun space(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun spaceHorizontal(w: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(w), 1) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(value: Long): String = NumberFormat.getNumberInstance(Locale("fa", "IR")).format(value) + " تومان"
}

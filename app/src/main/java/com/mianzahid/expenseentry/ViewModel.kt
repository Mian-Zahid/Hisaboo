package com.mianzahid.expenseentry

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

fun sha256(s: String): String =
    MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        .joinToString("") { "%02x".format(it) }

fun fmtRs(v: Long): String = "Rs " + "%,d".format(v)

// One hadith for the Home screen: Urdu text + English text + book + number.
// The reference is always book + hadith number — never shown without it.
data class DailyHadith(
    val textUrdu: String,
    val textEn: String,
    val book: String, // e.g. "Sahih al-Bukhari"
    val number: Int, // hadith number within the book
    val section: String // e.g. "Revelation"
)

fun fullDate(millis: Long, lang: String): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val days = listOf("sun","mon","tue","wed","thu","fri","sat")
    val dow = Strings.get(lang, days[cal.get(Calendar.DAY_OF_WEEK) - 1])
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val months = listOf("jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec")
    val mon = Strings.get(lang, months[cal.get(Calendar.MONTH)])
    val year = cal.get(Calendar.YEAR)
    return if (lang == "ur") "$dow، $day $mon $year" else "$dow, $day $mon $year"
}

// Localized timestamp for "last updated / last backup" lines: day + translated
// month + year + time (never English month names).
fun localizedStamp(millis: Long, lang: String): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val months = listOf("jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec")
    val mon = Strings.get(lang, months[cal.get(Calendar.MONTH)])
    val year = cal.get(Calendar.YEAR)
    val hh = "%02d".format(cal.get(Calendar.HOUR_OF_DAY))
    val mm = "%02d".format(cal.get(Calendar.MINUTE))
    return "$day $mon $year $hh:$mm"
}

fun shortDate(millis: Long, lang: String): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val months = listOf("jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec")
    val mon = Strings.get(lang, months[cal.get(Calendar.MONTH)])
    return "$day $mon ${cal.get(Calendar.YEAR)}"
}

fun startOfDay(millis: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

data class Dashboard(
    val totalIn: Long = 0,
    val totalOut: Long = 0,
    val balance: Long = 0,
    val todayIn: Long = 0,
    val todayOut: Long = 0,
    val monthIn: Long = 0,
    val monthOut: Long = 0
)

class AppViewModel(private val ctx: Context) : ViewModel() {
    private val dao = AppDao(ctx)
    private val prefs: SharedPreferences = ctx.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)

    private val _lang = MutableStateFlow(prefs.getString("lang", "en") ?: "en")
    val lang: StateFlow<String> = _lang.asStateFlow()
    private val _theme = MutableStateFlow(prefs.getString("theme", "auto") ?: "auto")
    val theme: StateFlow<String> = _theme.asStateFlow()
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()
    private val _viewer = MutableStateFlow<Member?>(null) // null = not chosen; id 0 admin handled via flag
    val viewer: StateFlow<Member?> = _viewer.asStateFlow()
    private val _isAdmin = MutableStateFlow(true)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()
    private val _invites = MutableStateFlow<List<Invite>>(emptyList())
    val invites: StateFlow<List<Invite>> = _invites.asStateFlow()
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()
    private val _udhar = MutableStateFlow<List<Udhar>>(emptyList())
    val udhar: StateFlow<List<Udhar>> = _udhar.asStateFlow()
    private val _bills = MutableStateFlow<List<Bill>>(emptyList())
    val bills: StateFlow<List<Bill>> = _bills.asStateFlow()
    private val _recurring = MutableStateFlow<List<Recurring>>(emptyList())
    val recurring: StateFlow<List<Recurring>> = _recurring.asStateFlow()
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()
    private val _rates = MutableStateFlow<Map<String, Double>>(mapOf("USD" to 276.63, "EUR" to 310.65, "GBP" to 372.36, "AED" to 75.32, "SAR" to 73.77, "PKR" to 1.0))
    val rates: StateFlow<Map<String, Double>> = _rates.asStateFlow()
    private val _ratesUpdated = MutableStateFlow(prefs.getString("rates_updated", "") ?: "")
    val ratesUpdated: StateFlow<String> = _ratesUpdated.asStateFlow()
    private val _ratesLive = MutableStateFlow(false)
    val ratesLive: StateFlow<Boolean> = _ratesLive.asStateFlow()
    private val _shortcuts = MutableStateFlow<List<String>>(emptyList())
    val shortcuts: StateFlow<List<String>> = _shortcuts.asStateFlow()
    private val _lastBackup = MutableStateFlow(prefs.getString("last_backup", "") ?: "")
    val lastBackup: StateFlow<String> = _lastBackup.asStateFlow()
    private val _toast = MutableStateFlow("")
    val toast: StateFlow<String> = _toast.asStateFlow()
    // Default hadith (Sahih al-Bukhari #1 — actions are but by intentions) shown
    // only if no hadith was ever fetched and cached (e.g. first launch offline).
    private val defaultHadith = DailyHadith(
        textUrdu = "کو حمیدی نے یہ حدیث بیان کی، انہوں نے کہا کہ ہم کو سفیان نے یہ حدیث بیان کی، وہ کہتے ہیں ہم کو یحییٰ بن سعید انصاری نے یہ حدیث بیان کی، انہوں نے کہا کہ مجھے یہ حدیث محمد بن ابراہیم تیمی سے حاصل ہوئی۔ انہوں نے اس حدیث کو علقمہ بن وقاص لیثی سے سنا، ان کا بیان ہے کہ میں نے مسجد نبوی میں منبر رسول صلی اللہ علیہ وسلم پر عمر بن خطاب رضی اللہ عنہ کی زبان سے سنا، وہ فرما رہے تھے کہ میں نے جناب رسول اللہ صلی اللہ علیہ وسلم سے سنا آپ صلی اللہ علیہ وسلم فرما رہے تھے کہ تمام اعمال کا دارومدار نیت پر ہے اور ہر عمل کا نتیجہ ہر انسان کو اس کی نیت کے مطابق ہی ملے گا۔ پس جس کی ہجرت ( ترک وطن ) دولت دنیا حاصل کرنے کے لیے ہو یا کسی عورت سے شادی کی غرض ہو۔ پس اس کی ہجرت ان ہی چیزوں کے لیے ہو گی جن کے حاصل کرنے کی نیت سے اس نے ہجرت کی ہے۔",
        textEn = "Narrated 'Umar bin Al-Khattab: I heard Allah's Messenger (ﷺ) saying, \"The reward of deeds depends upon the intentions and every person will get the reward according to what he has intended. So whoever emigrated for worldly benefits or for a woman to marry, his emigration was for what he emigrated for\"",
        book = "Sahih al-Bukhari",
        number = 1,
        section = "Revelation"
    )
    private val _dailyHadith = MutableStateFlow(defaultHadith)
    val dailyHadith: StateFlow<DailyHadith> = _dailyHadith.asStateFlow()

    fun t(key: String): String = Strings.get(_lang.value, key)

    // Display name for a category: default categories are translated (their stored
    // names are English); user-created categories show the name the user typed.
    fun catName(cat: Category): String {
        if (!cat.isDefault) return cat.name
        return when (cat.name.trim().lowercase()) {
            "salary" -> t("catSalary")
            "business" -> t("catBusiness")
            "other income" -> t("catOtherIncome")
            "food" -> t("catFood")
            "transport" -> t("catTransport")
            "shopping" -> t("catShopping")
            "bills" -> t("catBills")
            "rent" -> t("catRent")
            "other expense" -> t("catOtherExpense")
            else -> cat.name
        }
    }

    // Stored system words are English ("Admin", "Recurring") — translate at display.
    fun memberDisplayName(name: String): String =
        if (name.trim().equals("Admin", ignoreCase = true)) t("admin") else name
    fun noteDisplay(note: String): String =
        if (note.trim().equals("Recurring", ignoreCase = true)) t("recurring") else note
    fun showToast(s: String) { _toast.value = s }
    fun clearToast() { _toast.value = "" }

    fun hasPin(): Boolean = prefs.contains("admin_pin_hash")
    private val _fingerprintEnabled = MutableStateFlow(prefs.getBoolean("fingerprint", false))
    val fingerprintEnabled: StateFlow<Boolean> = _fingerprintEnabled.asStateFlow()
    fun setFingerprint(v: Boolean) { _fingerprintEnabled.value = v; prefs.edit().putBoolean("fingerprint", v).apply() }

    fun setLang(l: String) { _lang.value = l; prefs.edit().putString("lang", l).apply() }
    fun setTheme(th: String) { _theme.value = th; prefs.edit().putString("theme", th).apply() }

    fun dashboard(): Dashboard {
        val list = _entries.value
        val totalIn = list.filter { it.type == "IN" }.sumOf { it.amount }
        val totalOut = list.filter { it.type == "OUT" }.sumOf { it.amount }
        val today = startOfDay(System.currentTimeMillis())
        val todayIn = list.filter { it.type == "IN" && it.dateMillis >= today }.sumOf { it.amount }
        val todayOut = list.filter { it.type == "OUT" && it.dateMillis >= today }.sumOf { it.amount }
        val cal = Calendar.getInstance()
        val m = cal.get(Calendar.MONTH); val y = cal.get(Calendar.YEAR)
        val monthIn = list.filter { it.type == "IN" && Calendar.getInstance().apply { timeInMillis = it.dateMillis }.get(Calendar.MONTH) == m && Calendar.getInstance().apply { timeInMillis = it.dateMillis }.get(Calendar.YEAR) == y }.sumOf { it.amount }
        val monthOut = list.filter { it.type == "OUT" && Calendar.getInstance().apply { timeInMillis = it.dateMillis }.get(Calendar.MONTH) == m && Calendar.getInstance().apply { timeInMillis = it.dateMillis }.get(Calendar.YEAR) == y }.sumOf { it.amount }
        return Dashboard(totalIn, totalOut, totalIn - totalOut, todayIn, todayOut, monthIn, monthOut)
    }

    fun refresh() {
        viewModelScope.launch {
            try {
            _categories.value = dao.allCategories()
            _members.value = dao.allMembers()
            _invites.value = dao.allInvites()
            _budgets.value = dao.allBudgets()
            _udhar.value = dao.allUdhar()
            _bills.value = dao.allBills()
            _recurring.value = dao.allRecurring()
            _reminders.value = dao.allReminders()
            loadEntries()
            val sc = dao.setting("shortcuts")?.value
            _shortcuts.value = if (sc.isNullOrBlank()) List(6) { "" } else sc.split(",").let { if (it.size < 6) it + List(6 - it.size) { "" } else it.take(6) }
            val r = dao.setting("rates")?.value
            if (!r.isNullOrBlank()) {
                try {
                    val j = JSONObject(r)
                    val map = mutableMapOf<String, Double>()
                    j.keys().forEach { k -> map[k] = j.getDouble(k) }
                    if (map.isNotEmpty()) _rates.value = map
                } catch (_: Exception) { }
            }
            applyRecurring()
            } catch (e: Exception) {
                // Startup must never crash the app: keep whatever data loaded so far
                // and let the UI show (empty lists if the DB could not be read).
            }
        }
    }

    private suspend fun loadEntries() {
        val v = _viewer.value
        _entries.value = when {
            _isAdmin.value -> dao.allEntries()
            v != null -> {
                val own = dao.entriesForMember(v.id)
                val assigned = dao.allEntries().filter { it.type == "IN" && it.memberId == v.id }
                (own + assigned).distinctBy { it.id }.sortedByDescending { it.dateMillis }
            }
            else -> dao.allEntries()
        }
    }

    fun unlockAsAdmin() { _isAdmin.value = true; _viewer.value = null; _unlocked.value = true }
    fun unlockAsMember(m: Member) { _isAdmin.value = false; _viewer.value = m; _unlocked.value = true }
    fun lock() { _unlocked.value = false }

    fun tryPin(pin: String): Boolean {
        val h = sha256(pin)
        if (prefs.getString("admin_pin_hash", "") == h) { unlockAsAdmin(); return true }
        val m = _members.value.firstOrNull { it.pinHash == h }
        if (m != null) { unlockAsMember(m); return true }
        return false
    }

    fun setAdminPin(pin: String) { prefs.edit().putString("admin_pin_hash", sha256(pin)).apply() }
    fun changeAdminPin(old: String, new: String): Boolean {
        if (prefs.getString("admin_pin_hash", "") != sha256(old)) return false
        setAdminPin(new); return true
    }

    fun seedIfEmpty() {
        viewModelScope.launch {
            try {
            if (dao.allCategories().isEmpty()) {
                val defaults = listOf(
                    Category(name = "Salary", type = "IN", isDefault = true),
                    Category(name = "Business", type = "IN", isDefault = true),
                    Category(name = "Other Income", type = "IN", isDefault = true),
                    Category(name = "Food", type = "OUT", isDefault = true),
                    Category(name = "Transport", type = "OUT", isDefault = true),
                    Category(name = "Shopping", type = "OUT", isDefault = true),
                    Category(name = "Bills", type = "OUT", isDefault = true),
                    Category(name = "Rent", type = "OUT", isDefault = true),
                    Category(name = "Other Expense", type = "OUT", isDefault = true)
                )
                defaults.forEach { dao.insertCategory(it) }
            }
            } catch (e: Exception) {
                // Seeding must never crash first launch: an empty DB is a safe fallback.
            }
            refresh()
        }
    }

    fun addEntry(categoryId: Int, amount: Long, note: String, dateMillis: Long, memberId: Int = 0, memberName: String = "Admin", receiptUri: String = "") {
        viewModelScope.launch {
            val cat = dao.categoryById(categoryId) ?: return@launch
            val mid = if (_isAdmin.value) memberId else (_viewer.value?.id ?: 0)
            val mname = if (_isAdmin.value) memberName else (_viewer.value?.name ?: "Admin")
            val type = if (_isAdmin.value) cat.type else "OUT" // members can only add expenses
            dao.insertEntry(Entry(categoryId = categoryId, amount = amount, type = type, note = note, dateMillis = dateMillis, memberId = mid, memberName = mname, receiptUri = receiptUri))
            applyRecurring()
            refresh()
        }
    }

    fun deleteEntry(id: Int) {
        viewModelScope.launch {
            val e = dao.allEntries().firstOrNull { it.id == id } ?: return@launch
            if (!_isAdmin.value) {
                val v = _viewer.value ?: return@launch
                if (e.memberId != v.id) { showToast(t("notAllowed")); return@launch }
            }
            dao.deleteEntry(id); refresh()
        }
    }

    fun updateEntry(e: Entry) { viewModelScope.launch { dao.updateEntry(e); refresh() } }

    fun addCategory(name: String, type: String) {
        viewModelScope.launch { dao.insertCategory(Category(name = name, type = type)); refresh() }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch { dao.deleteCategory(id); dao.deleteBudget(id); refresh() }
    }

    fun setBudget(catId: Int, amount: Long) {
        viewModelScope.launch {
            val cat = dao.categoryById(catId) ?: return@launch
            if (cat.type != "OUT") return@launch // budgets only for expense categories
            dao.upsertBudget(Budget(categoryId = catId, amount = amount)); refresh()
        }
    }

    fun deleteBudget(catId: Int) { viewModelScope.launch { dao.deleteBudget(catId); refresh() } }

    fun addUdhar(person: String, amount: Long, kind: String, dueMillis: Long = 0) {
        viewModelScope.launch { dao.insertUdhar(Udhar(personName = person, amount = amount, kind = kind, dateMillis = System.currentTimeMillis(), dueMillis = dueMillis)); refresh() }
    }

    fun markUdharPaid(u: Udhar) { viewModelScope.launch { dao.updateUdhar(u.copy(status = "paid")); refresh() } }
    fun deleteUdhar(id: Int) { viewModelScope.launch { dao.deleteUdhar(id); refresh() } }

    fun addBill(name: String, provider: String, amount: Long, dueMillis: Long, photoUri: String = "") {
        viewModelScope.launch { dao.insertBill(Bill(name = name, provider = provider, amount = amount, dueMillis = dueMillis, photoUri = photoUri)); refresh() }
    }

    fun markBillPaid(b: Bill) { viewModelScope.launch { dao.updateBill(b.copy(status = "paid")); refresh() } }
    fun deleteBill(id: Int) { viewModelScope.launch { dao.deleteBill(id); refresh() } }

    fun addRecurring(catId: Int, amount: Long, note: String, day: Int) {
        viewModelScope.launch { dao.insertRecurring(Recurring(categoryId = catId, amount = amount, note = note, dayOfMonth = day)); refresh() }
    }

    fun deleteRecurring(id: Int) { viewModelScope.launch { dao.deleteRecurring(id); refresh() } }

    fun addReminder(title: String, dateMillis: Long) {
        viewModelScope.launch { dao.insertReminder(Reminder(title = title, dateMillis = dateMillis)); refresh() }
    }

    fun toggleReminder(r: Reminder) { viewModelScope.launch { dao.updateReminder(r.copy(done = !r.done)); refresh() } }
    fun deleteReminder(id: Int) { viewModelScope.launch { dao.deleteReminder(id); refresh() } }

    private suspend fun applyRecurring() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH); val year = cal.get(Calendar.YEAR)
        for (r in dao.allRecurring()) {
            if (today < r.dayOfMonth) continue
            val last = Calendar.getInstance().apply { timeInMillis = r.lastAppliedMillis }
            if (r.lastAppliedMillis > 0 && last.get(Calendar.MONTH) == month && last.get(Calendar.YEAR) == year) continue
            val cat = dao.categoryById(r.categoryId) ?: continue
            val dateCal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, r.dayOfMonth); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            dao.insertEntry(Entry(categoryId = r.categoryId, amount = r.amount, type = cat.type, note = r.note.ifBlank { "Recurring" }, dateMillis = dateCal.timeInMillis, memberId = 0, memberName = "Admin"))
            dao.updateRecurring(r.copy(lastAppliedMillis = now))
        }
    }

    fun createInvite(): String {
        val code = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
        viewModelScope.launch {
            dao.insertInvite(Invite(code = code, createdAt = System.currentTimeMillis(), expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L, status = "active"))
            refresh()
        }
        return code
    }

    fun revokeInvite(id: Int) {
        viewModelScope.launch {
            val inv = dao.allInvites().firstOrNull { it.id == id } ?: return@launch
            dao.updateInvite(inv.copy(status = "revoked")); refresh()
        }
    }

    fun previewInvite(code: String): Invite? {
        val inv = _invites.value.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: return null
        if (inv.status != "active") return null
        if (System.currentTimeMillis() > inv.expiresAt) return null
        return inv
    }

    fun joinWithInvite(code: String, name: String, pin: String): Boolean {
        val inv = previewInvite(code) ?: return false
        viewModelScope.launch {
            val id = dao.insertMember(Member(name = name, pinHash = sha256(pin)))
            dao.updateInvite(inv.copy(status = "used", usedBy = name))
            refresh()
        }
        return true
    }

    fun addMember(name: String, pin: String) {
        viewModelScope.launch { dao.insertMember(Member(name = name, pinHash = sha256(pin))); refresh() }
    }

    fun deleteMember(id: Int) { viewModelScope.launch { dao.deleteMember(id); refresh() } }

    fun saveShortcuts(list: List<String>) {
        _shortcuts.value = list
        viewModelScope.launch { dao.putSetting(Setting("shortcuts", list.joinToString(","))) }
    }

    fun fetchRates() {
        viewModelScope.launch(Dispatchers.IO) {
            // Network I/O must run off the main thread (viewModelScope defaults
            // to Main) or Android throws NetworkOnMainThreadException.
            // Try the primary API, then a fallback mirror. Timeouts are generous
            // because the owner's mobile data is slow (8s was timing out).
            val endpoints = listOf(
                "https://open.er-api.com/v6/latest/USD",
                "https://api.exchangerate-api.com/v4/latest/USD"
            )
            var lastError: Exception? = null
            for (endpoint in endpoints) {
                try {
                    val url = URL(endpoint)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 20000; conn.readTimeout = 30000
                    val text = conn.inputStream.bufferedReader().readText()
                    val j = JSONObject(text).getJSONObject("rates")
                    val map = mutableMapOf<String, Double>()
                    listOf("PKR","EUR","GBP","AED","SAR","INR","CNY","JPY","AUD","CAD").forEach { c ->
                        if (j.has(c)) map[c] = j.getDouble(c)
                    }
                    map["USD"] = 1.0
                    // store as PKR per 1 unit: invert USD-based rates (rates are per USD)
                    val pkrPerUsd = map["PKR"] ?: 276.63
                    val out = mutableMapOf<String, Double>()
                    out["PKR"] = 1.0
                    out["USD"] = pkrPerUsd
                    map.forEach { (k, v) -> if (k != "PKR" && k != "USD") out[k] = pkrPerUsd / v }
                    _rates.value = out
                    _ratesLive.value = true
                    val stamp = localizedStamp(System.currentTimeMillis(), _lang.value)
                    _ratesUpdated.value = stamp
                    prefs.edit().putString("rates_updated", stamp).apply()
                    dao.putSetting(Setting("rates", JSONObject(out as Map<*, *>).toString()))
                    showToast(t("ratesUpdated"))
                    return@launch
                } catch (e: Exception) {
                    lastError = e
                    // try the next endpoint
                }
            }
            _ratesLive.value = false
            val reason = lastError?.javaClass?.simpleName ?: "Unknown"
            showToast("${t("ratesFailed")}: $reason ${t("usingOffline")}")
        }
    }

    // Daily Quranic verse for the Home screen (AlQuran Cloud API, free, no key).
    // One hadith a day from Sahih al-Bukhari (fawazahmed0/hadith-api via jsDelivr).
    // Hadith number is derived from the date: stable within a day, new every day.
    // Runs on Dispatchers.IO (network must never run on the main thread).
    // Offline: shows the cached hadith; if never cached, the default (Bukhari #1).
    // The reference is always book + hadith number — a hadith is never shown
    // without it.
    fun fetchDailyHadith() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
            // Step 1: Use today's cached hadith if we already fetched it.
            try {
                val cached = dao.setting("daily_hadith")?.value
                if (cached != null) {
                    val cj = JSONObject(cached)
                    if (cj.optString("date") == today && (cj.optString("textUrdu").isNotBlank() || cj.optString("textEn").isNotBlank())) {
                        _dailyHadith.value = DailyHadith(
                            cj.optString("textUrdu"), cj.optString("textEn"),
                            cj.optString("book"), cj.optInt("number"),
                            cj.optString("section")
                        )
                        return@launch
                    }
                }
            } catch (_: Exception) { /* fall through to network */ }
            // Step 2: Fetch today's hadith (Urdu + English editions).
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val number = (dayOfYear * 17 % 7563) + 1 // Sahih al-Bukhari has 7563 hadiths
            try {
                val urduJson = fetchUrl("https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/urd-bukhari/$number.json")
                val enJson = fetchUrl("https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-bukhari/$number.json")
                val urduObj = JSONObject(urduJson)
                val enObj = JSONObject(enJson)
                val urduHadith = urduObj.optJSONArray("hadiths")?.optJSONObject(0)
                val enHadith = enObj.optJSONArray("hadiths")?.optJSONObject(0)
                val textUrdu = urduHadith?.optString("text") ?: ""
                val textEn = enHadith?.optString("text") ?: ""
                if (textUrdu.isBlank() && textEn.isBlank()) return@launch // keep current
                val section = urduObj.optJSONObject("metadata")?.optJSONObject("section")?.optString("1") ?: ""
                val hadith = DailyHadith(textUrdu, textEn, "Sahih al-Bukhari", number, section)
                _dailyHadith.value = hadith
                val cj = JSONObject().apply {
                    put("date", today); put("number", number)
                    put("textUrdu", textUrdu); put("textEn", textEn)
                    put("book", hadith.book); put("section", section)
                }
                dao.putSetting(Setting("daily_hadith", cj.toString()))
            } catch (_: Exception) {
                // Offline: fall back to the last cached hadith (any day), else default.
                try {
                    val cached = dao.setting("daily_hadith")?.value
                    if (cached != null) {
                        val cj = JSONObject(cached)
                        if (cj.optString("textUrdu").isNotBlank() || cj.optString("textEn").isNotBlank()) {
                            _dailyHadith.value = DailyHadith(
                                cj.optString("textUrdu"), cj.optString("textEn"),
                                cj.optString("book"), cj.optInt("number"),
                                cj.optString("section")
                            )
                        }
                    }
                } catch (_: Exception) { /* keep default */ }
            }
        }
    }

    private fun fetchUrl(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 20000; conn.readTimeout = 30000
        return conn.inputStream.bufferedReader().readText()
    }

    fun setRate(currency: String, pkrPerUnit: Double) {
        val m = _rates.value.toMutableMap(); m[currency] = pkrPerUnit
        _rates.value = m; _ratesLive.value = false
        viewModelScope.launch { dao.putSetting(Setting("rates", JSONObject(m as Map<*, *>).toString())) }
        showToast(t("rateSaved"))
    }

    // ---------- Backup / Restore ----------
    fun buildBackupJson(): String {
        val j = JSONObject()
        j.put("app", "Expense Entry App")
        j.put("version", 1)
        j.put("exportedAt", System.currentTimeMillis())
        fun catArr(): JSONArray { val a = JSONArray(); _categories.value.forEach { c -> a.put(JSONObject().put("id", c.id).put("name", c.name).put("type", c.type)) }; return a }
        fun entArr(): JSONArray { val a = JSONArray(); _entries.value.forEach { e -> a.put(JSONObject().put("categoryId", e.categoryId).put("amount", e.amount).put("type", e.type).put("note", e.note).put("dateMillis", e.dateMillis).put("memberId", e.memberId).put("memberName", e.memberName)) }; return a }
        j.put("categories", catArr()); j.put("entries", entArr())
        val b = JSONArray(); _budgets.value.forEach { x -> b.put(JSONObject().put("categoryId", x.categoryId).put("amount", x.amount)) }; j.put("budgets", b)
        val u = JSONArray(); _udhar.value.forEach { x -> u.put(JSONObject().put("personName", x.personName).put("amount", x.amount).put("kind", x.kind).put("dateMillis", x.dateMillis).put("dueMillis", x.dueMillis).put("status", x.status)) }; j.put("udhar", u)
        val bl = JSONArray(); _bills.value.forEach { x -> bl.put(JSONObject().put("name", x.name).put("provider", x.provider).put("amount", x.amount).put("dueMillis", x.dueMillis).put("status", x.status)) }; j.put("bills", bl)
        val r = JSONArray(); _recurring.value.forEach { x -> r.put(JSONObject().put("categoryId", x.categoryId).put("amount", x.amount).put("note", x.note).put("dayOfMonth", x.dayOfMonth)) }; j.put("recurring", r)
        val rm = JSONArray(); _reminders.value.forEach { x -> rm.put(JSONObject().put("title", x.title).put("dateMillis", x.dateMillis).put("done", x.done)) }; j.put("reminders", rm)
        val sc = daoShortcutJson(); j.put("shortcuts", sc)
        return j.toString(2)
    }

    private fun daoShortcutJson(): JSONArray {
        val a = JSONArray(); _shortcuts.value.forEach { a.put(it) }; return a
    }

    fun saveServerBackup(): Boolean {
        return try {
            val json = buildBackupJson()
            val f = File(ctx.filesDir, "backup.json")
            f.writeText(json)
            val stamp = localizedStamp(System.currentTimeMillis(), _lang.value)
            _lastBackup.value = stamp
            prefs.edit().putString("last_backup", stamp).apply()
            true
        } catch (e: Exception) { false }
    }

    fun loadSavedBackupJson(): String? {
        val f = File(ctx.filesDir, "backup.json")
        return if (f.exists()) f.readText() else null
    }

    fun restoreFromJson(json: String): Boolean {
        return try {
            val j = JSONObject(json)
            if (j.optString("app") != "Expense Entry App") return false
            viewModelScope.launch {
                dao.clearEntries(); dao.clearBudgets(); dao.clearUdhar(); dao.clearBills(); dao.clearRecurring(); dao.clearReminders()
                val cats = j.optJSONArray("categories")
                val catMap = mutableMapOf<Int, Int>()
                if (cats != null) {
                    for (i in 0 until cats.length()) {
                        val c = cats.getJSONObject(i)
                        if (dao.allCategories().none { it.name == c.getString("name") }) {
                            val id = dao.insertCategory(Category(name = c.getString("name"), type = c.getString("type"))).toInt()
                            catMap[c.getInt("id")] = id
                        } else {
                            catMap[c.getInt("id")] = dao.allCategories().first { it.name == c.getString("name") }.id
                        }
                    }
                }
                val ents = j.optJSONArray("entries")
                if (ents != null) for (i in 0 until ents.length()) {
                    val e = ents.getJSONObject(i)
                    val cid = catMap[e.optInt("categoryId")] ?: e.optInt("categoryId")
                    dao.insertEntry(Entry(categoryId = cid, amount = e.getLong("amount"), type = e.getString("type"), note = e.optString("note"), dateMillis = e.getLong("dateMillis"), memberId = e.optInt("memberId"), memberName = e.optString("memberName", "Admin")))
                }
                val bs = j.optJSONArray("budgets")
                if (bs != null) for (i in 0 until bs.length()) {
                    val b = bs.getJSONObject(i)
                    dao.upsertBudget(Budget(categoryId = catMap[b.optInt("categoryId")] ?: b.optInt("categoryId"), amount = b.getLong("amount")))
                }
                val us = j.optJSONArray("udhar")
                if (us != null) for (i in 0 until us.length()) {
                    val u = us.getJSONObject(i)
                    dao.insertUdhar(Udhar(personName = u.getString("personName"), amount = u.getLong("amount"), kind = u.getString("kind"), dateMillis = u.optLong("dateMillis"), dueMillis = u.optLong("dueMillis"), status = u.optString("status", "pending")))
                }
                val bls = j.optJSONArray("bills")
                if (bls != null) for (i in 0 until bls.length()) {
                    val b = bls.getJSONObject(i)
                    dao.insertBill(Bill(name = b.getString("name"), provider = b.getString("provider"), amount = b.getLong("amount"), dueMillis = b.getLong("dueMillis"), status = b.optString("status", "pending")))
                }
                refresh()
            }
            true
        } catch (e: Exception) { false }
    }

    fun exportCsv(): String {
        val sb = StringBuilder("Date,Category,Type,Amount (PKR),Note,Member\n")
        val catMap = _categories.value.associateBy { it.id }
        _entries.value.forEach { e ->
            sb.append("${shortDate(e.dateMillis, "en")},${catMap[e.categoryId]?.name ?: ""},${e.type},${e.amount},\"${e.note.replace("\"", "'")}\",${e.memberName}\n")
        }
        return sb.toString()
    }
}

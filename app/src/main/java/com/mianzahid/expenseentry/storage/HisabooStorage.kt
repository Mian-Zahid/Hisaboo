package com.mianzahid.expenseentry.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.mianzahid.expenseentry.model.*
import com.mianzahid.expenseentry.storage.GoogleCloudBackupManager
import com.mianzahid.expenseentry.storage.HisabooStorage
import com.mianzahid.expenseentry.ui.theme.isHisabooDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HisabooViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = HisabooStorage(application.applicationContext)
    val cloudManager = GoogleCloudBackupManager(application.applicationContext)

    // Categories
    val categories = mutableStateListOf<CategoryItem>()

    // Transactions
    val transactions = mutableStateListOf<TransactionEntry>()

    // Budgets per category
    val categoryBudgets = mutableStateMapOf<String, Double>()

    // Sub-modules state
    val plannedPayments = mutableStateListOf<PlannedPaymentItem>()
    val reminders = mutableStateListOf<ReminderItem>()
    val loans = mutableStateListOf<LoanItem>()
    val utilityBills = mutableStateListOf<UtilityBillItem>()
    val familyMembers = mutableStateListOf<FamilyMemberItem>()
    var inviteCode by mutableStateOf<String?>(null)

    // Security, Theme & Language Settings (Default: Light Mode & English)
    var fingerprintEnabled by mutableStateOf(false)
    var appLanguage by mutableStateOf("English")
    var isDarkMode by mutableStateOf(false)
    var isAppLocked by mutableStateOf(true)
    var currentPin by mutableStateOf("1234")
    var lastBackupText by mutableStateOf("Never")
    var usdToPkrRate by mutableDoubleStateOf(278.50)
    var isFetchingRate by mutableStateOf(false)
    var rateStatusText by mutableStateOf("Tap refresh for live rate")
    val liveCurrencyRates = mutableStateMapOf(
        "PKR" to 278.50,
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "SAR" to 3.75,
        "AED" to 3.67,
        "INR" to 84.40,
        "CAD" to 1.39,
        "AUD" to 1.54,
        "CNY" to 7.24,
        "TRY" to 34.50,
        "KWD" to 0.31
    )

    // Gmail / Google Cloud Sync State
    var connectedGoogleEmail by mutableStateOf<String?>(null)
    var autoCloudSyncEnabled by mutableStateOf(true)
    var isCloudSyncing by mutableStateOf(false)
    var cloudStatusBanner by mutableStateOf<String?>(null)

    // Live Hadith State (Refreshes every time app opens or unlocks — no timer)
    var selectedHadithLang by mutableStateOf(HadithLanguage.URDU)
    var isFetchingHadith by mutableStateOf(false)
    private var lastHadithNumber by mutableIntStateOf(-1)

    private val offlineHadithPool = listOf(
        DailyHadith(
            arabic = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
            urdu = "اعمال کا دارومدار نیتوں پر ہے اور ہر شخص کو وہی ملے گا جس کی اس نے نیت کی۔",
            english = "Actions are judged by intentions, and every person will get the reward according to what they intended.",
            reference = "Sahih al-Bukhari 1"
        ),
        DailyHadith(
            arabic = "التَّاجِرُ الصَّدُوقُ الأَمِينُ مَعَ النَّبِيِّينَ وَالصِّدِّيقِينَ وَالشُّهَدَاءِ",
            urdu = "سچا اور امانت دار تاجر قیامت کے دن نبیوں، صدیقوں اور شہیدوں کے ساتھ ہوگا۔",
            english = "The truthful, trustworthy merchant will be with the Prophets, the truthful, and the martyrs.",
            reference = "Jami at-Tirmidhi 1209"
        ),
        DailyHadith(
            arabic = "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ",
            urdu = "صدقہ دینے سے مال میں کبھی کمی نہیں آتی۔",
            english = "Charity does not decrease wealth in any way.",
            reference = "Sahih Muslim 2588"
        ),
        DailyHadith(
            arabic = "خَيْرُ النَّاسِ أَنْفَعُهُمْ لِلنَّاسِ",
            urdu = "لوگوں میں سب سے بہتر وہ ہے جو دوسروں کو سب سے زیادہ فائدہ پہنچائے۔",
            english = "The best of people are those who are most beneficial to people.",
            reference = "Al-Mu'jam al-Awsat 6192"
        ),
        DailyHadith(
            arabic = "الْيَدُ الْعُلْيَا خَيْرٌ مِنَ الْيَدِ السُّفْلَى",
            urdu = "اوپر والا (دینے والا) ہاتھ نیچے والے (لینے والے) ہاتھ سے بہتر ہے۔",
            english = "The upper hand (the one that gives) is better than the lower hand (the one that receives).",
            reference = "Sahih al-Bukhari 1427"
        ),
        DailyHadith(
            arabic = "مَنْ أَنْظَرَ مُعْسِرًا أَوْ وَضَعَ عَنْهُ أَظَلَّهُ اللَّهُ فِي ظِلِّهِ",
            urdu = "جس نے کسی تنگ دست قرض دار کو مہلت دی یا اس کا قرض معاف کر دیا، اللہ اسے قیامت کے دن اپنے سائے میں جگہ دے گا۔",
            english = "Whoever grants respite to an indebted person in hardship or waives the debt, Allah will shade him under His Throne.",
            reference = "Sahih Muslim 3006"
        ),
        DailyHadith(
            arabic = "مَطْلُ الْغَنِيِّ ظُلْمٌ",
            urdu = "مال دار شخص کا قرض کی ادائیگی میں ٹال مٹول کرنا ظلم ہے۔",
            english = "Procrastination in repaying a debt by a wealthy person is injustice.",
            reference = "Sahih al-Bukhari 2287"
        ),
        DailyHadith(
            arabic = "لَيْسَ الْغِنَى عَنْ كَثْرَةِ الْعَرَضِ، وَلَكِنَّ الْغِنَى غِنَى النَّفْسِ",
            urdu = "مال داری سامان کی کثرت کا نام نہیں بلکہ اصل مال داری دل کا غنی ہونا ہے۔",
            english = "Wealth is not in having many possessions; rather, true wealth is the richness of the soul.",
            reference = "Sahih al-Bukhari 6446"
        )
    )

    var dailyHadith by mutableStateOf(offlineHadithPool.random())
        private set

    init {
        val savedJson = storage.loadRawJson()
        if (savedJson != null) {
            restoreFromJson(savedJson)
        } else {
            loadDefaultCategories()
            isDarkMode = false
            isHisabooDarkTheme = false
            persistState(triggerCloudSync = false)
        }

        isAppLocked = true
        liveCurrencyRates["PKR"] = usdToPkrRate

        fetchLiveUsdRate()
        fetchLiveHadith()

        val lastAccount = cloudManager.getLastSignedInAccount()
        if (lastAccount != null && !lastAccount.email.isNullOrBlank()) {
            connectedGoogleEmail = lastAccount.email
            if (transactions.isEmpty()) {
                autoRestoreFromCloudIfAvailable(lastAccount)
            }
        }
    }

    fun onAppOpenedOrUnlocked() {
        isAppLocked = false
        fetchLiveHadith()
    }

    fun fetchLiveHadith() {
        if (isFetchingHadith) return
        // Immediately rotate offline fallback so user sees a fresh Hadith right away
        val nextOffline = offlineHadithPool.filterNot { it.reference == dailyHadith.reference }
            .randomOrNull() ?: offlineHadithPool.random()
        dailyHadith = nextOffline

        viewModelScope.launch {
            isFetchingHadith = true
            try {
                // Curated authentic short-to-medium Hadith numbers from Sahih al-Bukhari
                val authenticNumbers = listOf(
                    1, 6, 8, 10, 13, 15, 24, 33, 52, 69,
                    528, 645, 1417, 1427, 1434, 1469, 2072, 2076,
                    2287, 2389, 2442, 2563, 2749, 6011, 6018, 6064, 6116, 6412, 6446, 6502
                )
                val chosenNum = authenticNumbers.filterNot { it == lastHadithNumber }.random()
                lastHadithNumber = chosenNum

                val liveResult = withContext(Dispatchers.IO) {
                    val urdDeferred = async {
                        fetchHttpText("https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/urd-bukhari/$chosenNum.min.json")
                    }
                    val engDeferred = async {
                        fetchHttpText("https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-bukhari/$chosenNum.min.json")
                    }
                    val araDeferred = async {
                        fetchHttpText("https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-bukhari/$chosenNum.min.json")
                    }

                    val urdJson = urdDeferred.await()
                    val engJson = engDeferred.await()
                    val araJson = araDeferred.await()

                    val urdText = extractHadithText(urdJson)
                    val engText = extractHadithText(engJson)
                    val araText = extractHadithText(araJson)

                    if (!urdText.isNullOrBlank() && !engText.isNullOrBlank()) {
                        DailyHadith(
                            arabic = araText?.takeIf { it.isNotBlank() } ?: urdText,
                            urdu = urdText,
                            english = engText,
                            reference = "Sahih al-Bukhari $chosenNum"
                        )
                    } else {
                        null
                    }
                }

                if (liveResult != null) {
                    dailyHadith = liveResult
                }
            } catch (_: Exception) {
                // Keep rotated offline authentic Hadith silently if offline
            } finally {
                isFetchingHadith = false
            }
        }
    }

    private fun fetchHttpText(urlString: String): String? {
        return try {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode == 200) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractHadithText(rawJson: String?): String? {
        if (rawJson.isNullOrBlank()) return null
        return try {
            val root = JSONObject(rawJson)
            val arr = root.optJSONArray("hadiths") ?: return null
            if (arr.length() == 0) return null
            arr.getJSONObject(0).optString("text", "").trim()
        } catch (_: Exception) {
            null
        }
    }

    fun fetchLiveUsdRate() {
        if (isFetchingRate) return
        viewModelScope.launch {
            isFetchingRate = true
            rateStatusText = "Updating live rate..."
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://open.er-api.com/v6/latest/USD")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 10000
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", "HisabooApp/1.0")
                    }
                    try {
                        if (conn.responseCode == 200) {
                            conn.inputStream.bufferedReader().use { it.readText() }
                        } else {
                            null
                        }
                    } finally {
                        conn.disconnect()
                    }
                }

                if (!result.isNullOrBlank()) {
                    val json = JSONObject(result)
                    val ratesObj = json.optJSONObject("rates")
                    if (ratesObj != null) {
                        val pkr = ratesObj.optDouble("PKR", -1.0)
                        if (pkr > 0) {
                            usdToPkrRate = pkr
                            liveCurrencyRates["PKR"] = pkr
                            persistState(triggerCloudSync = false)
                        }
                        val codes = listOf("USD", "EUR", "GBP", "SAR", "AED", "INR", "CAD", "AUD", "CNY", "TRY", "KWD")
                        for (code in codes) {
                            val valRate = ratesObj.optDouble(code, -1.0)
                            if (valRate > 0) {
                                liveCurrencyRates[code] = valRate
                            }
                        }
                        val time = SimpleDateFormat("hh:mm a, dd MMM", Locale.US).format(Date())
                        rateStatusText = "Live updated: $time"
                    } else {
                        rateStatusText = "Could not parse rate (offline?)"
                    }
                } else {
                    rateStatusText = "Network unavailable — using saved rate"
                }
            } catch (e: Exception) {
                rateStatusText = "Update failed — check internet"
            } finally {
                isFetchingRate = false
            }
        }
    }

    private fun loadDefaultCategories() {
        categories.clear()
        categories.addAll(
            listOf(
                CategoryItem("cat_business", "Business", TransactionType.CASH_IN, "business"),
                CategoryItem("cat_other_in", "Other Income", TransactionType.CASH_IN, "other_income"),
                CategoryItem("cat_salary", "Salary", TransactionType.CASH_IN, "salary"),
                CategoryItem("cat_bills", "Bills", TransactionType.CASH_OUT, "bills"),
                CategoryItem("cat_food", "Food", TransactionType.CASH_OUT, "food"),
                CategoryItem("cat_other_out", "Other Expense", TransactionType.CASH_OUT, "other_expense"),
                CategoryItem("cat_rent", "Rent", TransactionType.CASH_OUT, "rent"),
                CategoryItem("cat_shopping", "Shopping", TransactionType.CASH_OUT, "shopping"),
                CategoryItem("cat_transport", "Transport", TransactionType.CASH_OUT, "transport")
            )
        )
    }

    private fun persistState(triggerCloudSync: Boolean = true) {
        storage.saveAll(
            categories = categories.toList(),
            transactions = transactions.toList(),
            budgets = categoryBudgets.toMap(),
            plannedPayments = plannedPayments.toList(),
            reminders = reminders.toList(),
            loans = loans.toList(),
            utilityBills = utilityBills.toList(),
            familyMembers = familyMembers.toList(),
            inviteCode = inviteCode,
            fingerprintEnabled = fingerprintEnabled,
            appLanguage = appLanguage,
            isDarkMode = isDarkMode,
            currentPin = currentPin,
            lastBackupText = lastBackupText,
            usdToPkrRate = usdToPkrRate,
            connectedEmail = connectedGoogleEmail,
            autoCloudSyncEnabled = autoCloudSyncEnabled
        )

        if (triggerCloudSync && autoCloudSyncEnabled && !connectedGoogleEmail.isNullOrBlank()) {
            val account = cloudManager.getLastSignedInAccount() ?: return
            val jsonSnapshot = buildCurrentJsonString(pretty = false)
            viewModelScope.launch {
                val res = cloudManager.uploadBackupToCloud(account, jsonSnapshot)
                if (res.isSuccess) {
                    val time = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
                    lastBackupText = "$time (Gmail Cloud)"
                    storage.saveAll(
                        categories.toList(), transactions.toList(), categoryBudgets.toMap(),
                        plannedPayments.toList(), reminders.toList(), loans.toList(),
                        utilityBills.toList(), familyMembers.toList(), inviteCode,
                        fingerprintEnabled, appLanguage, isDarkMode, currentPin, lastBackupText,
                        usdToPkrRate, connectedGoogleEmail, autoCloudSyncEnabled
                    )
                }
            }
        }
    }

    private fun buildCurrentJsonString(pretty: Boolean = true): String {
        val obj = storage.serializeToJson(
            categories, transactions, categoryBudgets, plannedPayments,
            reminders, loans, utilityBills, familyMembers,
            inviteCode, fingerprintEnabled, appLanguage,
            currentPin, lastBackupText, usdToPkrRate
        )
        return if (pretty) obj.toString(2) else obj.toString()
    }

    private fun restoreFromJson(root: JSONObject) {
        try {
            val catArr = root.optJSONArray("categories")
            if (catArr != null && catArr.length() > 0) {
                categories.clear()
                for (i in 0 until catArr.length()) {
                    val o = catArr.getJSONObject(i)
                    categories.add(
                        CategoryItem(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            type = TransactionType.valueOf(o.getString("type")),
                            iconName = o.optString("iconName", "bills"),
                            isDefault = o.optBoolean("isDefault", true)
                        )
                    )
                }
            } else {
                loadDefaultCategories()
            }

            transactions.clear()
            val txArr = root.optJSONArray("transactions")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val o = txArr.getJSONObject(i)
                    transactions.add(
                        TransactionEntry(
                            id = o.getString("id"),
                            categoryId = o.getString("categoryId"),
                            categoryName = o.getString("categoryName"),
                            amount = o.getDouble("amount"),
                            note = o.getString("note"),
                            date = o.getString("date"),
                            type = TransactionType.valueOf(o.getString("type"))
                        )
                    )
                }
            }

            categoryBudgets.clear()
            val budgetObj = root.optJSONObject("budgets")
            if (budgetObj != null) {
                val keys = budgetObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    categoryBudgets[k] = budgetObj.getDouble(k)
                }
            }

            plannedPayments.clear()
            val planArr = root.optJSONArray("plannedPayments")
            if (planArr != null) {
                for (i in 0 until planArr.length()) {
                    val o = planArr.getJSONObject(i)
                    plannedPayments.add(
                        PlannedPaymentItem(
                            id = o.getString("id"),
                            amount = o.getDouble("amount"),
                            note = o.getString("note"),
                            dayOfMonth = o.getInt("dayOfMonth"),
                            categoryName = o.getString("categoryName")
                        )
                    )
                }
            }

            reminders.clear()
            val remArr = root.optJSONArray("reminders")
            if (remArr != null) {
                for (i in 0 until remArr.length()) {
                    val o = remArr.getJSONObject(i)
                    reminders.add(
                        ReminderItem(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            dueDate = o.getString("dueDate"),
                            isCompleted = o.optBoolean("isCompleted", false)
                        )
                    )
                }
            }

            loans.clear()
            val loanArr = root.optJSONArray("loans")
            if (loanArr != null) {
                for (i in 0 until loanArr.length()) {
                    val o = loanArr.getJSONObject(i)
                    val paymentList = mutableListOf<LoanPaymentRecord>()
                    val payArr = o.optJSONArray("payments")
                    if (payArr != null) {
                        for (j in 0 until payArr.length()) {
                            val po = payArr.getJSONObject(j)
                            paymentList.add(
                                LoanPaymentRecord(
                                    id = po.getString("id"),
                                    amount = po.getDouble("amount"),
                                    note = po.optString("note", ""),
                                    date = po.optString("date", ""),
                                    isAdditionalLoan = po.optBoolean("isAdditionalLoan", false)
                                )
                            )
                        }
                    }
                    loans.add(
                        LoanItem(
                            id = o.getString("id"),
                            person = o.getString("person"),
                            amount = o.getDouble("amount"),
                            isToReceive = o.getBoolean("isToReceive"),
                            payments = paymentList
                        )
                    )
                }
            }

            utilityBills.clear()
            val billArr = root.optJSONArray("utilityBills")
            if (billArr != null) {
                for (i in 0 until billArr.length()) {
                    val o = billArr.getJSONObject(i)
                    utilityBills.add(
                        UtilityBillItem(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            provider = o.getString("provider"),
                            amount = o.getDouble("amount"),
                            dueDate = o.getString("dueDate")
                        )
                    )
                }
            }

            familyMembers.clear()
            val famArr = root.optJSONArray("familyMembers")
            if (famArr != null) {
                for (i in 0 until famArr.length()) {
                    val o = famArr.getJSONObject(i)
                    familyMembers.add(
                        FamilyMemberItem(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            pin = o.getString("pin")
                        )
                    )
                }
            }

            val code = root.optString("inviteCode", "")
            inviteCode = code.ifBlank { null }
            fingerprintEnabled = root.optBoolean("fingerprintEnabled", false)
            appLanguage = root.optString("appLanguage", "English")
            isDarkMode = root.optBoolean("isDarkMode", false)
            isHisabooDarkTheme = isDarkMode
            currentPin = root.optString("currentPin", "1234")
            lastBackupText = root.optString("lastBackupText", "Never")
            usdToPkrRate = root.optDouble("usdToPkrRate", 278.50)

            val savedEmail = root.optString("connectedEmail", "")
            if (savedEmail.isNotBlank()) connectedGoogleEmail = savedEmail
            autoCloudSyncEnabled = root.optBoolean("autoCloudSyncEnabled", true)
        } catch (_: Exception) {
            if (categories.isEmpty()) loadDefaultCategories()
        }
    }

    fun onGoogleSignInSuccess(account: GoogleSignInAccount) {
        connectedGoogleEmail = account.email
        persistState(triggerCloudSync = false)
        autoRestoreFromCloudIfAvailable(account)
    }

    private fun autoRestoreFromCloudIfAvailable(account: GoogleSignInAccount) {
        viewModelScope.launch {
            isCloudSyncing = true
            cloudStatusBanner = "Checking Gmail cloud backup for ${account.email}..."
            val downloadResult = cloudManager.downloadBackupFromCloud(account)
            if (downloadResult.isSuccess) {
                val cloudJson = downloadResult.getOrNull()
                if (cloudJson != null) {
                    if (transactions.isEmpty()) {
                        restoreFromJson(cloudJson)
                        persistState(triggerCloudSync = false)
                        cloudStatusBanner = "Auto-restored ledger from ${account.email}"
                    } else {
                        cloudManager.uploadBackupToCloud(account, buildCurrentJsonString(false))
                        cloudStatusBanner = "Connected & synced with ${account.email}"
                    }
                }
            } else {
                val uploadRes = cloudManager.uploadBackupToCloud(account, buildCurrentJsonString(false))
                cloudStatusBanner = if (uploadRes.isSuccess) {
                    val time = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
                    lastBackupText = "$time (Gmail Cloud)"
                    persistState(triggerCloudSync = false)
                    "Connected! Auto-backup active for ${account.email}"
                } else {
                    "Connected ${account.email} (Offline / Local backup active)"
                }
            }
            isCloudSyncing = false
        }
    }

    fun manualCloudBackupNow() {
        val account = cloudManager.getLastSignedInAccount()
        if (account == null) {
            cloudStatusBanner = "Please connect your Gmail account first."
            return
        }
        viewModelScope.launch {
            isCloudSyncing = true
            val res = cloudManager.uploadBackupToCloud(account, buildCurrentJsonString(false))
            cloudStatusBanner = if (res.isSuccess) {
                val time = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
                lastBackupText = "$time (Gmail Cloud)"
                persistState(triggerCloudSync = false)
                "Backup uploaded to ${account.email}"
            } else {
                res.exceptionOrNull()?.message ?: "Cloud upload failed. Local backup saved."
            }
            isCloudSyncing = false
        }
    }

    fun manualCloudRestoreNow() {
        val account = cloudManager.getLastSignedInAccount()
        if (account == null) {
            cloudStatusBanner = "Please connect your Gmail account first."
            return
        }
        viewModelScope.launch {
            isCloudSyncing = true
            val res = cloudManager.downloadBackupFromCloud(account)
            cloudStatusBanner = if (res.isSuccess && res.getOrNull() != null) {
                restoreFromJson(res.getOrNull()!!)
                persistState(triggerCloudSync = false)
                "Restored all records from ${account.email}"
            } else {
                res.exceptionOrNull()?.message ?: "No cloud backup found."
            }
            isCloudSyncing = false
        }
    }

    fun disconnectGoogleAccount() {
        viewModelScope.launch {
            cloudManager.signOut()
            connectedGoogleEmail = null
            cloudStatusBanner = "Disconnected Gmail account. Offline auto-save active."
            persistState(triggerCloudSync = false)
        }
    }

    fun toggleAutoCloudSync(enabled: Boolean) {
        autoCloudSyncEnabled = enabled
        persistState(triggerCloudSync = enabled)
    }

    val totalCashIn: Double
        get() = transactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }

    val totalCashOut: Double
        get() = transactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }

    val balanceInHand: Double
        get() = totalCashIn - totalCashOut

    fun todayFormatted(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
    }

    fun addTransaction(
        categoryId: String,
        categoryName: String,
        amount: Double,
        note: String,
        date: String,
        type: TransactionType
    ) {
        if (amount <= 0) return
        transactions.add(
            0,
            TransactionEntry(
                id = UUID.randomUUID().toString(),
                categoryId = categoryId,
                categoryName = categoryName,
                amount = amount,
                note = note.ifBlank { categoryName },
                date = date.ifBlank { todayFormatted() },
                type = type
            )
        )
        persistState()
    }

    fun deleteTransaction(id: String) {
        transactions.removeAll { it.id == id }
        persistState()
    }

    fun addCategory(name: String, type: TransactionType) {
        if (name.isBlank()) return
        categories.add(
            CategoryItem(
                id = "cat_${UUID.randomUUID()}",
                name = name.trim(),
                type = type,
                iconName = name.trim().lowercase(),
                isDefault = false
            )
        )
        persistState()
    }

    fun deleteCategory(categoryId: String) {
        categories.removeAll { it.id == categoryId }
        transactions.removeAll { it.categoryId == categoryId }
        categoryBudgets.remove(categoryId)
        persistState()
    }

    fun setBudgetForCategory(categoryId: String, amount: Double) {
        if (amount > 0) {
            categoryBudgets[categoryId] = amount
        } else {
            categoryBudgets.remove(categoryId)
        }
        persistState()
    }

    fun addPlannedPayment(amount: Double, note: String, day: Int, categoryName: String) {
        if (amount <= 0) return
        plannedPayments.add(
            PlannedPaymentItem(
                id = UUID.randomUUID().toString(),
                amount = amount,
                note = note.ifBlank { "Scheduled Payment" },
                dayOfMonth = day.coerceIn(1, 28),
                categoryName = categoryName
            )
        )
        persistState()
    }

    fun addReminder(title: String, date: String) {
        if (title.isBlank()) return
        reminders.add(
            ReminderItem(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                dueDate = date.ifBlank { todayFormatted() }
            )
        )
        persistState()
    }

    fun addLoan(person: String, amount: Double, isToReceive: Boolean) {
        if (person.isBlank() || amount <= 0) return
        loans.add(
            0,
            LoanItem(
                id = UUID.randomUUID().toString(),
                person = person.trim(),
                amount = amount,
                isToReceive = isToReceive,
                payments = emptyList()
            )
        )
        persistState()
    }

    fun deleteLoan(loanId: String) {
        loans.removeAll { it.id == loanId }
        persistState()
    }

    fun addLoanPayment(
        loanId: String,
        amount: Double,
        note: String,
        date: String,
        isAdditionalLoan: Boolean = false
    ) {
        if (amount <= 0) return
        val idx = loans.indexOfFirst { it.id == loanId }
        if (idx == -1) return
        val existing = loans[idx]
        val isUrdu = appLanguage == "اردو"
        val defaultNote = if (isAdditionalLoan) {
            if (isUrdu) "مزید قرض شامل کیا" else "Additional loan added"
        } else if (existing.isToReceive) {
            if (isUrdu) "رقم وصول ہوئی" else "Payment received"
        } else {
            if (isUrdu) "رقم واپس کی" else "Payment returned"
        }
        val newRecord = LoanPaymentRecord(
            id = UUID.randomUUID().toString(),
            amount = amount,
            note = note.ifBlank { defaultNote },
            date = date.ifBlank { todayFormatted() },
            isAdditionalLoan = isAdditionalLoan
        )
        loans[idx] = existing.copy(payments = listOf(newRecord) + existing.payments)
        persistState()
    }

    fun deleteLoanPayment(loanId: String, paymentId: String) {
        val idx = loans.indexOfFirst { it.id == loanId }
        if (idx == -1) return
        val existing = loans[idx]
        loans[idx] = existing.copy(payments = existing.payments.filterNot { it.id == paymentId })
        persistState()
    }

    fun addUtilityBill(title: String, provider: String, amount: Double, dueDate: String) {
        if (title.isBlank() || amount <= 0) return
        utilityBills.add(
            UtilityBillItem(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                provider = provider,
                amount = amount,
                dueDate = dueDate.ifBlank { todayFormatted() }
            )
        )
        persistState()
    }

    fun generateInviteCode() {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        inviteCode = "HSB-" + (1..6).map { chars.random() }.joinToString("")
        persistState()
    }

    fun addFamilyMember(name: String, pin: String) {
        if (name.isBlank() || pin.isBlank()) return
        familyMembers.add(
            FamilyMemberItem(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                pin = pin.trim()
            )
        )
        persistState()
    }

    fun toggleFingerprint(enabled: Boolean) {
        fingerprintEnabled = enabled
        persistState()
    }

    fun toggleLanguage() {
        appLanguage = if (appLanguage == "English") "اردو" else "English"
        persistState()
    }

    fun toggleThemeMode() {
        isDarkMode = !isDarkMode
        isHisabooDarkTheme = isDarkMode
        persistState()
    }

    fun updatePin(newPin: String) {
        currentPin = newPin
        persistState()
    }

    fun updateUsdRate(newRate: Double) {
        if (newRate > 0) {
            usdToPkrRate = newRate
            liveCurrencyRates["PKR"] = newRate
            persistState()
        }
    }

    fun backupNow(): String {
        val json = buildCurrentJsonString(pretty = false)
        val time = storage.saveManualSnapshot(json)
        lastBackupText = time
        persistState(triggerCloudSync = true)
        return "Local backup saved ($time)"
    }

    fun downloadBackupFile(): String {
        return storage.exportBackupToDownloads(buildCurrentJsonString(pretty = true))
    }

    fun restoreSavedBackup(): String {
        val snap = storage.loadManualSnapshot() ?: return "No saved local backup found yet."
        restoreFromJson(snap.first)
        persistState(triggerCloudSync = false)
        return "Restored backup from ${snap.second}"
    }

    fun restoreFromDownloadsFile(): String {
        val imported = storage.importBackupFromDownloads()
            ?: return "No Hisaboo_Backup.json found in Downloads."
        restoreFromJson(imported)
        persistState(triggerCloudSync = false)
        return "Restored ledger from Hisaboo_Backup.json"
    }

    fun restoreFromUri(uri: Uri): String {
        val imported = storage.readJsonFromUri(uri)
            ?: return "Invalid backup file selected."
        restoreFromJson(imported)
        persistState(triggerCloudSync = false)
        return "Restored all records from selected backup file!"
    }

    fun createGmailShareIntent(): Intent {
        return storage.buildSendToGmailIntent(
            jsonString = buildCurrentJsonString(pretty = true),
            targetEmail = connectedGoogleEmail
        )
    }

    fun exportExcelCsv(): String {
        return storage.exportCsvToDownloads(transactions.toList())
    }
}

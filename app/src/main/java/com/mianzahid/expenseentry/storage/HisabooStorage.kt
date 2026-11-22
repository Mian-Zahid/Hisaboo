package com.mianzahid.expenseentry.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.mianzahid.expenseentry.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HisabooStorage(private val context: Context) {

    private val prefs = context.getSharedPreferences("hisaboo_permanent_ledger_v3", Context.MODE_PRIVATE)

    fun saveAll(
        categories: List<CategoryItem>,
        transactions: List<TransactionEntry>,
        budgets: Map<String, Double>,
        plannedPayments: List<PlannedPaymentItem>,
        reminders: List<ReminderItem>,
        loans: List<LoanItem>,
        utilityBills: List<UtilityBillItem>,
        familyMembers: List<FamilyMemberItem>,
        inviteCode: String?,
        fingerprintEnabled: Boolean,
        appLanguage: String,
        isDarkMode: Boolean,
        currentPin: String,
        lastBackupText: String,
        usdToPkrRate: Double,
        connectedEmail: String?,
        autoCloudSyncEnabled: Boolean
    ) {
        val root = serializeToJson(
            categories, transactions, budgets, plannedPayments,
            reminders, loans, utilityBills, familyMembers,
            inviteCode, fingerprintEnabled, appLanguage,
            currentPin, lastBackupText, usdToPkrRate
        )
        root.put("isDarkMode", isDarkMode)
        root.put("connectedEmail", connectedEmail ?: "")
        root.put("autoCloudSyncEnabled", autoCloudSyncEnabled)
        prefs.edit().putString("ledger_state_json", root.toString()).apply()
    }

    fun loadRawJson(): JSONObject? {
        val raw = prefs.getString("ledger_state_json", null) ?: return null
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun saveManualSnapshot(jsonString: String): String {
        val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        prefs.edit()
            .putString("manual_backup_snapshot", jsonString)
            .putString("manual_backup_time", timestamp)
            .apply()
        return timestamp
    }

    fun loadManualSnapshot(): Pair<JSONObject, String>? {
        val raw = prefs.getString("manual_backup_snapshot", null) ?: return null
        val time = prefs.getString("manual_backup_time", "Saved Snapshot") ?: "Saved Snapshot"
        return try {
            JSONObject(raw) to time
        } catch (_: Exception) {
            null
        }
    }

    fun exportBackupToDownloads(jsonString: String): String {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, "Hisaboo_Backup.json")
            file.writeText(jsonString)
            "Saved to Downloads/Hisaboo_Backup.json"
        } catch (_: Exception) {
            val internalFile = File(context.filesDir, "Hisaboo_Backup.json")
            internalFile.writeText(jsonString)
            "Saved to App Storage/Hisaboo_Backup.json"
        }
    }

    fun importBackupFromDownloads(): JSONObject? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val externalFile = File(downloadsDir, "Hisaboo_Backup.json")
            val internalFile = File(context.filesDir, "Hisaboo_Backup.json")
            val target = when {
                externalFile.exists() -> externalFile
                internalFile.exists() -> internalFile
                else -> return null
            }
            JSONObject(target.readText())
        } catch (_: Exception) {
            null
        }
    }

    fun readJsonFromUri(uri: Uri): JSONObject? {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return null
            JSONObject(text)
        } catch (_: Exception) {
            null
        }
    }

    fun buildSendToGmailIntent(jsonString: String, targetEmail: String?): Intent {
        val backupFile = File(context.filesDir, "Hisaboo_Backup.json")
        backupFile.writeText(jsonString)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val dateLabel = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            if (!targetEmail.isNullOrBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(targetEmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, "Hisaboo Ledger Backup ($dateLabel)")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is your encrypted Hisaboo backup file (Hisaboo_Backup.json).\n\nTo restore on any phone: Open Hisaboo -> Hub -> Backup / Export -> Restore from file -> Select Hisaboo_Backup.json."
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.android.gm")
        }
        return intent
    }

    fun exportCsvToDownloads(transactions: List<TransactionEntry>): String {
        val sb = StringBuilder()
        sb.append("Date,Type,Category,Note,Amount (PKR)\n")
        transactions.forEach { tx ->
            val safeNote = tx.note.replace(",", " ")
            val safeCat = tx.categoryName.replace(",", " ")
            sb.append("${tx.date},${tx.type.name},$safeCat,$safeNote,${tx.amount}\n")
        }
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, "Hisaboo_Ledger.csv")
            file.writeText(sb.toString())
            "Exported to Downloads/Hisaboo_Ledger.csv"
        } catch (_: Exception) {
            val internalFile = File(context.filesDir, "Hisaboo_Ledger.csv")
            internalFile.writeText(sb.toString())
            "Exported to App Storage/Hisaboo_Ledger.csv"
        }
    }

    fun serializeToJson(
        categories: List<CategoryItem>,
        transactions: List<TransactionEntry>,
        budgets: Map<String, Double>,
        plannedPayments: List<PlannedPaymentItem>,
        reminders: List<ReminderItem>,
        loans: List<LoanItem>,
        utilityBills: List<UtilityBillItem>,
        familyMembers: List<FamilyMemberItem>,
        inviteCode: String?,
        fingerprintEnabled: Boolean,
        appLanguage: String,
        currentPin: String,
        lastBackupText: String,
        usdToPkrRate: Double
    ): JSONObject {
        val root = JSONObject()

        val catArr = JSONArray()
        categories.forEach { c ->
            catArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("type", c.type.name)
                put("iconName", c.iconName)
                put("isDefault", c.isDefault)
            })
        }
        root.put("categories", catArr)

        val txArr = JSONArray()
        transactions.forEach { t ->
            txArr.put(JSONObject().apply {
                put("id", t.id)
                put("categoryId", t.categoryId)
                put("categoryName", t.categoryName)
                put("amount", t.amount)
                put("note", t.note)
                put("date", t.date)
                put("type", t.type.name)
            })
        }
        root.put("transactions", txArr)

        val budgetObj = JSONObject()
        budgets.forEach { (k, v) -> budgetObj.put(k, v) }
        root.put("budgets", budgetObj)

        val planArr = JSONArray()
        plannedPayments.forEach { p ->
            planArr.put(JSONObject().apply {
                put("id", p.id)
                put("amount", p.amount)
                put("note", p.note)
                put("dayOfMonth", p.dayOfMonth)
                put("categoryName", p.categoryName)
            })
        }
        root.put("plannedPayments", planArr)

        val remArr = JSONArray()
        reminders.forEach { r ->
            remArr.put(JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("dueDate", r.dueDate)
                put("isCompleted", r.isCompleted)
            })
        }
        root.put("reminders", remArr)

        val loanArr = JSONArray()
        loans.forEach { l ->
            val paymentsArr = JSONArray()
            l.payments.forEach { pay ->
                paymentsArr.put(JSONObject().apply {
                    put("id", pay.id)
                    put("amount", pay.amount)
                    put("note", pay.note)
                    put("date", pay.date)
                    put("isAdditionalLoan", pay.isAdditionalLoan)
                })
            }
            loanArr.put(JSONObject().apply {
                put("id", l.id)
                put("person", l.person)
                put("amount", l.amount)
                put("isToReceive", l.isToReceive)
                put("payments", paymentsArr)
            })
        }
        root.put("loans", loanArr)

        val billArr = JSONArray()
        utilityBills.forEach { b ->
            billArr.put(JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("provider", b.provider)
                put("amount", b.amount)
                put("dueDate", b.dueDate)
            })
        }
        root.put("utilityBills", billArr)

        val famArr = JSONArray()
        familyMembers.forEach { f ->
            famArr.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("pin", f.pin)
            })
        }
        root.put("familyMembers", famArr)

        root.put("inviteCode", inviteCode ?: "")
        root.put("fingerprintEnabled", fingerprintEnabled)
        root.put("appLanguage", appLanguage)
        root.put("currentPin", currentPin)
        root.put("lastBackupText", lastBackupText)
        root.put("usdToPkrRate", usdToPkrRate)

        return root
    }
}

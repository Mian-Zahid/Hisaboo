package com.mianzahid.expenseentry

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Category(
    val id: Int = 0,
    val name: String,
    val type: String, // "IN" or "OUT"
    val color: Long = 0xFF0B5E40,
    val isDefault: Boolean = false
)

data class Entry(
    val id: Int = 0,
    val categoryId: Int,
    val amount: Long,
    val type: String, // "IN" or "OUT"
    val note: String = "",
    val dateMillis: Long,
    val memberId: Int = 0, // 0 = Admin
    val memberName: String = "Admin",
    val receiptUri: String = ""
)

data class Member(
    val id: Int = 0,
    val name: String,
    val pinHash: String,
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Invite(
    val id: Int = 0,
    val code: String,
    val createdAt: Long,
    val expiresAt: Long,
    val status: String = "active", // active, used, revoked, expired
    val usedBy: String = ""
)

data class Budget(
    val id: Int = 0,
    val categoryId: Int,
    val amount: Long
)

data class Udhar(
    val id: Int = 0,
    val personName: String,
    val amount: Long,
    val kind: String, // "lend" = you gave, "borrow" = you took
    val dateMillis: Long,
    val dueMillis: Long = 0,
    val status: String = "pending" // pending / paid
)

data class Bill(
    val id: Int = 0,
    val name: String,
    val provider: String,
    val amount: Long,
    val dueMillis: Long,
    val status: String = "pending", // pending / paid / overdue
    val photoUri: String = ""
)

data class Recurring(
    val id: Int = 0,
    val categoryId: Int,
    val amount: Long,
    val note: String = "",
    val dayOfMonth: Int,
    val lastAppliedMillis: Long = 0
)

data class Reminder(
    val id: Int = 0,
    val title: String,
    val dateMillis: Long,
    val done: Boolean = false
)

data class Setting(
    val key: String,
    val value: String
)

private class DbHelper(ctx: Context) : SQLiteOpenHelper(ctx, "expense.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, type TEXT, color INTEGER, isDefault INTEGER)")
        db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, categoryId INTEGER, amount INTEGER, type TEXT, note TEXT, dateMillis INTEGER, memberId INTEGER, memberName TEXT, receiptUri TEXT)")
        db.execSQL("CREATE TABLE members (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, pinHash TEXT, isAdmin INTEGER, createdAt INTEGER)")
        db.execSQL("CREATE TABLE invites (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT, createdAt INTEGER, expiresAt INTEGER, status TEXT, usedBy TEXT)")
        db.execSQL("CREATE TABLE budgets (id INTEGER PRIMARY KEY AUTOINCREMENT, categoryId INTEGER UNIQUE, amount INTEGER)")
        db.execSQL("CREATE TABLE udhar (id INTEGER PRIMARY KEY AUTOINCREMENT, personName TEXT, amount INTEGER, kind TEXT, dateMillis INTEGER, dueMillis INTEGER, status TEXT)")
        db.execSQL("CREATE TABLE bills (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, provider TEXT, amount INTEGER, dueMillis INTEGER, status TEXT, photoUri TEXT)")
        db.execSQL("CREATE TABLE recurring (id INTEGER PRIMARY KEY AUTOINCREMENT, categoryId INTEGER, amount INTEGER, note TEXT, dayOfMonth INTEGER, lastAppliedMillis INTEGER)")
        db.execSQL("CREATE TABLE reminders (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, dateMillis INTEGER, done INTEGER)")
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
}

class AppDao(ctx: Context) {
    private val helper = DbHelper(ctx.applicationContext)

    private suspend fun <T> io(block: (SQLiteDatabase) -> T): T = withContext(Dispatchers.IO) { block(helper.writableDatabase) }
    private suspend fun <T> ro(block: (SQLiteDatabase) -> T): T = withContext(Dispatchers.IO) { block(helper.readableDatabase) }

    private fun cv(vararg pairs: Pair<String, Any?>): ContentValues {
        val c = ContentValues()
        for ((k, v) in pairs) {
            when (v) {
                null -> c.putNull(k)
                is String -> c.put(k, v)
                is Int -> c.put(k, v)
                is Long -> c.put(k, v)
                is Boolean -> c.put(k, if (v) 1 else 0)
                else -> c.put(k, v.toString())
            }
        }
        return c
    }

    suspend fun allCategories(): List<Category> = ro { db ->
        val list = mutableListOf<Category>()
        db.rawQuery("SELECT * FROM categories ORDER BY type DESC, name ASC", null).use { cur ->
            while (cur.moveToNext()) list.add(Category(cur.getInt(0), cur.getString(1), cur.getString(2), cur.getLong(3), cur.getInt(4) == 1))
        }
        list
    }

    suspend fun insertCategory(c: Category): Long = io { db ->
        db.insert("categories", null, cv("name" to c.name, "type" to c.type, "color" to c.color, "isDefault" to c.isDefault))
    }

    suspend fun updateCategory(c: Category) {
        io { db -> db.update("categories", cv("name" to c.name, "type" to c.type, "color" to c.color, "isDefault" to c.isDefault), "id=?", arrayOf(c.id.toString())) }
    }

    suspend fun deleteCategory(id: Int) { io { db -> db.delete("categories", "id=?", arrayOf(id.toString())) } }

    suspend fun categoryById(id: Int): Category? = ro { db ->
        db.rawQuery("SELECT * FROM categories WHERE id=?", arrayOf(id.toString())).use { cur ->
            if (cur.moveToFirst()) Category(cur.getInt(0), cur.getString(1), cur.getString(2), cur.getLong(3), cur.getInt(4) == 1) else null
        }
    }

    suspend fun allEntries(): List<Entry> = ro { db ->
        val list = mutableListOf<Entry>()
        db.rawQuery("SELECT * FROM entries ORDER BY dateMillis DESC", null).use { cur ->
            while (cur.moveToNext()) list.add(Entry(cur.getInt(0), cur.getInt(1), cur.getLong(2), cur.getString(3), cur.getString(4) ?: "", cur.getLong(5), cur.getInt(6), cur.getString(7) ?: "Admin", cur.getString(8) ?: ""))
        }
        list
    }

    suspend fun entriesForCategory(catId: Int): List<Entry> = allEntries().filter { it.categoryId == catId }

    suspend fun entriesForMember(memberId: Int): List<Entry> = allEntries().filter { it.memberId == memberId }

    suspend fun insertEntry(e: Entry): Long = io { db ->
        db.insert("entries", null, cv("categoryId" to e.categoryId, "amount" to e.amount, "type" to e.type, "note" to e.note, "dateMillis" to e.dateMillis, "memberId" to e.memberId, "memberName" to e.memberName, "receiptUri" to e.receiptUri))
    }

    suspend fun updateEntry(e: Entry) {
        io { db -> db.update("entries", cv("categoryId" to e.categoryId, "amount" to e.amount, "type" to e.type, "note" to e.note, "dateMillis" to e.dateMillis, "memberId" to e.memberId, "memberName" to e.memberName, "receiptUri" to e.receiptUri), "id=?", arrayOf(e.id.toString())) }
    }

    suspend fun deleteEntry(id: Int) { io { db -> db.delete("entries", "id=?", arrayOf(id.toString())) } }

    suspend fun allMembers(): List<Member> = ro { db ->
        val list = mutableListOf<Member>()
        db.rawQuery("SELECT * FROM members ORDER BY id ASC", null).use { cur ->
            while (cur.moveToNext()) list.add(Member(cur.getInt(0), cur.getString(1), cur.getString(2), cur.getInt(3) == 1, cur.getLong(4)))
        }
        list
    }

    suspend fun insertMember(m: Member): Long = io { db ->
        db.insert("members", null, cv("name" to m.name, "pinHash" to m.pinHash, "isAdmin" to m.isAdmin, "createdAt" to m.createdAt))
    }

    suspend fun deleteMember(id: Int) { io { db -> db.delete("members", "id=?", arrayOf(id.toString())) } }

    suspend fun memberById(id: Int): Member? = allMembers().firstOrNull { it.id == id }

    suspend fun allInvites(): List<Invite> = ro { db ->
        val list = mutableListOf<Invite>()
        db.rawQuery("SELECT * FROM invites ORDER BY createdAt DESC", null).use { cur ->
            while (cur.moveToNext()) list.add(Invite(cur.getInt(0), cur.getString(1), cur.getLong(2), cur.getLong(3), cur.getString(4), cur.getString(5) ?: ""))
        }
        list
    }

    suspend fun insertInvite(i: Invite): Long = io { db ->
        db.insert("invites", null, cv("code" to i.code, "createdAt" to i.createdAt, "expiresAt" to i.expiresAt, "status" to i.status, "usedBy" to i.usedBy))
    }

    suspend fun updateInvite(i: Invite) {
        io { db -> db.update("invites", cv("code" to i.code, "createdAt" to i.createdAt, "expiresAt" to i.expiresAt, "status" to i.status, "usedBy" to i.usedBy), "id=?", arrayOf(i.id.toString())) }
    }

    suspend fun inviteByCode(code: String): Invite? = allInvites().firstOrNull { it.code.equals(code, ignoreCase = true) }

    suspend fun allBudgets(): List<Budget> = ro { db ->
        val list = mutableListOf<Budget>()
        db.rawQuery("SELECT * FROM budgets", null).use { cur ->
            while (cur.moveToNext()) list.add(Budget(cur.getInt(0), cur.getInt(1), cur.getLong(2)))
        }
        list
    }

    suspend fun upsertBudget(b: Budget) {
        io { db ->
            val rows = db.update("budgets", cv("amount" to b.amount), "categoryId=?", arrayOf(b.categoryId.toString()))
            if (rows == 0) db.insert("budgets", null, cv("categoryId" to b.categoryId, "amount" to b.amount))
        }
    }

    suspend fun deleteBudget(catId: Int) { io { db -> db.delete("budgets", "categoryId=?", arrayOf(catId.toString())) } }

    suspend fun budgetFor(catId: Int): Budget? = allBudgets().firstOrNull { it.categoryId == catId }

    suspend fun allUdhar(): List<Udhar> = ro { db ->
        val list = mutableListOf<Udhar>()
        db.rawQuery("SELECT * FROM udhar ORDER BY dateMillis DESC", null).use { cur ->
            while (cur.moveToNext()) list.add(Udhar(cur.getInt(0), cur.getString(1), cur.getLong(2), cur.getString(3), cur.getLong(4), cur.getLong(5), cur.getString(6)))
        }
        list
    }

    suspend fun insertUdhar(u: Udhar): Long = io { db ->
        db.insert("udhar", null, cv("personName" to u.personName, "amount" to u.amount, "kind" to u.kind, "dateMillis" to u.dateMillis, "dueMillis" to u.dueMillis, "status" to u.status))
    }

    suspend fun updateUdhar(u: Udhar) {
        io { db -> db.update("udhar", cv("personName" to u.personName, "amount" to u.amount, "kind" to u.kind, "dateMillis" to u.dateMillis, "dueMillis" to u.dueMillis, "status" to u.status), "id=?", arrayOf(u.id.toString())) }
    }

    suspend fun deleteUdhar(id: Int) { io { db -> db.delete("udhar", "id=?", arrayOf(id.toString())) } }

    suspend fun allBills(): List<Bill> = ro { db ->
        val list = mutableListOf<Bill>()
        db.rawQuery("SELECT * FROM bills ORDER BY dueMillis ASC", null).use { cur ->
            while (cur.moveToNext()) list.add(Bill(cur.getInt(0), cur.getString(1), cur.getString(2), cur.getLong(3), cur.getLong(4), cur.getString(5), cur.getString(6) ?: ""))
        }
        list
    }

    suspend fun insertBill(b: Bill): Long = io { db ->
        db.insert("bills", null, cv("name" to b.name, "provider" to b.provider, "amount" to b.amount, "dueMillis" to b.dueMillis, "status" to b.status, "photoUri" to b.photoUri))
    }

    suspend fun updateBill(b: Bill) {
        io { db -> db.update("bills", cv("name" to b.name, "provider" to b.provider, "amount" to b.amount, "dueMillis" to b.dueMillis, "status" to b.status, "photoUri" to b.photoUri), "id=?", arrayOf(b.id.toString())) }
    }

    suspend fun deleteBill(id: Int) { io { db -> db.delete("bills", "id=?", arrayOf(id.toString())) } }

    suspend fun allRecurring(): List<Recurring> = ro { db ->
        val list = mutableListOf<Recurring>()
        db.rawQuery("SELECT * FROM recurring ORDER BY dayOfMonth ASC", null).use { cur ->
            while (cur.moveToNext()) list.add(Recurring(cur.getInt(0), cur.getInt(1), cur.getLong(2), cur.getString(3) ?: "", cur.getInt(4), cur.getLong(5)))
        }
        list
    }

    suspend fun insertRecurring(r: Recurring): Long = io { db ->
        db.insert("recurring", null, cv("categoryId" to r.categoryId, "amount" to r.amount, "note" to r.note, "dayOfMonth" to r.dayOfMonth, "lastAppliedMillis" to r.lastAppliedMillis))
    }

    suspend fun updateRecurring(r: Recurring) {
        io { db -> db.update("recurring", cv("categoryId" to r.categoryId, "amount" to r.amount, "note" to r.note, "dayOfMonth" to r.dayOfMonth, "lastAppliedMillis" to r.lastAppliedMillis), "id=?", arrayOf(r.id.toString())) }
    }

    suspend fun deleteRecurring(id: Int) { io { db -> db.delete("recurring", "id=?", arrayOf(id.toString())) } }

    suspend fun allReminders(): List<Reminder> = ro { db ->
        val list = mutableListOf<Reminder>()
        db.rawQuery("SELECT * FROM reminders ORDER BY dateMillis ASC", null).use { cur ->
            while (cur.moveToNext()) list.add(Reminder(cur.getInt(0), cur.getString(1), cur.getLong(2), cur.getInt(3) == 1))
        }
        list
    }

    suspend fun insertReminder(r: Reminder): Long = io { db ->
        db.insert("reminders", null, cv("title" to r.title, "dateMillis" to r.dateMillis, "done" to r.done))
    }

    suspend fun updateReminder(r: Reminder) {
        io { db -> db.update("reminders", cv("title" to r.title, "dateMillis" to r.dateMillis, "done" to r.done), "id=?", arrayOf(r.id.toString())) }
    }

    suspend fun deleteReminder(id: Int) { io { db -> db.delete("reminders", "id=?", arrayOf(id.toString())) } }

    suspend fun setting(key: String): Setting? = ro { db ->
        db.rawQuery("SELECT * FROM settings WHERE key=?", arrayOf(key)).use { cur ->
            if (cur.moveToFirst()) Setting(cur.getString(0), cur.getString(1)) else null
        }
    }

    suspend fun putSetting(s: Setting) {
        io { db ->
            val rows = db.update("settings", cv("value" to s.value), "key=?", arrayOf(s.key))
            if (rows == 0) db.insert("settings", null, cv("key" to s.key, "value" to s.value))
        }
    }

    suspend fun allSettings(): List<Setting> = ro { db ->
        val list = mutableListOf<Setting>()
        db.rawQuery("SELECT * FROM settings", null).use { cur ->
            while (cur.moveToNext()) list.add(Setting(cur.getString(0), cur.getString(1)))
        }
        list
    }

    suspend fun clearEntries() { io { db -> db.delete("entries", null, null) } }
    suspend fun clearCategories() { io { db -> db.delete("categories", null, null) } }
    suspend fun clearBudgets() { io { db -> db.delete("budgets", null, null) } }
    suspend fun clearUdhar() { io { db -> db.delete("udhar", null, null) } }
    suspend fun clearBills() { io { db -> db.delete("bills", null, null) } }
    suspend fun clearRecurring() { io { db -> db.delete("recurring", null, null) } }
    suspend fun clearReminders() { io { db -> db.delete("reminders", null, null) } }
}

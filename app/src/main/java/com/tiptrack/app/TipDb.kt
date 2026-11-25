package com.tiptrack.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Tip(
    val id: Long, val amount: Long, val name: String, val phone: String,
    val note: String, val createdAt: Long, val settled: Boolean
)

class TipDb(context: Context) : SQLiteOpenHelper(context, "tiptrack.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE tips(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            amount INTEGER NOT NULL,
            name TEXT NOT NULL DEFAULT '',
            phone TEXT NOT NULL DEFAULT '',
            note TEXT NOT NULL DEFAULT '',
            created_at INTEGER NOT NULL,
            settled INTEGER NOT NULL DEFAULT 0
        )""")
        db.execSQL("""CREATE TABLE settlements(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            amount INTEGER NOT NULL,
            count INTEGER NOT NULL,
            settled_at INTEGER NOT NULL,
            note TEXT NOT NULL DEFAULT ''
        )""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun addTip(amount: Long, name: String, phone: String, note: String, date: Long) {
        val v = ContentValues().apply {
            put("amount", amount); put("name", name); put("phone", phone)
            put("note", note); put("created_at", date); put("settled", 0)
        }
        writableDatabase.insert("tips", null, v)
    }

    fun updateTip(id: Long, amount: Long, name: String, phone: String, note: String, date: Long) {
        val v = ContentValues().apply {
            put("amount", amount); put("name", name); put("phone", phone)
            put("note", note); put("created_at", date)
        }
        writableDatabase.update("tips", v, "id=?", arrayOf(id.toString()))
    }

    fun deleteTip(id: Long) { writableDatabase.delete("tips", "id=?", arrayOf(id.toString())) }

    fun allTips(): List<Tip> = readTips("SELECT * FROM tips ORDER BY created_at DESC", null)
    fun tipsBetween(start: Long, end: Long): List<Tip> =
        readTips("SELECT * FROM tips WHERE created_at>=? AND created_at<? ORDER BY created_at DESC",
            arrayOf(start.toString(), end.toString()))

    fun unsettledBetween(start: Long, end: Long): List<Tip> =
        readTips("SELECT * FROM tips WHERE settled=0 AND created_at>=? AND created_at<? ORDER BY created_at",
            arrayOf(start.toString(), end.toString()))

    private fun readTips(sql: String, args: Array<String>?): List<Tip> {
        val result = mutableListOf<Tip>()
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) result.add(Tip(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getLong(c.getColumnIndexOrThrow("amount")),
                c.getString(c.getColumnIndexOrThrow("name")) ?: "",
                c.getString(c.getColumnIndexOrThrow("phone")) ?: "",
                c.getString(c.getColumnIndexOrThrow("note")) ?: "",
                c.getLong(c.getColumnIndexOrThrow("created_at")),
                c.getInt(c.getColumnIndexOrThrow("settled")) == 1
            ))
        }
        return result
    }

    fun settle(start: Long, end: Long, note: String): Pair<Long, Int> {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val pending = unsettledBetween(start, end)
            val total = pending.sumOf { it.amount }
            if (pending.isNotEmpty()) {
                val v = ContentValues().apply { put("settled", 1) }
                db.update("tips", v, "settled=0 AND created_at>=? AND created_at<?",
                    arrayOf(start.toString(), end.toString()))
                val s = ContentValues().apply {
                    put("amount", total); put("count", pending.size)
                    put("settled_at", System.currentTimeMillis()); put("note", note)
                }
                db.insert("settlements", null, s)
            }
            db.setTransactionSuccessful()
            return Pair(total, pending.size)
        } finally { db.endTransaction() }
    }
}

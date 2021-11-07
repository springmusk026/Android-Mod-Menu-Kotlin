package uk.lgl.modmenu

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.content.SharedPreferences
import java.lang.ClassCastException
import java.util.LinkedHashSet

class Preferences {
    private constructor(context: Context?) {
        sharedPreferences = context!!.applicationContext.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
    }

    private constructor(context: Context, preferencesName: String) {
        sharedPreferences = context.applicationContext.getSharedPreferences(
            preferencesName,
            Context.MODE_PRIVATE
        )
    }

    fun readString(what: String?): String? {
        return sharedPreferences.getString(what, DEFAULT_STRING_VALUE)
    }

    fun readString(what: Int): String? {
        return try {
            sharedPreferences.getString(what.toString(), DEFAULT_STRING_VALUE)
        } catch (ex: ClassCastException) {
            ""
        }
    }

    fun readString(what: String?, defaultString: String?): String? {
        return sharedPreferences.getString(what, defaultString)
    }

    fun writeString(where: String?, what: String?) {
        sharedPreferences.edit().putString(where, what).apply()
    }

    fun writeString(where: Int, what: String?) {
        sharedPreferences.edit().putString(where.toString(), what).apply()
    }

    fun readInt(what: String?): Int {
        return sharedPreferences.getInt(what, DEFAULT_INT_VALUE)
    }

    fun readInt(what: Int): Int {
        return try {
            sharedPreferences.getInt(what.toString(), DEFAULT_INT_VALUE)
        } catch (ex: ClassCastException) {
            0
        }
    }

    fun readInt(what: String?, defaultInt: Int): Int {
        return sharedPreferences.getInt(what, defaultInt)
    }

    fun writeInt(where: String?, what: Int) {
        sharedPreferences.edit().putInt(where, what).apply()
    }

    fun writeInt(where: Int, what: Int) {
        sharedPreferences.edit().putInt(where.toString(), what).apply()
    }

    fun readDouble(what: String?): Double {
        return if (!contains(what)) DEFAULT_DOUBLE_VALUE else java.lang.Double.longBitsToDouble(
            readLong(what)
        )
    }


    fun readDouble(what: String?, defaultDouble: Double): Double {
        return if (!contains(what)) defaultDouble else java.lang.Double.longBitsToDouble(
            readLong(
                what
            )
        )
    }

    fun writeDouble(where: String?, what: Double) {
        writeLong(where, java.lang.Double.doubleToRawLongBits(what))
    }

    fun readFloat(what: String?): Float {
        return sharedPreferences.getFloat(what, DEFAULT_FLOAT_VALUE)
    }

    fun readFloat(what: String?, defaultFloat: Float): Float {
        return sharedPreferences.getFloat(what, defaultFloat)
    }

    fun writeFloat(where: String?, what: Float) {
        sharedPreferences.edit().putFloat(where, what).apply()
    }

    fun readLong(what: String?): Long {
        return sharedPreferences.getLong(what, DEFAULT_LONG_VALUE)
    }

    fun readLong(what: String?, defaultLong: Long): Long {
        return sharedPreferences.getLong(what, defaultLong)
    }

    fun writeLong(where: String?, what: Long) {
        sharedPreferences.edit().putLong(where, what).apply()
    }

    fun readBoolean(what: String?): Boolean {
        return sharedPreferences.getBoolean(what, DEFAULT_BOOLEAN_VALUE)
    }

    fun readBoolean(what: Int): Boolean {
        return sharedPreferences.getBoolean(what.toString(), DEFAULT_BOOLEAN_VALUE)
    }

    fun readBoolean(what: String?, defaultBoolean: Boolean): Boolean {
      return sharedPreferences.getBoolean(what, defaultBoolean)
    }

    fun readBoolean(what: Int, defaultBoolean: Boolean): Boolean {
         return try {
            sharedPreferences.getBoolean(what.toString(), defaultBoolean)
        } catch (ex: ClassCastException) {
            defaultBoolean
        }
    }

    fun writeBoolean(where: String?, what: Boolean) {
        sharedPreferences.edit().putBoolean(where, what).apply()
    }

    fun writeBoolean(where: Int, what: Boolean) {
        sharedPreferences.edit().putBoolean(where.toString(), what).apply()
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun putStringSet(key: String, value: Set<String?>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            sharedPreferences.edit().putStringSet(key, value).apply()
        } else {
            putOrderedStringSet(key, value)
        }
    }

    fun putOrderedStringSet(key: String, value: Set<String?>) {
        var stringSetLength = 0
        if (sharedPreferences.contains(key + LENGTH)) {
            stringSetLength = readInt(key + LENGTH)
        }
        writeInt(key + LENGTH, value.size)
        var i = 0
        for (aValue in value) {
            writeString("$key[$i]", aValue)
            i++
        }
        while (i < stringSetLength) {

            remove("$key[$i]")
            i++
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun getStringSet(key: String, defValue: Set<String?>?): Set<String?>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            sharedPreferences.getStringSet(key, defValue)
        } else {
           getOrderedStringSet(key, defValue)
        }
    }

    fun getOrderedStringSet(key: String, defValue: Set<String?>?): Set<String?>? {
        if (contains(key + LENGTH)) {
            val set = LinkedHashSet<String?>()
            val stringSetLength = readInt(key + LENGTH)
            if (stringSetLength >= 0) {
                for (i in 0 until stringSetLength) {
                    set.add(readString("$key[$i]"))
                }
            }
            return set
        }
        return defValue
    }

    fun remove(key: String) {
        if (contains(key + LENGTH)) {
            val stringSetLength = readInt(key + LENGTH)
            if (stringSetLength >= 0) {
                sharedPreferences.edit().remove(key + LENGTH).apply()
                for (i in 0 until stringSetLength) {
                    sharedPreferences.edit().remove("$key[$i]").apply()
                }
            }
        }
        sharedPreferences.edit().remove(key).apply()
    }

    operator fun contains(key: String?): Boolean {
        return sharedPreferences.contains(key)
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private lateinit var sharedPreferences: SharedPreferences
        private var prefsInstance: Preferences? = null
        var context: Context? = null
        var loadPref = false
        var isExpanded = false
        private const val LENGTH = "_length"
        private const val DEFAULT_STRING_VALUE = ""
        private const val DEFAULT_INT_VALUE = 0 //-1
        private const val DEFAULT_DOUBLE_VALUE = 0.0 //-1d
        private const val DEFAULT_FLOAT_VALUE = 0f //-1f
        private const val DEFAULT_LONG_VALUE = 0L //-1L
        private const val DEFAULT_BOOLEAN_VALUE = false




        fun with(context: Context?): Preferences? {
            if (prefsInstance == null) {
                prefsInstance = Preferences(context)
            }
            return prefsInstance
        }

        fun with(context: Context?, forceInstantiation: Boolean): Preferences? {
            if (forceInstantiation) {
                prefsInstance = Preferences(context)
            }
            return prefsInstance
        }

        fun with(context: Context, preferencesName: String): Preferences? {
            if (prefsInstance == null) {
                prefsInstance = Preferences(context, preferencesName)
            }
            return prefsInstance
        }

        fun with(
            context: Context, preferencesName: String,
            forceInstantiation: Boolean
        ): Preferences? {
            if (forceInstantiation) {
                prefsInstance = Preferences(context, preferencesName)
            }
            return prefsInstance
        }
    }
}
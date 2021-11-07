package uk.lgl.modmenu

import android.content.Context
import android.widget.Toast
import android.os.Build
import android.util.Log
import java.io.*
import java.lang.StringBuilder

object Logcat {
    fun Clear(context: Context?) {
        try {
            Runtime.getRuntime().exec("logcat -c")
            Toast.makeText(context, "Logcat cleared", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(context, "There was an error saving logcat to file", Toast.LENGTH_LONG)
                .show()
            e.printStackTrace()
        }
    }

    fun Save(context: Context) {
        var path: File? = null
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            val log = StringBuilder()
            var line = ""
            while (bufferedReader.readLine().also { line = it } != null) {
                log.append(
                    """
    $line
    
    """.trimIndent()
                )
            }
            val unixTime = System.currentTimeMillis() / 1000L
            path =
                if (Build.VERSION.SDK_INT >= 30) { //Android R. AIDE didn't support Build.VERSION_CODES.R
                    File("/storage/emulated/0/Documents/")
                } else {
                    File(context.getExternalFilesDir(null).toString() + "/Mod Menu")
                }
            val folder = File(path.toString())
            folder.mkdirs()
            val file = File(path.toString() + "/Mod menu log - " + context.packageName + ".txt")
            file.createNewFile()
            try {
                //BufferedWriter for performance, true to set append to file flag
                val buf = BufferedWriter(FileWriter(file))
                buf.append(log.toString())
                buf.newLine()
                buf.close()
                Toast.makeText(context, "Logcat saved successfully to: $file", Toast.LENGTH_LONG)
                    .show()
                Toast.makeText(context, "Logcat saved successfully to: $file", Toast.LENGTH_LONG)
                    .show()
            } catch (e: IOException) {
                Toast.makeText(
                    context,
                    "There was an error saving logcat to file: " + e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        } catch (e: IOException) {
            Toast.makeText(
                context,
                "There was an error saving logcat to file: " + Log.getStackTraceString(e),
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }
}
package uk.lgl

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import uk.lgl.modmenu.FloatingModMenuService
import android.os.Bundle
import android.os.Handler
import android.provider.Settings

class MainActivity : Activity() {
    var GameActivity = "com.unity3d.player.UnityPlayerActivity"
    var hasLaunched = false

    companion object {
        fun Start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Toast.makeText(
                    context.applicationContext,
                    "Overlay permission is required in order to show mod menu. Restart the game after you allow permission",
                    Toast.LENGTH_LONG
                ).show()
                Toast.makeText(
                    context.applicationContext,
                    "Overlay permission is required in order to show mod menu. Restart the game after you allow permission",
                    Toast.LENGTH_LONG
                ).show()
                context.startActivity(
                    Intent(
                        "android.settings.action.MANAGE_OVERLAY_PERMISSION",
                        Uri.parse("package:" + context.packageName)
                    )
                )
                val handler = Handler()
                handler.postDelayed({ System.exit(1) }, 5000)
                return
            } else {
                val handler = Handler()
                handler.postDelayed({
                    context.startService(
                        Intent(
                            context,
                            FloatingModMenuService::class.java
                        )
                    )
                }, 500)
            }
        }

        init {
           System.loadLibrary("MyLibName")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Start(this)

        if (!hasLaunched) {
            hasLaunched = try {
                this@MainActivity.startActivity(
                    Intent(
                        this@MainActivity, Class.forName(
                            GameActivity
                        )
                    )
                )
                true
            } catch (e: ClassNotFoundException) {
               e.printStackTrace()
                return
            }
        }
    }
}
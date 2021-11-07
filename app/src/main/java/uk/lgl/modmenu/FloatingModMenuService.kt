package uk.lgl.modmenu

import android.R
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.text.Html
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextUtils
import android.text.method.DigitsKeyListener
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.SeekBar.OnSeekBarChangeListener
import java.util.*

class FloatingModMenuService : Service() {
    var TEXT_COLOR = Color.parseColor("#82CAFD")
    var TEXT_COLOR_2 = Color.parseColor("#FFFFFF")
    var BTN_COLOR = Color.parseColor("#1C262D")
    var MENU_BG_COLOR = Color.parseColor("#EE1C2A35")
    var MENU_FEATURE_BG_COLOR = Color.parseColor("#DD141C22")
    var MENU_WIDTH = 290
    var MENU_HEIGHT = 210
    var MENU_CORNER = 4f
    var ICON_SIZE = 45
    var ICON_ALPHA = 0.7f
    var ToggleON = Color.GREEN
    var ToggleOFF = Color.RED
    var BtnON = Color.parseColor("#1b5e20")
    var BtnOFF = Color.parseColor("#7f0000")
    var CategoryBG = Color.parseColor("#2F3D4C")
    var SeekBarColor = Color.parseColor("#80CBC4")
    var SeekBarProgressColor = Color.parseColor("#80CBC4")
    var CheckBoxColor = Color.parseColor("#80CBC4")
    var RadioColor = Color.parseColor("#FFFFFF")
    var NumberTxtColor = "#41c300"

    var mCollapsed: RelativeLayout? = null
    var mRootContainer: RelativeLayout? = null
    var mExpanded: LinearLayout? = null
    var patches: LinearLayout? = null
    var mSettings: LinearLayout? = null
    var mCollapse: LinearLayout? = null
    var scrlLLExpanded: LinearLayout.LayoutParams? = null
    var scrlLL: LinearLayout.LayoutParams? = null
    var mWindowManager: WindowManager? = null
    var params: WindowManager.LayoutParams? = null
    var startimage: ImageView? = null
    var rootFrame: FrameLayout? = null
    var scrollView: ScrollView? = null
    var stopChecking = false

    external fun setTitleText(textView: TextView?)
    external fun setHeadingText(textView: TextView?)
    external fun Icon(): String?
    external fun IconWebViewData(): String?
    external fun getFeatureList(): Array<String>
    external fun settingsList(): Array<String>
    external fun isGameLibLoaded(): Boolean?
    external fun Changes(con: Context?, fNum: Int?, fName: String?, i: Int?, bool: Boolean?, str: String?)

    fun changeFeatureInt(featureName: String?, featureNum: Int, value: Int) {
        Preferences.with(Preferences.context)!!
            .writeInt(featureNum, value)
        Changes(Preferences.context, featureNum, featureName, value, false, null)
    }

    fun changeFeatureString(featureName: String?, featureNum: Int, str: String?) {
        Preferences.with(Preferences.context)!!
            .writeString(featureNum, str)
        Changes(Preferences.context, featureNum, featureName, 0, false, str)
    }

    fun changeFeatureBool(featureName: String?, featureNum: Int, bool: Boolean) {
        Preferences.with(Preferences.context)!!
            .writeBoolean(featureNum, bool)
        Changes(Preferences.context, featureNum, featureName, 0, bool, null)
    }

    fun loadPrefInt(featureName: String?, featureNum: Int): Int {
        if (Preferences.loadPref) {
            val i = Preferences.with(Preferences.context)!!
                .readInt(featureNum)
            Changes(Preferences.context, featureNum, featureName, i, false, null)
            return i
        }
        return 0
    }

    fun loadPrefBool(featureName: String?, featureNum: Int, bDef: Boolean): Boolean {
        var bDef = bDef
        val bool = Preferences.with(Preferences.context)!!
            .readBoolean(featureNum, bDef)
        if (featureNum == -1) {
            Preferences.loadPref = bool
        }
        if (featureNum == -3) {
            Preferences.isExpanded = bool
        }
        if (Preferences.loadPref || featureNum < 0) {
            bDef = bool
        }
        Changes(Preferences.context, featureNum, featureName, 0, bDef, null)
        return bDef
    }

    fun loadPrefString(featureName: String?, featureNum: Int): String? {
        if (Preferences.loadPref || featureNum <= 0) {
            val str = Preferences.with(Preferences.context)!!
                .readString(featureNum)
            Changes(Preferences.context, featureNum, featureName, 0, false, str)
            return str
        }
        return ""
    }

    override fun onCreate() {
        super.onCreate()
        Preferences.context = this

       initFloating()

        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                Thread()
                handler.postDelayed(this, 1000)
            }
        })
    }

     private fun initFloating() {
        rootFrame = FrameLayout(this)
        rootFrame!!.setOnTouchListener(onTouchListener())
        mRootContainer = RelativeLayout(this)
        mCollapsed = RelativeLayout(this)
        mCollapsed!!.visibility = View.VISIBLE
        mCollapsed!!.alpha = ICON_ALPHA

        mExpanded = LinearLayout(this)
        mExpanded!!.visibility = View.GONE
        mExpanded!!.setBackgroundColor(MENU_BG_COLOR)
        mExpanded!!.orientation = LinearLayout.VERTICAL
        mExpanded!!.layoutParams =
        LinearLayout.LayoutParams(dp(MENU_WIDTH), ViewGroup.LayoutParams.WRAP_CONTENT)
        val gdMenuBody = GradientDrawable()
        gdMenuBody.cornerRadius = MENU_CORNER
        gdMenuBody.setColor(MENU_BG_COLOR)
        gdMenuBody.setStroke(1, Color.parseColor("#32cb00"))

        startimage = ImageView(this)
        startimage!!.layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val applyDimension =
            TypedValue.applyDimension(1, ICON_SIZE.toFloat(), resources.displayMetrics)
                .toInt()
        startimage!!.layoutParams.height = applyDimension
        startimage!!.layoutParams.width = applyDimension
        startimage!!.scaleType = ImageView.ScaleType.FIT_XY
        val decode = Base64.decode(Icon(), 0)
        startimage!!.setImageBitmap(BitmapFactory.decodeByteArray(decode, 0, decode.size))
        (startimage!!.layoutParams as MarginLayoutParams).topMargin = convertDipToPixels(10)
        startimage!!.setOnTouchListener(onTouchListener())
        startimage!!.setOnClickListener {
            mCollapsed!!.visibility = View.GONE
            mExpanded!!.visibility = View.VISIBLE
        }

        val wView = WebView(this)
        wView.layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val applyDimension2 =
            TypedValue.applyDimension(1, ICON_SIZE.toFloat(), resources.displayMetrics)
                .toInt()
        wView.layoutParams.height = applyDimension2
        wView.layoutParams.width = applyDimension2
        wView.loadData(
            "<html>" +
                    "<head></head>" +
                    "<body style=\"margin: 0; padding: 0\">" +
                    "<img src=\"" + IconWebViewData() + "\" width=\"" + ICON_SIZE + "\" height=\"" + ICON_SIZE + "\" >" +
                    "</body>" +
                    "</html>", "text/html", "utf-8"
        )
        wView.setBackgroundColor(0x00000000)
        wView.alpha = ICON_ALPHA
        wView.settings.setAppCacheEnabled(true)
        wView.setOnTouchListener(onTouchListener())

        val settings = TextView(this)
        settings.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) "⚙" else "\uD83D\uDD27"
        settings.setTextColor(TEXT_COLOR)
        settings.typeface = Typeface.DEFAULT_BOLD
        settings.textSize = 20.0f
        val rlsettings = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        rlsettings.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        settings.layoutParams = rlsettings
        settings.setOnClickListener(object : View.OnClickListener {
            var settingsOpen = false
            override fun onClick(v: View) {
                try {
                    settingsOpen = !settingsOpen
                    if (settingsOpen) {
                        scrollView!!.removeView(patches)
                        scrollView!!.addView(mSettings)
                        scrollView!!.scrollTo(0, 0)
                    } else {
                        scrollView!!.removeView(mSettings)
                        scrollView!!.addView(patches)
                    }
                } catch (e: IllegalStateException) {
                }
            }
        })

        mSettings = LinearLayout(this)
        mSettings!!.orientation = LinearLayout.VERTICAL
        featureList(settingsList(), mSettings!!)

        val titleText = RelativeLayout(this)
        titleText.setPadding(10, 5, 10, 5)
        titleText.setVerticalGravity(16)
        val title = TextView(this)
        title.setTextColor(TEXT_COLOR)
        title.textSize = 18.0f
        title.gravity = Gravity.CENTER
        val rl = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        rl.addRule(RelativeLayout.CENTER_HORIZONTAL)
        title.layoutParams = rl
        setTitleText(title)

        val heading = TextView(this)
        heading.ellipsize = TextUtils.TruncateAt.MARQUEE
        heading.marqueeRepeatLimit = -1
        heading.isSingleLine = true
        heading.isSelected = true
        heading.setTextColor(TEXT_COLOR)
        heading.textSize = 10.0f
        heading.gravity = Gravity.CENTER
        heading.setPadding(0, 0, 0, 5)
        setHeadingText(heading)

        scrollView = ScrollView(this)
        scrlLL = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(MENU_HEIGHT))
        scrlLLExpanded = LinearLayout.LayoutParams(mExpanded!!.layoutParams)
        scrlLLExpanded!!.weight = 1.0f
        scrollView!!.layoutParams = if (Preferences.isExpanded) scrlLLExpanded else scrlLL
        scrollView!!.setBackgroundColor(MENU_FEATURE_BG_COLOR)
        patches = LinearLayout(this)
        patches!!.orientation = LinearLayout.VERTICAL

        val relativeLayout = RelativeLayout(this)
        relativeLayout.setPadding(10, 3, 10, 3)
        relativeLayout.setVerticalGravity(Gravity.CENTER)

        val lParamsHideBtn = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lParamsHideBtn.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        val hideBtn = Button(this)
        hideBtn.layoutParams = lParamsHideBtn
        hideBtn.setBackgroundColor(Color.TRANSPARENT)
        hideBtn.text = "HIDE/KILL (Hold)"
        hideBtn.setTextColor(TEXT_COLOR)
        hideBtn.setOnClickListener { view ->
            mCollapsed!!.visibility = View.VISIBLE
            mCollapsed!!.alpha = 0f
            mExpanded!!.visibility = View.GONE
            Toast.makeText(
                view.context,
                "Icon hidden. Remember the hidden icon position",
                Toast.LENGTH_LONG
            ).show()
        }
        hideBtn.setOnLongClickListener { view ->
            Toast.makeText(view.context, "Menu service killed", Toast.LENGTH_LONG).show()
            this@FloatingModMenuService.stopSelf()
            false
        }

        val lParamsCloseBtn = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lParamsCloseBtn.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        val closeBtn = Button(this)
        closeBtn.layoutParams = lParamsCloseBtn
        closeBtn.setBackgroundColor(Color.TRANSPARENT)
        closeBtn.text = "MINIMIZE"
        closeBtn.setTextColor(TEXT_COLOR)
        closeBtn.setOnClickListener {
            mCollapsed!!.visibility = View.VISIBLE
            mCollapsed!!.alpha = ICON_ALPHA
            mExpanded!!.visibility = View.GONE
        }

       val iparams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 2038 else 2002
        params = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            iparams,
            8,
            -3
        )
        params!!.gravity = 51
        params!!.x = 0
        params!!.y = 100

        rootFrame!!.addView(mRootContainer)
        mRootContainer!!.addView(mCollapsed)
        mRootContainer!!.addView(mExpanded)
        if (IconWebViewData() != null) {
            mCollapsed!!.addView(wView)
        } else {
            mCollapsed!!.addView(startimage)
        }
        titleText.addView(title)
        titleText.addView(settings)
        mExpanded!!.addView(titleText)
        mExpanded!!.addView(heading)
        scrollView!!.addView(patches)
        mExpanded!!.addView(scrollView)
        relativeLayout.addView(hideBtn)
        relativeLayout.addView(closeBtn)
        mExpanded!!.addView(relativeLayout)
        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mWindowManager!!.addView(rootFrame, params)
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            var viewLoaded = false
            override fun run() {
               if (Preferences.loadPref  && !stopChecking) {
                    if (!viewLoaded) {
                        patches!!.addView(Category("Save preferences was been enabled. Waiting for game lib to be loaded...\n\nForce load menu may not apply mods instantly. You would need to reactivate them again"))
                        patches!!.addView(Button(-100, "Force load menu"))
                        viewLoaded = true
                    }
                    handler.postDelayed(this, 600)
                } else {
                    patches!!.removeAllViews()
                   featureList(getFeatureList(), patches!!)
                }
            }
        }, 500)
    }

    private fun onTouchListener(): OnTouchListener {
        return object : OnTouchListener {
            val collapsedView: View? = mCollapsed
            val expandedView: View? = mExpanded
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var initialX = 0
            private var initialY = 0
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                return when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params!!.x
                        initialY = params!!.y
                        initialTouchX = motionEvent.rawX
                        initialTouchY = motionEvent.rawY
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val rawX = (motionEvent.rawX - initialTouchX).toInt()
                        val rawY = (motionEvent.rawY - initialTouchY).toInt()
                        mExpanded!!.alpha = 1f
                        mCollapsed!!.alpha = 1f
                        if (rawX < 10 && rawY < 10 && isViewCollapsed) {
                            try {
                                collapsedView!!.visibility = View.GONE
                                expandedView!!.visibility = View.VISIBLE
                            } catch (e: NullPointerException) {
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        mExpanded!!.alpha = 0.5f
                        mCollapsed!!.alpha = 0.5f
                        params!!.x = initialX + (motionEvent.rawX - initialTouchX).toInt()
                        params!!.y = initialY + (motionEvent.rawY - initialTouchY).toInt()
                        mWindowManager!!.updateViewLayout(rootFrame, params)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun featureList(listFT: Array<String>, linearLayout: LinearLayout) {
        var linearLayout: LinearLayout? = linearLayout
        var featNum: Int
        var subFeat = 0
        val llBak = linearLayout
        for (i in listFT.indices) {
            var switchedOn = false
            //Log.i("featureList", listFT[i]);
            var feature = listFT[i]
            if (feature.contains("True_")) {
                switchedOn = true
                feature = feature.replaceFirst("True_".toRegex(), "")
            }
            linearLayout = llBak
            if (feature.contains("CollapseAdd_")) {
                //if (collapse != null)
                linearLayout = mCollapse
                feature = feature.replaceFirst("CollapseAdd_".toRegex(), "")
            }
            val str = feature.split("_").toTypedArray()

            //Assign feature number
            if (TextUtils.isDigitsOnly(str[0])) {
                featNum = str[0].toInt()
                feature = feature.replaceFirst(str[0] + "_".toRegex(), "")
                subFeat++
            } else {
                featNum = i - subFeat
            }
            val strSplit = feature.split("_").toTypedArray()
            when (strSplit[0]) {
                "Toggle" -> linearLayout!!.addView(Switch(featNum, strSplit[1], switchedOn))
                "SeekBar" -> linearLayout!!.addView(
                    SeekBar(
                        featNum,
                        strSplit[1],
                        strSplit[2].toInt(),
                        strSplit[3].toInt()
                    )
                )
                "Button" -> linearLayout!!.addView(Button(featNum, strSplit[1]))
                "ButtonOnOff" -> linearLayout!!.addView(
                    ButtonOnOff(
                        featNum,
                        strSplit[1],
                        switchedOn
                    )
                )
                "Spinner" -> {
                    linearLayout!!.addView(RichTextView(strSplit[1]))
                    linearLayout.addView(Spinner(featNum, strSplit[1], strSplit[2]))
                }
                "InputText" -> linearLayout!!.addView(TextField(featNum, strSplit[1], false, 0))
                "InputValue" -> {
                    if (strSplit.size == 3) linearLayout!!.addView(
                        TextField(
                            featNum,
                            strSplit[2],
                            true,
                            strSplit[1].toInt()
                        )
                    )
                    if (strSplit.size == 2) linearLayout!!.addView(
                        TextField(
                            featNum,
                            strSplit[1],
                            true,
                            0
                        )
                    )
                }
                "CheckBox" -> linearLayout!!.addView(CheckBox(featNum, strSplit[1], switchedOn))
                "RadioButton" -> linearLayout!!.addView(
                    RadioButton(
                        featNum,
                        strSplit[1],
                        strSplit[2]
                    )
                )
                "Collapse" -> {
                    Collapse(linearLayout, strSplit[1])
                    subFeat++
                }
                "ButtonLink" -> {
                    subFeat++
                    linearLayout!!.addView(ButtonLink(strSplit[1], strSplit[2]))
                }
                "Category" -> {
                    subFeat++
                    linearLayout!!.addView(Category(strSplit[1]))
                }
                "RichTextView" -> {
                    subFeat++
                    linearLayout!!.addView(RichTextView(strSplit[1]))
                }
                "RichWebView" -> {
                    subFeat++
                    linearLayout!!.addView(RichWebView(strSplit[1]))
                }
            }
        }
    }

    private fun Switch(featNum: Int, featName: String, swiOn: Boolean): View {
        val switchR = Switch(this)
        val buttonStates = ColorStateList(
            arrayOf(
                intArrayOf(-R.attr.state_enabled),
                intArrayOf(R.attr.state_checked),
                intArrayOf()
            ), intArrayOf(
                Color.BLUE,
                ToggleON,  // ON
                ToggleOFF // OFF
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switchR.thumbDrawable.setTintList(buttonStates)
            switchR.trackDrawable.setTintList(buttonStates)
        }
        switchR.text = featName
        switchR.setTextColor(TEXT_COLOR_2)
        switchR.setPadding(10, 5, 0, 5)
        switchR.isChecked = loadPrefBool(featName, featNum, swiOn)
        switchR.setOnCheckedChangeListener { compoundButton, bool ->
           changeFeatureBool(featName, featNum, bool)
            when (featNum) {
                1000 -> {
                    Preferences.with(switchR.context)?.writeBoolean(1000, bool)

                }
                1001 -> {
                    Preferences.isExpanded = bool
                    scrollView!!.layoutParams = if (bool) scrlLLExpanded else scrlLL
                }
            }
        }
        return switchR
    }


    private fun SeekBar(featNum: Int, featName: String, min: Int, max: Int): View {
        val loadedProg = loadPrefInt(featName, featNum)
        val linearLayout = LinearLayout(this)
        linearLayout.setPadding(10, 5, 0, 5)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.gravity = Gravity.CENTER
        val textView = TextView(this)
        textView.text =
            Html.fromHtml(featName + ": <font color='" + NumberTxtColor + "'>" + if (loadedProg == 0) min else loadedProg)
        textView.setTextColor(TEXT_COLOR_2)
        val seekBar = SeekBar(this)
        seekBar.setPadding(25, 10, 35, 10)
        seekBar.max = max
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) seekBar.min =
            min
        seekBar.progress = if (loadedProg == 0) min else loadedProg
        seekBar.thumb.setColorFilter(SeekBarColor, PorterDuff.Mode.SRC_ATOP)
        seekBar.progressDrawable.setColorFilter(SeekBarProgressColor, PorterDuff.Mode.SRC_ATOP)
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, i: Int, z: Boolean) {
                seekBar.progress = if (i < min) min else i
                changeFeatureInt(featName, featNum, if (i < min) min else i)
                textView.text =
                    Html.fromHtml(featName + ": <font color='" + NumberTxtColor + "'>" + if (i < min) min else i)
            }
        })
        linearLayout.addView(textView)
        linearLayout.addView(seekBar)
        return linearLayout
    }

    private fun Button(featNum: Int, featName: String): View {
        val button = Button(this)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(7, 5, 7, 5)
        button.layoutParams = layoutParams
        button.setTextColor(TEXT_COLOR_2)
        button.isAllCaps = false //Disable caps to support html
        button.text = Html.fromHtml(featName)
        button.setBackgroundColor(BTN_COLOR)
        button.setOnClickListener {
            when (featNum) {
                1003 -> Logcat.Save(applicationContext)
                1004 -> Logcat.Clear(applicationContext)
                1005 -> {
                    scrollView!!.removeView(mSettings)
                    scrollView!!.addView(patches)
                }
                -100 -> stopChecking = true
            }
            changeFeatureInt(featName, featNum, 0)
        }
        return button
    }

    private fun ButtonLink(featName: String, url: String): View {
        val button = Button(this)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(7, 5, 7, 5)
        button.layoutParams = layoutParams
        button.isAllCaps = false //Disable caps to support html
        button.setTextColor(TEXT_COLOR_2)
        button.text = Html.fromHtml(featName)
        button.setBackgroundColor(BTN_COLOR)
        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
        return button
    }

    private fun ButtonOnOff(featNum: Int, featName: String, switchedOn: Boolean): View {
        val button = Button(this)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(7, 5, 7, 5)
        button.layoutParams = layoutParams
        button.setTextColor(TEXT_COLOR_2)
        button.isAllCaps = false //Disable caps to support html
        val finalfeatName = featName.replace("OnOff_", "")
        var isOn = loadPrefBool(featName, featNum, switchedOn)
        if (isOn) {
            button.text = Html.fromHtml("$finalfeatName: ON")
            button.setBackgroundColor(BtnON)
            isOn = false
        } else {
            button.text = Html.fromHtml("$finalfeatName: OFF")
            button.setBackgroundColor(BtnOFF)
            isOn = true
        }
        val finalIsOn = isOn
        button.setOnClickListener(object : View.OnClickListener {
            var isOn = finalIsOn
            override fun onClick(v: View) {
                changeFeatureBool(finalfeatName, featNum, isOn)
                //Log.d(TAG, finalfeatName + " " + featNum + " " + isActive2);
                if (isOn) {
                    button.text = Html.fromHtml("$finalfeatName: ON")
                    button.setBackgroundColor(BtnON)
                    isOn = false
                } else {
                    button.text = Html.fromHtml("$finalfeatName: OFF")
                    button.setBackgroundColor(BtnOFF)
                    isOn = true
                }
            }
        })
        return button
    }

    private fun Spinner(featNum: Int, featName: String, list: String): View {
        Log.d(TAG, "spinner $featNum $featName $list")
        val lists: List<String?> = LinkedList(Arrays.asList(*list.split(",").toTypedArray()))

        // Create another LinearLayout as a workaround to use it as a background
        // to keep the down arrow symbol. No arrow symbol if setBackgroundColor set
        val linearLayout2 = LinearLayout(this)
        val layoutParams2 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams2.setMargins(10, 2, 10, 5)
        linearLayout2.orientation = LinearLayout.VERTICAL
        linearLayout2.setBackgroundColor(BTN_COLOR)
        linearLayout2.layoutParams = layoutParams2
        val spinner = Spinner(this, Spinner.MODE_DROPDOWN)
        spinner.setPadding(5, 10, 5, 8)
        spinner.layoutParams = layoutParams2
        spinner.background.setColorFilter(
            1,
            PorterDuff.Mode.SRC_ATOP
        ) //trick to show white down arrow color
        //Creating the ArrayAdapter instance having the list
        val aa: ArrayAdapter<*> = ArrayAdapter<Any?>(this, R.layout.simple_spinner_item, lists)
        aa.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        //Setting the ArrayAdapter data on the Spinner'
        spinner.adapter = aa
        spinner.setSelection(loadPrefInt(featName, featNum))
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View,
                position: Int,
                id: Long
            ) {
                changeFeatureInt(spinner.selectedItem.toString(), featNum, position)
                (parentView.getChildAt(0) as TextView).setTextColor(TEXT_COLOR_2)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        linearLayout2.addView(spinner)
        return linearLayout2
    }

    private fun TextField(featNum: Int, featName: String, numOnly: Boolean, maxValue: Int): View {
        val edittextstring = EditTextString()
        val edittextnum = EditTextNum()
        val linearLayout = LinearLayout(this)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(7, 5, 7, 5)
        val button = Button(this)
        if (numOnly) {
            val num = loadPrefInt(featName, featNum)

            button.text =
                Html.fromHtml(featName + ": <font color='" + NumberTxtColor + "'>" + (if (num == 0) 1 else num) + "</font>")
        } else {
            val string = loadPrefString(featName, featNum)
            edittextstring.string = if (string === "") "" else string
            button.text = Html.fromHtml("$featName: <font color='$NumberTxtColor'>$string</font>")
        }
        button.isAllCaps = false
        button.layoutParams = layoutParams
        button.setBackgroundColor(BTN_COLOR)
        button.setTextColor(TEXT_COLOR_2)
        button.setOnClickListener {
            val alert = AlertDialog.Builder(applicationContext, 2).create()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Objects.requireNonNull(alert.window)
                    ?.setType(if (Build.VERSION.SDK_INT >= 26) 2038 else 2002)
            }
            alert.setOnCancelListener {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
            }

            //LinearLayout
            val linearLayout1 = LinearLayout(applicationContext)
            linearLayout1.setPadding(5, 5, 5, 5)
            linearLayout1.orientation = LinearLayout.VERTICAL
            linearLayout1.setBackgroundColor(MENU_FEATURE_BG_COLOR)

            //TextView
            val TextViewNote = TextView(applicationContext)
            TextViewNote.text = "Tap OK to apply changes. Tap outside to cancel"
            if (maxValue != 0) TextViewNote.text =
                "Tap OK to apply changes. Tap outside to cancel\nMax value: $maxValue"
            TextViewNote.setTextColor(TEXT_COLOR_2)

            //Edit text
            val edittext = EditText(applicationContext)
            edittext.maxLines = 1
            edittext.width = convertDipToPixels(300)
            edittext.setTextColor(TEXT_COLOR_2)
            if (numOnly) {
                edittext.inputType = InputType.TYPE_CLASS_NUMBER
                edittext.keyListener = DigitsKeyListener.getInstance("0123456789-")
                val FilterArray = arrayOfNulls<InputFilter>(1)
                FilterArray[0] = LengthFilter(10)
                edittext.filters = FilterArray
            } else {
                edittext.setText(edittextstring.string)
            }
            edittext.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                if (hasFocus) {
                    imm.toggleSoftInput(
                        InputMethodManager.SHOW_FORCED,
                        InputMethodManager.HIDE_IMPLICIT_ONLY
                    )
                } else {
                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
                }
            }
            edittext.requestFocus()

            //Button
            val btndialog = Button(applicationContext)
            btndialog.setBackgroundColor(BTN_COLOR)
            btndialog.setTextColor(TEXT_COLOR_2)
            btndialog.text = "OK"
            btndialog.setOnClickListener {
                if (numOnly) {
                    var num: Int
                    try {
                        (if (TextUtils.isEmpty(edittext.text.toString())) "0" else edittext.text.toString()
                            .toInt()).also { num = it as Int }
                        if (maxValue != 0 && num >= maxValue) num = maxValue
                    } catch (ex: NumberFormatException) {
                        num = 2147483640
                    }

                    button.text =
                        Html.fromHtml("$featName: <font color='$NumberTxtColor'>$num</font>")
                    alert.dismiss()
                    changeFeatureInt(featName, featNum, num)
                } else {
                    val str = edittext.text.toString()
                    edittextstring.string = edittext.text.toString()
                    button.text =
                        Html.fromHtml("$featName: <font color='$NumberTxtColor'>$str</font>")
                    alert.dismiss()
                   changeFeatureString(featName, featNum, str)
                }
                edittext.isFocusable = false
            }
            linearLayout1.addView(TextViewNote)
            linearLayout1.addView(edittext)
            linearLayout1.addView(btndialog)
            alert.setView(linearLayout1)
            alert.show()
        }
        linearLayout.addView(button)
        return linearLayout
    }

    private fun CheckBox(featNum: Int, featName: String, switchedOn: Boolean): View {
        val checkBox = CheckBox(this)
        checkBox.text = featName
        checkBox.setTextColor(TEXT_COLOR_2)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) checkBox.buttonTintList =
            ColorStateList.valueOf(CheckBoxColor)
        checkBox.isChecked = loadPrefBool(featName, featNum, switchedOn)
        checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (checkBox.isChecked) {
                changeFeatureBool(featName, featNum, isChecked)
            } else {
               changeFeatureBool(featName, featNum, isChecked)
            }
        }
        return checkBox
    }

    private fun RadioButton(featNum: Int, featName: String, list: String): View {
        //Credit: LoraZalora
        val lists: List<String> = LinkedList(Arrays.asList(*list.split(",").toTypedArray()))
        val textView = TextView(this)
        textView.text = "$featName:"
        textView.setTextColor(TEXT_COLOR_2)
        val radioGroup = RadioGroup(this)
        radioGroup.setPadding(10, 5, 10, 5)
        radioGroup.orientation = LinearLayout.VERTICAL
        radioGroup.addView(textView)
        for (i in lists.indices) {
            val Radioo = RadioButton(this)
            val radioName = lists[i]
            val first_radio_listener = View.OnClickListener {
                textView.text = Html.fromHtml("$featName: <font color='$NumberTxtColor'>$radioName")
                changeFeatureInt(
                    featName,
                    featNum,
                    radioGroup.indexOfChild(Radioo)
                )
            }
            println(lists[i])
            Radioo.text = lists[i]
            Radioo.setTextColor(Color.LTGRAY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Radioo.buttonTintList =
                ColorStateList.valueOf(RadioColor)
            Radioo.setOnClickListener(first_radio_listener)
            radioGroup.addView(Radioo)
        }
        val index = loadPrefInt(featName, featNum)
        if (index > 0) { //Preventing it to get an index less than 1. below 1 = null = crash
            textView.text =
                Html.fromHtml(featName + ": <font color='" + NumberTxtColor + "'>" + lists[index - 1])
            (radioGroup.getChildAt(index) as RadioButton).isChecked = true
        }
        return radioGroup
    }

    private fun Collapse(linLayout: LinearLayout?, text: String) {
        val layoutParamsLL = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParamsLL.setMargins(0, 5, 0, 0)
        val collapse = LinearLayout(this)
        collapse.layoutParams = layoutParamsLL
        collapse.setVerticalGravity(16)
        collapse.orientation = LinearLayout.VERTICAL
        val collapseSub = LinearLayout(this)
        collapseSub.setVerticalGravity(16)
        collapseSub.setPadding(0, 5, 0, 5)
        collapseSub.orientation = LinearLayout.VERTICAL
        collapseSub.setBackgroundColor(Color.parseColor("#222D38"))
        collapseSub.visibility = View.GONE
        mCollapse = collapseSub
        val textView = TextView(this)
        textView.setBackgroundColor(CategoryBG)
        textView.text = "▽ $text ▽"
        textView.gravity = Gravity.CENTER
        textView.setTextColor(TEXT_COLOR_2)
        textView.setTypeface(null, Typeface.BOLD)
        textView.setPadding(0, 20, 0, 20)
        textView.setOnClickListener(object : View.OnClickListener {
            var isChecked = false
            override fun onClick(v: View) {
                val z = !isChecked
                isChecked = z
                if (z) {
                    collapseSub.visibility = View.VISIBLE
                    textView.text = "△ $text △"
                    return
                }
                collapseSub.visibility = View.GONE
                textView.text = "▽ $text ▽"
            }
        })
        collapse.addView(textView)
        collapse.addView(collapseSub)
        linLayout!!.addView(collapse)
    }

    private fun Category(text: String): View {
        val textView = TextView(this)
        textView.setBackgroundColor(CategoryBG)
        textView.text = Html.fromHtml(text)
        textView.gravity = Gravity.CENTER
        textView.setTextColor(TEXT_COLOR_2)
        textView.setTypeface(null, Typeface.BOLD)
        textView.setPadding(0, 5, 0, 5)
        return textView
    }

    private fun RichTextView(text: String): View {
        val textView = TextView(this)
        textView.text = Html.fromHtml(text)
        textView.setTextColor(TEXT_COLOR_2)
        textView.setPadding(10, 5, 10, 5)
        return textView
    }

    private fun RichWebView(text: String): View {
        val wView = WebView(this)
        wView.loadData(text, "text/html", "utf-8")
        wView.setBackgroundColor(0x00000000) //Transparent
        wView.setPadding(0, 5, 0, 5)
        wView.settings.setAppCacheEnabled(false)
        return wView
    }

    override fun onStartCommand(intent: Intent, i: Int, i2: Int): Int {
        return START_NOT_STICKY
    }

    private val isViewCollapsed: Boolean
        private get() = rootFrame == null || mCollapsed!!.visibility == View.VISIBLE

    private fun convertDipToPixels(i: Int): Int {
        return (i.toFloat() * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun dp(i: Int): Int {
        return TypedValue.applyDimension(1, i.toFloat(), resources.displayMetrics).toInt()
    }

    private val isNotInGame: Boolean
        private get() {
            val runningAppProcessInfo = RunningAppProcessInfo()
            ActivityManager.getMyMemoryState(runningAppProcessInfo)
            return runningAppProcessInfo.importance != 100
        }

    override fun onDestroy() {
        super.onDestroy()
        if (rootFrame != null) {
            mWindowManager!!.removeView(rootFrame)
        }
    }

    override fun onTaskRemoved(intent: Intent) {
        super.onTaskRemoved(intent)
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        stopSelf()
    }

    private fun Thread() {
        if (rootFrame == null) {
            return
        }
        if (isNotInGame) {
            rootFrame!!.visibility = View.INVISIBLE
        } else {
            rootFrame!!.visibility = View.VISIBLE
        }
    }

    private inner class EditTextString {
        var string: String? = null
    }

    private inner class EditTextNum {
        var num = 0
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        const val TAG = "Mod_Menu"
    }
}
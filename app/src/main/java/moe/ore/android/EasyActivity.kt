package moe.ore.android

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity

abstract class EasyActivity : AppCompatActivity() {

    override fun getTheme(): Resources.Theme {
        return super.getTheme()
    }
}
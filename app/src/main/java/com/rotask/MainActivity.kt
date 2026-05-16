package com.rotask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rotask.ui.RotaskNavHost
import com.rotask.ui.theme.RotaskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as RotaskApplication
        setContent {
            RotaskTheme {
                RotaskNavHost(app)
            }
        }
    }
}

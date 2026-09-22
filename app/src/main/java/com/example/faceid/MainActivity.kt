package com.example.faceid

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.faceid.kevin.navigation.AppNavigation
import com.example.faceid.ui.theme.FACEIDTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FACEIDTheme {
                AppNavigation()
            }
        }
    }
}

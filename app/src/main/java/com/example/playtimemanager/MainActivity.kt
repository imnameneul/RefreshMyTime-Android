// 파일 경로: app/src/main/java/com/example/playtimemanager/MainActivity.kt

package com.example.playtimemanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.playtimemanager.ui.AppScreen
import com.example.playtimemanager.ui.MainViewModel
import com.example.playtimemanager.ui.theme.PlayTimeManagerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    // ✅ API 33+ 알림 권한 런처
    private val requestNotiPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "알림 권한이 거부되어 타이머 알림 표시가 제한될 수 있어요.",
                Toast.LENGTH_SHORT
            ).show()
        }
        // granted면 별도 동작 불필요: 사용자가 타이머 시작할 때 ViewModel이 서비스 시작
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 앱 시작 시 1회: API 33+에서 알림 권한 체크/요청
        ensureNotificationPermission()

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_START) {
                    viewModel.checkForDateChangeAndRefresh()
                }
            }
        })

        setContent {
            PlayTimeManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                // 필요 시 rationale UI를 선행할 수 있음 (스낵바/다이얼로그)
                requestNotiPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

}
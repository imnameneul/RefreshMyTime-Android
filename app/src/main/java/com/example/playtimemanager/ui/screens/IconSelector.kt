// 파일 경로: app/src/main/java/com/example/playtimemanager/ui/screens/IconSelector.kt

package com.example.playtimemanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.playtimemanager.data.ActivityType

/**
 * '활동 선택창'의 UI와 동작을 정의하는 함수(설계도)입니다.
 * @param activities 표시할 활동 아이콘 목록
 * @param onActivitySelected 아이콘이 선택되었을 때 호출될 함수
 * @param onDismiss 다이얼로그가 닫혀야 할 때 호출될 함수
 */
@Composable
fun ActivitySelectorDialog(
    activities: List<ActivityType>,
    onActivitySelected: (ActivityType) -> Unit,
    onDismiss: () -> Unit
) {
    // 화면 위에 떠 있는 대화 상자를 만듭니다.
    Dialog(onDismissRequest = onDismiss) {
        // 대화 상자의 흰색 배경과 내용물입니다.
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "어떤 활동을 시작할까요?",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 아이콘들을 4열 격자(Grid) 형태로 보여줍니다.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 전달받은 활동 목록만큼 아이콘을 반복해서 그립니다.
                    items(activities) { activity ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onActivitySelected(activity) } // 클릭 시 선택된 활동을 ViewModel에 알립니다.
                        ) {
                            ActivityIcon(
                                type = activity,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activity.displayName,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
// 파일 경로: app/src/main/java/com/example/playtimemanager/ui/screens/ActivityIcon.kt

package com.example.playtimemanager.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.playtimemanager.data.ActivityType

@Composable
fun ActivityIcon(
    type: ActivityType,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(
        imageVector = type.icon,
        contentDescription = type.displayName,
        modifier = modifier,
        tint = tint
    )
}

val ActivityType.icon: ImageVector
    get() = when (this) {
        ActivityType.GAMING -> Icons.Default.Gamepad
        ActivityType.MUSIC -> Icons.Default.MusicNote
        ActivityType.READING -> Icons.Default.Book
        ActivityType.WATCHING -> Icons.Default.Tv
        // ActivityType.APP -> Icons.Default.Apps <-- 이 라인을 완전히 삭제했습니다!
        ActivityType.FRIENDS -> Icons.Default.Group
        ActivityType.TRAVEL -> Icons.Default.DirectionsCar
        ActivityType.COFFEE -> Icons.Default.Coffee
        ActivityType.PHOTO -> Icons.Default.PhotoCamera
        ActivityType.PODCAST -> Icons.Default.Headphones
        ActivityType.MOVIE -> Icons.Default.Movie
        ActivityType.SHOPPING -> Icons.Default.ShoppingCart
        ActivityType.EXERCISE -> Icons.Default.FitnessCenter
        ActivityType.ART -> Icons.Default.Palette
        ActivityType.OUTDOOR -> Icons.Default.Terrain
        ActivityType.HOBBY -> Icons.Default.Favorite
        ActivityType.EMPTY -> Icons.Default.RadioButtonUnchecked
    }
package com.example.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComputedStatus
import com.example.ui.theme.ExpiredOrangeBg
import com.example.ui.theme.ExpiredOrangeText
import com.example.ui.theme.InvalidRedBg
import com.example.ui.theme.InvalidRedText
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.PendingAmberText
import com.example.ui.theme.ValidGreenBg
import com.example.ui.theme.ValidGreenText

@Composable
fun StatusBadge(
    status: ComputedStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        ComputedStatus.VALID -> ValidGreenBg to ValidGreenText
        ComputedStatus.PENDING -> PendingAmberBg to PendingAmberText
        ComputedStatus.EXPIRED -> ExpiredOrangeBg to ExpiredOrangeText
        ComputedStatus.INVALID -> InvalidRedBg to InvalidRedText
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(99.dp),
        color = bg
    ) {
        Text(
            text = status.label,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LineBorder
import com.example.util.QrCodecService

@Composable
fun QrThumbnail(
    content: String,
    qrColorHex: String,
    qrPattern: String,
    size: Dp,
    pixelSize: Int = 256,
    modifier: Modifier = Modifier
) {
    val bitmap: Bitmap = remember(content, qrColorHex, qrPattern, pixelSize) {
        QrCodecService.generateQrBitmap(
            content = content,
            colorHex = qrColorHex,
            sizePx = pixelSize,
            pattern = qrPattern
        )
    }

    val cornerRadius = if (qrPattern == "soft") 12.dp else 8.dp
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White)
            .border(1.dp, LineBorder, RoundedCornerShape(cornerRadius))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.cd_qr_code),
            modifier = Modifier.size(size - 8.dp),
            contentScale = ContentScale.Fit
        )
    }
}

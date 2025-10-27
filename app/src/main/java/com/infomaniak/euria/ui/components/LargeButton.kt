package com.infomaniak.euria.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.infomaniak.core.compose.basicbutton.BasicButton
import com.infomaniak.core.compose.basics.Typography
import com.infomaniak.core.compose.margin.Margin
import com.infomaniak.euria.ui.theme.Dimens

@Composable
fun LargeButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: () -> Boolean = { true },
    showIndeterminateProgress: () -> Boolean = { false },
    progress: (() -> Float)? = null,
    onClick: () -> Unit,
    imageVector: ImageVector? = null,
) {
    BasicButton(
        onClick = onClick,
        modifier = modifier.height(Dimens.PrimaryButtonHeight),
        shape = Dimens.PrimaryButtonShape,
        enabled = enabled,
        showIndeterminateProgress = showIndeterminateProgress,
        progress = progress,
    ) {
        ButtonTextContent(imageVector, title)
    }
}

@Composable
private fun ButtonTextContent(imageVector: ImageVector?, title: String) {
    imageVector?.let {
        Icon(modifier = Modifier.size(Dimens.SmallIconSize), imageVector = it, contentDescription = null)
        Spacer(Modifier.width(Margin.Mini))
    }
    Text(text = title, style = Typography.bodyMedium)
}

package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnType

@Composable
fun DtnTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = DtnType.body.copy(color = DtnColors.TextPrimary),
        cursorBrush = SolidColor(DtnColors.Cream),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(DtnShapes.radiusFull)
            .background(DtnColors.Surface2)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) DtnColors.Cream else DtnColors.BorderSubtle,
                shape = DtnShapes.radiusFull
            )
            .padding(horizontal = 20.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = DtnType.body.copy(color = DtnColors.TextTertiary)
                    )
                }
                innerTextField()
            }
        }
    )
}


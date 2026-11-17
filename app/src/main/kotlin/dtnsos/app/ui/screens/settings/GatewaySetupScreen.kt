package dtnsos.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dtnsos.app.R
import dtnsos.app.ui.components.DtnTextField
import dtnsos.app.ui.components.GhostButton
import dtnsos.app.ui.components.PrimaryButton
import dtnsos.app.ui.components.ScreenScaffold
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GatewaySetupScreen(
    onSaveAndBecomeGateway: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var gatewayName by remember { mutableStateOf("Relief Camp Alpha") }
    var brokerAddress by remember { mutableStateOf("broker.hivemq.com:8883") }
    var accessToken by remember { mutableStateOf("dtn-secret-token") }
    var showToken by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    ScreenScaffold(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DtnColors.TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.gateway_setup_title),
                style = DtnType.headline,
                color = DtnColors.TextPrimary
            )
        }

        Text(
            text = stringResource(R.string.gateway_setup_helper),
            style = DtnType.body,
            color = DtnColors.TextSecondary
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DtnTextField(
                value = gatewayName,
                onValueChange = { gatewayName = it },
                placeholder = stringResource(R.string.label_gateway_name)
            )

            DtnTextField(
                value = brokerAddress,
                onValueChange = { brokerAddress = it },
                placeholder = stringResource(R.string.label_broker_address),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            DtnTextField(
                value = accessToken,
                onValueChange = { accessToken = it },
                placeholder = stringResource(R.string.label_access_token),
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            GhostButton(
                text = if (showToken) "Hide token" else "Show token",
                onClick = { showToken = !showToken },
                height = 40.dp
            )
        }

        Spacer(Modifier.height(8.dp))

        if (isTesting) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = DtnColors.Cream
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.action_test_connection),
                onClick = {
                    isTesting = true
                    testResult = null
                    scope.launch {
                        delay(800) // Simulated TLS connection
                        isTesting = false
                        testResult = true to "+ Connected"
                    }
                }
            )
        }

        testResult?.let { (success, text) ->
            Text(
                text = text,
                style = DtnType.bodyStrong,
                color = if (success) DtnColors.Positive else DtnColors.Negative
            )
        }

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = stringResource(R.string.action_save_gateway),
            onClick = onSaveAndBecomeGateway,
            enabled = testResult?.first == true
        )
    }
}


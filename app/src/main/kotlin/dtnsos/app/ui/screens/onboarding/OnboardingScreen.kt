package dtnsos.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.R
import dtnsos.app.ui.components.*
import dtnsos.app.ui.data.LocalMeshUiRepository
import dtnsos.app.ui.model.OnboardingUi
import dtnsos.app.ui.model.PermissionKey
import dtnsos.app.ui.model.PermissionStatus
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnSpacing
import dtnsos.app.ui.theme.DtnType
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repo = LocalMeshUiRepository.current
    val scope = rememberCoroutineScope()
    val onboardingState by repo.onboarding.collectAsState()

    var currentStep by remember { mutableIntStateOf(onboardingState.step) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DtnColors.BgBase)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = DtnSpacing.gutter),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = DtnSpacing.maxContentWidth)
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ProgressDots(currentStep = currentStep, totalSteps = 4)

            when (currentStep) {
                0 -> StepWelcome(onNext = { currentStep = 1 })
                1 -> StepPermissions(
                    state = onboardingState,
                    onRequestPermission = { key ->
                        scope.launch { repo.requestPermission(key) }
                    },
                    onNext = { currentStep = 2 },
                    onBack = { currentStep = 0 }
                )
                2 -> StepProfile(
                    state = onboardingState,
                    onSubmitProfile = { name, phone, notes ->
                        scope.launch {
                            repo.submitProfile(name, phone, notes)
                            currentStep = 3
                        }
                    },
                    onBack = { currentStep = 1 }
                )
                3 -> StepReady(
                    state = onboardingState,
                    onOpenApp = {
                        scope.launch {
                            repo.completeOnboarding()
                            onComplete()
                        }
                    },
                    onBack = { currentStep = 2 }
                )
            }
        }
    }
}

@Composable
private fun StepWelcome(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        HeroCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val wordmark = buildAnnotatedString {
                    withStyle(SpanStyle(color = DtnColors.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)) {
                        append("dtn")
                    }
                    withStyle(SpanStyle(color = DtnColors.AccentDot, fontSize = 32.sp, fontWeight = FontWeight.Bold)) {
                        append(".")
                    }
                }
                Text(text = wordmark)

                Text(
                    text = stringResource(R.string.onboarding_welcome_headline),
                    style = DtnType.headlineLg,
                    color = DtnColors.TextPrimary
                )

                Text(
                    text = stringResource(R.string.onboarding_welcome_body),
                    style = DtnType.body,
                    color = DtnColors.TextSecondary
                )
            }
        }

        Spacer(Modifier.weight(1f, fill = false))

        PrimaryButton(
            text = stringResource(R.string.action_get_started),
            onClick = onNext
        )
    }
}

@Composable
private fun StepPermissions(
    state: OnboardingUi,
    onRequestPermission: (PermissionKey) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val perms = state.permissions
    val nearbyGranted = perms[PermissionKey.NEARBY_DEVICES] == PermissionStatus.GRANTED
    val locationGranted = perms[PermissionKey.LOCATION] == PermissionStatus.GRANTED
    val bgGranted = perms[PermissionKey.BACKGROUND] == PermissionStatus.GRANTED
    val canContinue = nearbyGranted && locationGranted && bgGranted

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_perm_title),
            style = DtnType.headline,
            color = DtnColors.TextPrimary
        )

        Text(
            text = stringResource(R.string.onboarding_perm_helper),
            style = DtnType.label,
            color = DtnColors.TextSecondary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                PermissionRow(
                    title = stringResource(R.string.perm_nearby_title),
                    reason = stringResource(R.string.perm_nearby_reason),
                    status = perms[PermissionKey.NEARBY_DEVICES] ?: PermissionStatus.NOT_ASKED,
                    isRequired = true,
                    onActionClick = { onRequestPermission(PermissionKey.NEARBY_DEVICES) }
                )
                PermissionRow(
                    title = stringResource(R.string.perm_location_title),
                    reason = stringResource(R.string.perm_location_reason),
                    status = perms[PermissionKey.LOCATION] ?: PermissionStatus.NOT_ASKED,
                    isRequired = true,
                    onActionClick = { onRequestPermission(PermissionKey.LOCATION) }
                )
                PermissionRow(
                    title = stringResource(R.string.perm_background_title),
                    reason = stringResource(R.string.perm_background_reason),
                    status = perms[PermissionKey.BACKGROUND] ?: PermissionStatus.NOT_ASKED,
                    isRequired = true,
                    onActionClick = { onRequestPermission(PermissionKey.BACKGROUND) }
                )
                PermissionRow(
                    title = stringResource(R.string.perm_notifications_title),
                    reason = stringResource(R.string.perm_notifications_reason),
                    status = perms[PermissionKey.NOTIFICATIONS] ?: PermissionStatus.NOT_ASKED,
                    isRequired = false,
                    onActionClick = { onRequestPermission(PermissionKey.NOTIFICATIONS) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = stringResource(R.string.action_continue),
            onClick = onNext,
            enabled = canContinue
        )

        GhostButton(
            text = stringResource(R.string.action_back),
            onClick = onBack
        )
    }
}

@Composable
private fun StepProfile(
    state: OnboardingUi,
    onSubmitProfile: (name: String, phone: String, notes: String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_profile_title),
            style = DtnType.headline,
            color = DtnColors.TextPrimary
        )

        Text(
            text = stringResource(R.string.onboarding_profile_helper),
            style = DtnType.label,
            color = DtnColors.TextSecondary
        )

        DtnTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.label_name)
        )

        DtnTextField(
            value = phone,
            onValueChange = { phone = it },
            placeholder = stringResource(R.string.label_phone),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        DtnTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = stringResource(R.string.label_notes)
        )

        if (errorMsg != null) {
            Text(
                text = errorMsg!!,
                style = DtnType.caption,
                color = DtnColors.Negative
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.profile_key_title),
                    style = DtnType.title,
                    color = DtnColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.profile_key_body),
                    style = DtnType.body,
                    color = DtnColors.TextSecondary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = DtnColors.Cream
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.action_create_key_continue),
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Name is required"
                        return@PrimaryButton
                    }
                    val cleanPhone = phone.filter { it.isDigit() }
                    if (cleanPhone.length !in 7..15) {
                        errorMsg = "Enter a valid 7–15 digit phone number"
                        return@PrimaryButton
                    }
                    errorMsg = null
                    isSubmitting = true
                    onSubmitProfile(name, phone, notes)
                }
            )

            GhostButton(
                text = stringResource(R.string.action_back),
                onClick = onBack
            )
        }
    }
}

@Composable
private fun StepReady(
    state: OnboardingUi,
    onOpenApp: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_ready_title),
            style = DtnType.headline,
            color = DtnColors.TextPrimary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.readiness.forEach { item ->
                    ChecklistRow(
                        text = item.label,
                        isOk = item.ok,
                        reason = item.reason
                    )
                }
                if (state.keyRegistered == false) {
                    ChecklistRow(
                        text = stringResource(R.string.profile_key_unregistered_warning),
                        isOk = false,
                        reason = "Will retry automatically."
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        PrimaryButton(
            text = stringResource(R.string.action_open_app),
            onClick = onOpenApp
        )

        GhostButton(
            text = stringResource(R.string.action_back),
            onClick = onBack
        )
    }
}


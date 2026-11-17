package dtnsos.app.ui.navigation

sealed interface SubScreen {
    data object None : SubScreen
    data object GatewaySetup : SubScreen
    data object BackgroundHelp : SubScreen
    data object StateGallery : SubScreen
}

sealed interface ActiveSheet {
    data object None : ActiveSheet
    data object ComposeSos : ActiveSheet
    data object StopListening : ActiveSheet
    data object CancelSos : ActiveSheet
    data object LowBattery : ActiveSheet
    data object SupportedPhones : ActiveSheet
}


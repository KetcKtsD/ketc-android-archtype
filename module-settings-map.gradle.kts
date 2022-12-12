subprojects {
    /**
     * isAndroid: Boolean <- If True, treat it as an AndroidProject.
     * isAndroidLib: Boolean <- If True, treat it as an AndroidLibraryProject.
     * isCompose: Boolean <- If True, enable Jetpack Compose.
     */
    val moduleSettingsMap: Map<String, Map<String, Any>> by extra {
        mapOf(
            "app" to mapOf(
                "isAndroid" to true,
                "isCompose" to true
            )
        )
    }
}

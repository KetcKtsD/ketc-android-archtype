@file:Suppress("UNUSED_VARIABLE")

subprojects {
    val minAndroidSdkVersion by extra { 31 }
    val targetAndroidSdkVersion by extra { 33 }
    val androidBuildToolVersion by extra { "33.0.1" }
    // val androidNdkVersion: String by extra { "TODO(Please set androidNdkVersion)" }

    val appVersionName by extra { "0.0.1" }
    val appVersionCode by extra { 1 }
    val appId by extra { "tech.ketc.sample" }

    val namespace by extra { "tech.ketc" }
}

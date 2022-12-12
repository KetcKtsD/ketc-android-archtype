@Suppress("UNUSED_VARIABLE")
subprojects {
    //dependency versions
    val spekVersion by extra { "2.0.17" }
    val coroutinesVersion by extra { "1.6.4" }
    val timberVersion by extra { "5.0.1" }
    val xAnnotationVersion by extra { "1.5.0" }
    val composeBomVersion by extra { "2022.10.00" } // compose versions bom -> https://developer.android.com/jetpack/compose/setup?#setup-compose

    //test dependency versions
    val junitTestRunnerVersion by extra { "1.0.0" }
    val junitJupiterVersion by extra { "5.6.2" }
    val androidJunit5TestRunnerVersion by extra { "1.2.2" }
    val mockkVersion by extra { "1.12.8" }
    val xJunitVersion by extra { "1.1.3" }
    val xTestRunnerVersion by extra { "1.4.0" }
    val xEspressoVersion by extra { "3.4.0" }

    //dependencies
    val timber by extra { "com.jakewharton.timber:timber:$timberVersion" }
    val xAnnotation by extra { "androidx.annotation:annotation:$xAnnotationVersion" }

    //test dependencies
    val androidJunit5TestRunner by extra { "de.mannodermaus.junit5:android-test-runner:$androidJunit5TestRunnerVersion" }
    val mockk by extra { "io.mockk:mockk:$mockkVersion" }
    val xJunit by extra { "androidx.test.ext:junit:$xJunitVersion" }
    val xTestRunner by extra { "androidx.test:runner:$xTestRunnerVersion" }
    val xEspresso by extra { "androidx.test.espresso:espresso-core:$xEspressoVersion" }

    //dependency notation builders
    notation("coroutines") { module -> "org.jetbrains.kotlinx:kotlinx-coroutines-$module:$coroutinesVersion" }
    notation("compose") { module, sub ->
        "androidx.compose.$module:$module${sub.takeIf(String::isNotEmpty)?.let { "-$it" } ?: ""}"
    }

    //test dependency notation builders
    notation("spek") { module -> "org.spekframework.spek2:spek-$module:$spekVersion" }
    notation("junitJupiter") { module -> "org.junit.jupiter:junit-jupiter-$module:$junitJupiterVersion" }
}

fun Project.notation(name: String, creator: (module: String) -> Any) {
    extensions.extraProperties[name] = creator
}

fun Project.notation(name: String, creator: (module: String, subModule: String) -> Any) {
    extensions.extraProperties[name] = creator
}

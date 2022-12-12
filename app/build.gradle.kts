val compose: (String, String) -> Any by extra
dependencies {
    implementation(compose("material3", ""))

    implementation(compose("runtime", "livedata"))
}

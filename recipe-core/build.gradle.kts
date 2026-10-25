plugins { `java-library` }
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
dependencies { api("com.google.code.gson:gson:2.11.0"); testImplementation("junit:junit:4.13.2") }

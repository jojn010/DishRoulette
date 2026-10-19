plugins { `java-library` }
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
tasks.withType<JavaCompile>().configureEach { options.release.set(11) }
dependencies { testImplementation("junit:junit:4.13.2") }

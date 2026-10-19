plugins { application }
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
application { mainClass = "com.example.dishroulette.backend.RecipeServer" }
dependencies {
    implementation(project(":recipe-core"))
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation("junit:junit:4.13.2")
}
tasks.test { useJUnit() }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }

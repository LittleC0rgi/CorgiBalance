plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "4.1.1"
}

repositories {
    mavenCentral()
}

javafx {
    version = "25.0.4"
    modules = listOf("javafx.controls")
}

dependencies {
    implementation("org.openjfx:javafx-controls:25.0.4")
    implementation("org.openjfx:javafx-fxml:25.0.4")

    testImplementation(libs.junit)
    implementation(libs.guava)

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainModule = "javafx.demo"
    mainClass = "org.example.App"
}

jlink {
    imageName.set("Corgi Balance")

    launcher {
        name = "corgi-balance"
    }

    jpackage {
        imageName = "Corgi Balance"
        installerName = "Corgi Balance"
        description = "Corgi Balance is an open-source desktop application for personal finance management."
        installerType = "rpm"
        appVersion = "0.1.0"
        vendor = "Kirill Vasiliev"
        icon = "src/main/resources/icons/img.png"
        installerOptions = listOf(
            "--linux-shortcut",
            "--linux-menu-group", "Utility"
        )
    }
}
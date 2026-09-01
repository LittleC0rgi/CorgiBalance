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

    implementation("org.xerial:sqlite-jdbc:3.53.2.1")

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
    mainClass = "com.corgibalance.App"
}

jlink {
    imageName.set("CorgiBalance")

    launcher {
        name = "CorgiBalance"
    }

    jpackage {
        imageName = "CorgiBalance"
        installerName = "CorgiBalance"
        installerType = "rpm"
        appVersion = "1.0.0"
        vendor = "Kirill Vasiliev"
        icon = "src/main/resources/icons/256x256.png"
        installerOptions = listOf(
            "--description", "CorgiBalance is an open-source desktop application for personal finance management.",
            "--linux-shortcut",
            "--linux-menu-group", "Utility",
        )
    }
}

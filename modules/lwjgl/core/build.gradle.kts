/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
plugins {
    idea
    kotlin("jvm") version "1.3.61"
}

repositories {
    mavenCentral()
}

sourceSets {
    create("lwjgl.core9") {
        java {
            srcDirs("src/main/java9")
        }
    }
    create("lwjgl.core10") {
        java {
            srcDirs("src/main/java10")
        }
    }
    create("templates.core") {
        java.srcDir("src/templates/kotlin")

        dependencies {
            implementation(kotlin("stdlib"))
            implementation(project(":modules:generator"))
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":modules:generator"))
}

//kotlin {
//    sourceSets.create("templates.core") {
//        kotlin.srcDir("src/templates/kotlin")
//
//        dependencies {
//            implementation(kotlin("stdlib"))
//        }
//    }
//}
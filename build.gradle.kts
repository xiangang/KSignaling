import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc


val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val mysql_version: String by project
val protobuf_version: String by project
val log4j2_version: String by project
val swagger_codegen_version: String by project


plugins {
    kotlin("jvm") version "1.8.20"
    id("io.ktor.plugin") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
    id("com.google.protobuf") version "0.8.17"
}

group = "com.nxg"
version = "0.0.1"
application {
    mainClass.set("com.nxg.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-gson-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-protobuf:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-swagger:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-compression-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-swagger:$ktor_version")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("io.swagger.codegen.v3:swagger-codegen-generators:$swagger_codegen_version")
    //和swagger中的ch.qos.logback:logback-classic:1.2.10冲突了，暂时不用
    //implementation("org.apache.logging.log4j:log4j-core:$log4j2_version")
    //implementation("org.apache.logging.log4j:log4j-api:$log4j2_version")
    //implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2_version")
    implementation("com.rabbitmq:amqp-client:5.18.0")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jodatime:$exposed_version")
    implementation("mysql:mysql-connector-java:$mysql_version")
    implementation("com.google.protobuf:protobuf-javalite:$protobuf_version")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("io.lettuce:lettuce-core:6.2.4.RELEASE")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.14.0"
    }

    // Generates the java Protobuf-lite code for the Protobufs in this project. See
    // https://github.com/google/protobuf-gradle-plugin#customizing-protobuf-compilation
    // for more information.
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                getByName("java") {
                    option("lite")
                }
            }
        }
    }
}

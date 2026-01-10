# LinkOps 구현 아키텍처 문서

## 목차
1. [프로젝트 구조 및 모듈 조직](#1-프로젝트-구조-및-모듈-조직)
2. [계층별 상세 컴포넌트 설계](#2-계층별-상세-컴포넌트-설계)
3. [데이터 플로우 다이어그램](#3-데이터-플로우-다이어그램)
4. [핵심 인터페이스 및 추상화](#4-핵심-인터페이스-및-추상화)
5. [의존성 주입 전략](#5-의존성-주입-전략)
6. [에러 핸들링 패턴](#6-에러-핸들링-패턴)
7. [테스팅 전략](#7-테스팅-전략)
8. [빌드 시퀀스 및 구현 순서](#8-빌드-시퀀스-및-구현-순서)

---

## 1. 프로젝트 구조 및 모듈 조직

### 1.1 디렉토리 구조

```
link-ops/
├── buildSrc/                          # 빌드 설정 및 버전 관리
│   └── src/main/kotlin/
│       ├── Dependencies.kt            # 의존성 버전 중앙 관리
│       └── BuildConfig.kt             # 빌드 설정
├── core/                              # 핵심 모듈
│   ├── domain/                        # 도메인 로직 (순수 Kotlin)
│   │   └── src/main/kotlin/
│   │       ├── model/                 # 도메인 모델
│   │       │   ├── Device.kt
│   │       │   ├── AppLink.kt
│   │       │   ├── IntentConfig.kt
│   │       │   ├── AssetLinks.kt
│   │       │   └── VerificationState.kt
│   │       ├── repository/            # Repository 인터페이스
│   │       │   ├── DeviceRepository.kt
│   │       │   ├── AppLinkRepository.kt
│   │       │   ├── AssetLinksRepository.kt
│   │       │   └── ManifestRepository.kt
│   │       └── usecase/               # 비즈니스 로직
│   │           ├── device/
│   │           │   ├── DetectDevicesUseCase.kt
│   │           │   └── GetDeviceInfoUseCase.kt
│   │           ├── applink/
│   │           │   ├── GetAppLinksUseCase.kt
│   │           │   ├── FireIntentUseCase.kt
│   │           │   └── ForceReverifyUseCase.kt
│   │           ├── diagnostics/
│   │           │   ├── ValidateAssetLinksUseCase.kt
│   │           │   └── CheckFingerprintMatchUseCase.kt
│   │           └── manifest/
│   │               └── ParseManifestUseCase.kt
│   ├── data/                          # 데이터 레이어
│   │   └── src/main/kotlin/
│   │       ├── repository/            # Repository 구현
│   │       │   ├── DeviceRepositoryImpl.kt
│   │       │   ├── AppLinkRepositoryImpl.kt
│   │       │   ├── AssetLinksRepositoryImpl.kt
│   │       │   └── ManifestRepositoryImpl.kt
│   │       ├── datasource/            # 데이터 소스
│   │       │   ├── adb/
│   │       │   │   ├── AdbDataSource.kt
│   │       │   │   └── AdbCommandFactory.kt
│   │       │   ├── network/
│   │       │   │   └── AssetLinksDataSource.kt
│   │       │   └── local/
│   │       │       └── FileSystemDataSource.kt
│   │       └── mapper/                # DTO ↔ Domain 변환
│   │           ├── DeviceMapper.kt
│   │           ├── AppLinkMapper.kt
│   │           └── AssetLinksMapper.kt
│   └── infrastructure/                # 인프라 레이어
│       └── src/main/kotlin/
│           ├── adb/                   # ADB 통신
│           │   ├── AdbClient.kt
│           │   ├── AdbShellExecutor.kt
│           │   ├── AdbBinaryManager.kt
│           │   ├── strategy/
│           │   │   ├── AdbCommandStrategy.kt
│           │   │   ├── Android11Strategy.kt
│           │   │   └── Android12PlusStrategy.kt
│           │   └── parser/
│           │       ├── GetAppLinksParser.kt
│           │       ├── DumpsysParser.kt
│           │       └── LogcatParser.kt
│           ├── network/               # 네트워크 클라이언트
│           │   ├── KtorClientFactory.kt
│           │   └── AssetLinksClient.kt
│           └── filesystem/            # 파일 시스템
│               ├── ApkParser.kt
│               └── ManifestParser.kt
├── presentation/                      # UI 레이어
│   └── src/main/kotlin/
│       ├── ui/
│       │   ├── theme/                 # 테마 및 디자인 시스템
│       │   │   ├── Color.kt
│       │   │   ├── Typography.kt
│       │   │   └── Theme.kt
│       │   ├── component/             # 재사용 가능한 컴포넌트
│       │   │   ├── DeviceCard.kt
│       │   │   ├── AppLinkItem.kt
│       │   │   ├── StatusBadge.kt
│       │   │   ├── IntentFireDialog.kt
│       │   │   └── LoadingOverlay.kt
│       │   └── screen/                # 화면
│       │       ├── main/
│       │       │   ├── MainScreen.kt
│       │       │   └── MainViewModel.kt
│       │       ├── device/
│       │       │   ├── DeviceSelectionScreen.kt
│       │       │   └── DeviceSelectionViewModel.kt
│       │       ├── dashboard/
│       │       │   ├── DashboardScreen.kt
│       │       │   └── DashboardViewModel.kt
│       │       ├── diagnostics/
│       │       │   ├── DiagnosticsScreen.kt
│       │       │   └── DiagnosticsViewModel.kt
│       │       └── manifest/
│       │           ├── ManifestAnalyzerScreen.kt
│       │           └── ManifestAnalyzerViewModel.kt
│       ├── navigation/
│       │   ├── NavGraph.kt
│       │   └── Screen.kt
│       └── App.kt
├── di/                                # 의존성 주입
│   └── src/main/kotlin/
│       ├── AppModule.kt
│       ├── DataModule.kt
│       ├── DomainModule.kt
│       └── InfrastructureModule.kt
└── desktop/                           # 데스크톱 진입점
    └── src/main/kotlin/
        └── Main.kt
```

### 1.2 Gradle 모듈 구성

**settings.gradle.kts**
```kotlin
rootProject.name = "link-ops"

include(
    ":core:domain",
    ":core:data",
    ":core:infrastructure",
    ":presentation",
    ":di",
    ":desktop"
)
```

**모듈 의존성 그래프**
```
desktop
  └─> presentation
  └─> di
        └─> core:data
        └─> core:domain
        └─> core:infrastructure

presentation
  └─> core:domain

core:data
  └─> core:domain
  └─> core:infrastructure

core:infrastructure
  (외부 의존성만)

core:domain
  (순수 Kotlin, 외부 의존성 없음)
```

---

## 2. 계층별 상세 컴포넌트 설계

### 2.1 Domain Layer (core/domain)

#### 2.1.1 Domain Models

**/core/domain/src/main/kotlin/model/Device.kt**
```kotlin
package com.linkops.core.domain.model

data class Device(
    val serialNumber: String,
    val model: String,
    val osVersion: String,
    val sdkLevel: Int,
    val connectionType: ConnectionType,
    val state: DeviceState
) {
    enum class ConnectionType {
        USB, WIFI, EMULATOR
    }

    enum class DeviceState {
        ONLINE, OFFLINE, UNAUTHORIZED, UNKNOWN
    }

    val displayName: String
        get() = "$model (Android $osVersion)"

    val isAvailable: Boolean
        get() = state == DeviceState.ONLINE
}
```

**/core/domain/src/main/kotlin/model/AppLink.kt**
```kotlin
package com.linkops.core.domain.model

data class AppLink(
    val packageName: String,
    val domains: List<DomainVerification>
)

data class DomainVerification(
    val domain: String,
    val verificationState: VerificationState,
    val fingerprint: String?
)

enum class VerificationState {
    VERIFIED,           // Android 12+ "verified"
    APPROVED,           // Android 11 "always"
    DENIED,             // Android 11 "never"
    UNVERIFIED,         // Android 12+ "none"
    LEGACY_FAILURE,     // Android 11 실패
    UNKNOWN;

    val isSuccessful: Boolean
        get() = this == VERIFIED || this == APPROVED
}
```

**/core/domain/src/main/kotlin/model/IntentConfig.kt**
```kotlin
package com.linkops.core.domain.model

data class IntentConfig(
    val uri: String,
    val action: String = "android.intent.action.VIEW",
    val flags: Set<IntentFlag> = emptySet(),
    val packageName: String? = null
) {
    enum class IntentFlag(val value: String) {
        ACTIVITY_NEW_TASK("--activity-new-task"),
        ACTIVITY_CLEAR_TOP("--activity-clear-top"),
        ACTIVITY_SINGLE_TOP("--activity-single-top"),
        ACTIVITY_CLEAR_TASK("--activity-clear-task")
    }

    fun toAdbCommand(): String {
        val flagsStr = flags.joinToString(" ") { it.value }
        val pkgStr = packageName?.let { "-p $it" } ?: ""
        return "am start -a $action -d \"$uri\" $flagsStr $pkgStr".trim()
    }
}
```

**/core/domain/src/main/kotlin/model/AssetLinks.kt**
```kotlin
package com.linkops.core.domain.model

data class AssetLinksValidation(
    val domain: String,
    val url: String,
    val status: ValidationStatus,
    val issues: List<ValidationIssue> = emptyList(),
    val content: AssetLinksContent? = null
)

data class AssetLinksContent(
    val statements: List<Statement>
) {
    data class Statement(
        val relation: List<String>,
        val target: Target
    ) {
        data class Target(
            val namespace: String,
            val packageName: String,
            val sha256CertFingerprints: List<String>
        )
    }
}

enum class ValidationStatus {
    VALID,
    INVALID_JSON,
    NOT_FOUND,
    REDIRECT,
    NETWORK_ERROR,
    FINGERPRINT_MISMATCH
}

data class ValidationIssue(
    val severity: Severity,
    val message: String,
    val details: String? = null
) {
    enum class Severity {
        ERROR, WARNING, INFO
    }
}
```

#### 2.1.2 Repository Interfaces

**/core/domain/src/main/kotlin/repository/DeviceRepository.kt**
```kotlin
package com.linkops.core.domain.repository

import com.linkops.core.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    /**
     * 연결된 디바이스 목록을 실시간으로 감지
     * @return Device Flow (새 디바이스 연결/해제 시 emit)
     */
    fun observeDevices(): Flow<List<Device>>

    /**
     * 특정 디바이스 정보 조회
     */
    suspend fun getDevice(serialNumber: String): Result<Device>

    /**
     * ADB 서버 시작 여부 확인
     */
    suspend fun isAdbAvailable(): Boolean
}
```

**/core/domain/src/main/kotlin/repository/AppLinkRepository.kt**
```kotlin
package com.linkops.core.domain.repository

import com.linkops.core.domain.model.AppLink
import com.linkops.core.domain.model.IntentConfig
import kotlinx.coroutines.flow.Flow

interface AppLinkRepository {
    /**
     * 디바이스의 앱 링크 설정 조회
     * @param device 대상 디바이스
     * @return AppLink 목록
     */
    suspend fun getAppLinks(deviceSerial: String): Result<List<AppLink>>

    /**
     * Intent 실행
     * @return stdout/stderr 로그 스트림
     */
    fun fireIntent(
        deviceSerial: String,
        config: IntentConfig
    ): Flow<String>

    /**
     * 앱 링크 재검증 강제 실행
     */
    suspend fun forceReverify(
        deviceSerial: String,
        packageName: String
    ): Result<Unit>

    /**
     * Logcat에서 검증 로그 스트리밍
     */
    fun observeVerificationLogs(deviceSerial: String): Flow<String>
}
```

**/core/domain/src/main/kotlin/repository/AssetLinksRepository.kt**
```kotlin
package com.linkops.core.domain.repository

import com.linkops.core.domain.model.AssetLinksValidation

interface AssetLinksRepository {
    /**
     * assetlinks.json 검증
     * @param domain 도메인 (예: "example.com")
     * @return 검증 결과
     */
    suspend fun validateAssetLinks(domain: String): Result<AssetLinksValidation>

    /**
     * SHA-256 핑거프린트 매칭 검증
     */
    suspend fun checkFingerprintMatch(
        domain: String,
        expectedFingerprints: List<String>
    ): Result<Boolean>
}
```

**/core/domain/src/main/kotlin/repository/ManifestRepository.kt**
```kotlin
package com.linkops.core.domain.repository

import com.linkops.core.domain.model.ManifestDeepLink
import java.io.File

data class ManifestDeepLink(
    val scheme: String?,
    val host: String?,
    val pathPattern: String?,
    val autoVerify: Boolean,
    val activityName: String
)

interface ManifestRepository {
    /**
     * APK 파일에서 AndroidManifest.xml 파싱
     */
    suspend fun parseApk(apkFile: File): Result<List<ManifestDeepLink>>

    /**
     * 프로젝트 폴더에서 AndroidManifest.xml 파싱
     */
    suspend fun parseProject(projectDir: File): Result<List<ManifestDeepLink>>
}
```

#### 2.1.3 Use Cases

**/core/domain/src/main/kotlin/usecase/device/DetectDevicesUseCase.kt**
```kotlin
package com.linkops.core.domain.usecase.device

import com.linkops.core.domain.model.Device
import com.linkops.core.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow

class DetectDevicesUseCase(
    private val deviceRepository: DeviceRepository
) {
    operator fun invoke(): Flow<List<Device>> {
        return deviceRepository.observeDevices()
    }
}
```

**/core/domain/src/main/kotlin/usecase/applink/GetAppLinksUseCase.kt**
```kotlin
package com.linkops.core.domain.usecase.applink

import com.linkops.core.domain.model.AppLink
import com.linkops.core.domain.repository.AppLinkRepository

class GetAppLinksUseCase(
    private val appLinkRepository: AppLinkRepository
) {
    suspend operator fun invoke(deviceSerial: String): Result<List<AppLink>> {
        return appLinkRepository.getAppLinks(deviceSerial)
    }
}
```

**/core/domain/src/main/kotlin/usecase/applink/FireIntentUseCase.kt**
```kotlin
package com.linkops.core.domain.usecase.applink

import com.linkops.core.domain.model.IntentConfig
import com.linkops.core.domain.repository.AppLinkRepository
import kotlinx.coroutines.flow.Flow

class FireIntentUseCase(
    private val appLinkRepository: AppLinkRepository
) {
    operator fun invoke(
        deviceSerial: String,
        config: IntentConfig
    ): Flow<String> {
        return appLinkRepository.fireIntent(deviceSerial, config)
    }
}
```

**/core/domain/src/main/kotlin/usecase/diagnostics/ValidateAssetLinksUseCase.kt**
```kotlin
package com.linkops.core.domain.usecase.diagnostics

import com.linkops.core.domain.model.AssetLinksValidation
import com.linkops.core.domain.repository.AssetLinksRepository

class ValidateAssetLinksUseCase(
    private val assetLinksRepository: AssetLinksRepository
) {
    suspend operator fun invoke(domain: String): Result<AssetLinksValidation> {
        return assetLinksRepository.validateAssetLinks(domain)
    }
}
```

---

### 2.2 Infrastructure Layer (core/infrastructure)

#### 2.2.1 ADB Client

**/core/infrastructure/src/main/kotlin/adb/AdbShellExecutor.kt**
```kotlin
package com.linkops.core.infrastructure.adb

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ADB 명령어 실행 및 스트리밍 처리
 */
class AdbShellExecutor(
    private val adbBinaryManager: AdbBinaryManager
) {
    /**
     * ADB 명령어 실행 (일회성)
     * @return stdout 결과
     */
    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val adbPath = adbBinaryManager.getAdbPath()
            val process = ProcessBuilder(adbPath, *command.split(" ").toTypedArray())
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw AdbException("Command failed: $command\nOutput: $output")
            }

            output
        }
    }

    /**
     * ADB 명령어 실행 (스트리밍)
     * @return stdout Flow
     */
    fun executeStream(command: String): Flow<String> = flow {
        val adbPath = adbBinaryManager.getAdbPath()
        val process = ProcessBuilder(adbPath, *command.split(" ").toTypedArray())
            .redirectErrorStream(true)
            .start()

        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
            }
        }
    }

    /**
     * 디바이스별 ADB 명령어 실행
     */
    suspend fun executeOnDevice(
        serialNumber: String,
        shellCommand: String
    ): Result<String> {
        return execute("-s $serialNumber shell $shellCommand")
    }
}

class AdbException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

**/core/infrastructure/src/main/kotlin/adb/AdbBinaryManager.kt**
```kotlin
package com.linkops.core.infrastructure.adb

import java.io.File
import java.net.URL

/**
 * ADB 바이너리 자동 다운로드 및 관리
 * - 시스템에 ADB가 설치되어 있으면 사용
 * - 없으면 Google SDK 저장소에서 다운로드
 */
class AdbBinaryManager {
    private val os: String = System.getProperty("os.name").lowercase()
    private val adbDir = File(System.getProperty("user.home"), ".linkops/adb")

    fun getAdbPath(): String {
        // 1. 시스템 PATH에서 ADB 찾기
        val systemAdb = findSystemAdb()
        if (systemAdb != null) return systemAdb

        // 2. 내장 ADB 디렉토리 확인
        val bundledAdb = File(adbDir, adbBinaryName)
        if (bundledAdb.exists()) return bundledAdb.absolutePath

        // 3. ADB 다운로드
        downloadAdb()
        return bundledAdb.absolutePath
    }

    private fun findSystemAdb(): String? {
        return try {
            val process = ProcessBuilder("which", "adb").start()
            val path = process.inputStream.bufferedReader().readText().trim()
            if (path.isNotEmpty()) path else null
        } catch (e: Exception) {
            null
        }
    }

    private fun downloadAdb() {
        adbDir.mkdirs()

        val downloadUrl = when {
            os.contains("mac") -> ADB_URL_MAC
            os.contains("win") -> ADB_URL_WINDOWS
            os.contains("linux") -> ADB_URL_LINUX
            else -> throw UnsupportedOperationException("Unsupported OS: $os")
        }

        // ZIP 다운로드 및 압축 해제 로직
        // (실제 구현 시 Ktor 또는 okio 사용)
        TODO("Implement ZIP download and extraction")
    }

    private val adbBinaryName: String
        get() = if (os.contains("win")) "adb.exe" else "adb"

    companion object {
        private const val ADB_URL_MAC = "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
        private const val ADB_URL_WINDOWS = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
        private const val ADB_URL_LINUX = "https://dl.google.com/android/repository/platform-tools-latest-linux.zip"
    }
}
```

#### 2.2.2 Command Strategy Pattern (Android 버전별 분기)

**/core/infrastructure/src/main/kotlin/adb/strategy/AdbCommandStrategy.kt**
```kotlin
package com.linkops.core.infrastructure.adb.strategy

import com.linkops.core.infrastructure.adb.AdbShellExecutor

/**
 * Android 버전별 ADB 명령어 전략
 */
interface AdbCommandStrategy {
    suspend fun getAppLinksCommand(packageName: String?): String
    suspend fun forceReverifyCommand(packageName: String): String
}

/**
 * Android 11 이하 (SDK <= 30)
 */
class Android11Strategy : AdbCommandStrategy {
    override suspend fun getAppLinksCommand(packageName: String?): String {
        return if (packageName != null) {
            "dumpsys package domain-preferred-apps $packageName"
        } else {
            "dumpsys package domain-preferred-apps"
        }
    }

    override suspend fun forceReverifyCommand(packageName: String): String {
        return "pm set-app-links --package $packageName 0"
    }
}

/**
 * Android 12+ (SDK >= 31)
 */
class Android12PlusStrategy : AdbCommandStrategy {
    override suspend fun getAppLinksCommand(packageName: String?): String {
        return if (packageName != null) {
            "pm get-app-links $packageName"
        } else {
            "pm get-app-links"
        }
    }

    override suspend fun forceReverifyCommand(packageName: String): String {
        return "pm verify-app-links --re-verify $packageName"
    }
}

/**
 * SDK 레벨에 따라 적절한 전략 선택
 */
class AdbCommandStrategyFactory {
    fun create(sdkLevel: Int): AdbCommandStrategy {
        return if (sdkLevel >= 31) {
            Android12PlusStrategy()
        } else {
            Android11Strategy()
        }
    }
}
```

#### 2.2.3 Parser

**/core/infrastructure/src/main/kotlin/adb/parser/GetAppLinksParser.kt**
```kotlin
package com.linkops.core.infrastructure.adb.parser

import com.linkops.core.domain.model.AppLink
import com.linkops.core.domain.model.DomainVerification
import com.linkops.core.domain.model.VerificationState

/**
 * `pm get-app-links` 출력 파싱 (Android 12+)
 *
 * 예시 출력:
 * com.example.app:
 *   ID: 12345678-1234-1234-1234-123456789012
 *   Signatures: [AB:CD:EF:...]
 *   Domain verification state:
 *     example.com: verified
 *     test.example.com: none
 */
class GetAppLinksParser {
    fun parse(output: String): List<AppLink> {
        val appLinks = mutableListOf<AppLink>()
        var currentPackage: String? = null
        val currentDomains = mutableListOf<DomainVerification>()
        var inDomainSection = false

        output.lines().forEach { line ->
            when {
                // 패키지명 감지 (예: "com.example.app:")
                line.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+:$")) -> {
                    // 이전 패키지 저장
                    if (currentPackage != null && currentDomains.isNotEmpty()) {
                        appLinks.add(AppLink(currentPackage!!, currentDomains.toList()))
                    }

                    currentPackage = line.removeSuffix(":")
                    currentDomains.clear()
                    inDomainSection = false
                }

                // Domain verification state 섹션 시작
                line.trim() == "Domain verification state:" -> {
                    inDomainSection = true
                }

                // 도메인 상태 파싱 (예: "  example.com: verified")
                inDomainSection && line.trim().isNotEmpty() -> {
                    val parts = line.trim().split(":")
                    if (parts.size == 2) {
                        val domain = parts[0].trim()
                        val state = parseVerificationState(parts[1].trim())
                        currentDomains.add(DomainVerification(domain, state, null))
                    }
                }
            }
        }

        // 마지막 패키지 저장
        if (currentPackage != null && currentDomains.isNotEmpty()) {
            appLinks.add(AppLink(currentPackage!!, currentDomains.toList()))
        }

        return appLinks
    }

    private fun parseVerificationState(state: String): VerificationState {
        return when (state.lowercase()) {
            "verified" -> VerificationState.VERIFIED
            "none" -> VerificationState.UNVERIFIED
            else -> VerificationState.UNKNOWN
        }
    }
}
```

**/core/infrastructure/src/main/kotlin/adb/parser/DumpsysParser.kt**
```kotlin
package com.linkops.core.infrastructure.adb.parser

import com.linkops.core.domain.model.AppLink
import com.linkops.core.domain.model.DomainVerification
import com.linkops.core.domain.model.VerificationState

/**
 * `dumpsys package domain-preferred-apps` 출력 파싱 (Android 11 이하)
 *
 * 예시 출력:
 * App linkages for user 0:
 * Package: com.example.app
 *   Domains: example.com test.example.com
 *   Status: always : 200000001
 */
class DumpsysParser {
    fun parse(output: String): List<AppLink> {
        val appLinks = mutableListOf<AppLink>()
        var currentPackage: String? = null
        var currentDomains: List<String>? = null
        var currentStatus: VerificationState? = null

        output.lines().forEach { line ->
            when {
                line.trim().startsWith("Package:") -> {
                    currentPackage = line.substringAfter("Package:").trim()
                }

                line.trim().startsWith("Domains:") -> {
                    currentDomains = line.substringAfter("Domains:")
                        .trim()
                        .split(" ")
                        .filter { it.isNotEmpty() }
                }

                line.trim().startsWith("Status:") -> {
                    val status = line.substringAfter("Status:").trim()
                    currentStatus = when {
                        status.startsWith("always") -> VerificationState.APPROVED
                        status.startsWith("never") -> VerificationState.DENIED
                        else -> VerificationState.LEGACY_FAILURE
                    }

                    // AppLink 객체 생성
                    if (currentPackage != null && currentDomains != null && currentStatus != null) {
                        val domainVerifications = currentDomains!!.map { domain ->
                            DomainVerification(domain, currentStatus!!, null)
                        }
                        appLinks.add(AppLink(currentPackage!!, domainVerifications))

                        // 초기화
                        currentPackage = null
                        currentDomains = null
                        currentStatus = null
                    }
                }
            }
        }

        return appLinks
    }
}
```

#### 2.2.4 Network Client

**/core/infrastructure/src/main/kotlin/network/AssetLinksClient.kt**
```kotlin
package com.linkops.core.infrastructure.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * assetlinks.json 가져오기
 */
class AssetLinksClient(
    private val httpClient: HttpClient,
    private val json: Json
) {
    suspend fun fetch(domain: String): AssetLinksResponse {
        val url = "https://$domain/.well-known/assetlinks.json"

        return try {
            val response: HttpResponse = httpClient.get(url) {
                header(HttpHeaders.Accept, "application/json")
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.bodyAsText()
                    AssetLinksResponse.Success(
                        content = body,
                        wasRedirected = response.request.url.host != domain
                    )
                }
                HttpStatusCode.NotFound -> AssetLinksResponse.NotFound
                HttpStatusCode.MovedPermanently,
                HttpStatusCode.Found -> AssetLinksResponse.Redirect(response.headers[HttpHeaders.Location])
                else -> AssetLinksResponse.Error("HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            AssetLinksResponse.NetworkError(e.message ?: "Unknown error")
        }
    }
}

sealed class AssetLinksResponse {
    data class Success(val content: String, val wasRedirected: Boolean) : AssetLinksResponse()
    object NotFound : AssetLinksResponse()
    data class Redirect(val location: String?) : AssetLinksResponse()
    data class Error(val message: String) : AssetLinksResponse()
    data class NetworkError(val message: String) : AssetLinksResponse()
}

@Serializable
data class AssetLinksDto(
    val relation: List<String>,
    val target: Target
) {
    @Serializable
    data class Target(
        val namespace: String,
        val package_name: String,
        val sha256_cert_fingerprints: List<String>
    )
}
```

---

### 2.3 Data Layer (core/data)

#### 2.3.1 Repository Implementation

**/core/data/src/main/kotlin/repository/DeviceRepositoryImpl.kt**
```kotlin
package com.linkops.core.data.repository

import com.linkops.core.domain.model.Device
import com.linkops.core.domain.repository.DeviceRepository
import com.linkops.core.infrastructure.adb.AdbShellExecutor
import com.linkops.core.data.mapper.DeviceMapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class DeviceRepositoryImpl(
    private val adbExecutor: AdbShellExecutor,
    private val deviceMapper: DeviceMapper
) : DeviceRepository {

    override fun observeDevices(): Flow<List<Device>> = flow {
        while (true) {
            val devices = fetchDevices()
            emit(devices)
            delay(2.seconds) // 2초마다 폴링
        }
    }.distinctUntilChanged()

    override suspend fun getDevice(serialNumber: String): Result<Device> {
        return adbExecutor.execute("devices -l")
            .mapCatching { output ->
                val devices = deviceMapper.parseDeviceList(output)
                devices.firstOrNull { it.serialNumber == serialNumber }
                    ?: throw DeviceNotFoundException(serialNumber)
            }
    }

    override suspend fun isAdbAvailable(): Boolean {
        return adbExecutor.execute("version").isSuccess
    }

    private suspend fun fetchDevices(): List<Device> {
        return adbExecutor.execute("devices -l")
            .map { deviceMapper.parseDeviceList(it) }
            .getOrElse { emptyList() }
    }
}

class DeviceNotFoundException(serialNumber: String) :
    Exception("Device not found: $serialNumber")
```

**/core/data/src/main/kotlin/repository/AppLinkRepositoryImpl.kt**
```kotlin
package com.linkops.core.data.repository

import com.linkops.core.domain.model.AppLink
import com.linkops.core.domain.model.IntentConfig
import com.linkops.core.domain.repository.AppLinkRepository
import com.linkops.core.infrastructure.adb.AdbShellExecutor
import com.linkops.core.infrastructure.adb.strategy.AdbCommandStrategyFactory
import com.linkops.core.infrastructure.adb.parser.GetAppLinksParser
import com.linkops.core.infrastructure.adb.parser.DumpsysParser
import kotlinx.coroutines.flow.Flow

class AppLinkRepositoryImpl(
    private val adbExecutor: AdbShellExecutor,
    private val strategyFactory: AdbCommandStrategyFactory,
    private val getAppLinksParser: GetAppLinksParser,
    private val dumpsysParser: DumpsysParser
) : AppLinkRepository {

    override suspend fun getAppLinks(deviceSerial: String): Result<List<AppLink>> {
        // 1. 디바이스 SDK 레벨 확인
        val sdkLevel = adbExecutor
            .executeOnDevice(deviceSerial, "getprop ro.build.version.sdk")
            .map { it.trim().toInt() }
            .getOrElse { return Result.failure(it) }

        // 2. 전략 선택
        val strategy = strategyFactory.create(sdkLevel)
        val command = strategy.getAppLinksCommand(packageName = null)

        // 3. 명령어 실행 및 파싱
        return adbExecutor.executeOnDevice(deviceSerial, command)
            .mapCatching { output ->
                if (sdkLevel >= 31) {
                    getAppLinksParser.parse(output)
                } else {
                    dumpsysParser.parse(output)
                }
            }
    }

    override fun fireIntent(
        deviceSerial: String,
        config: IntentConfig
    ): Flow<String> {
        val command = config.toAdbCommand()
        return adbExecutor.executeStream("-s $deviceSerial shell $command")
    }

    override suspend fun forceReverify(
        deviceSerial: String,
        packageName: String
    ): Result<Unit> {
        val sdkLevel = adbExecutor
            .executeOnDevice(deviceSerial, "getprop ro.build.version.sdk")
            .map { it.trim().toInt() }
            .getOrElse { return Result.failure(it) }

        val strategy = strategyFactory.create(sdkLevel)
        val command = strategy.forceReverifyCommand(packageName)

        return adbExecutor.executeOnDevice(deviceSerial, command)
            .map { }
    }

    override fun observeVerificationLogs(deviceSerial: String): Flow<String> {
        return adbExecutor.executeStream(
            "-s $deviceSerial logcat -v time ActivityTaskManager:I IntentFilter:I *:S"
        )
    }
}
```

**/core/data/src/main/kotlin/repository/AssetLinksRepositoryImpl.kt**
```kotlin
package com.linkops.core.data.repository

import com.linkops.core.domain.model.*
import com.linkops.core.domain.repository.AssetLinksRepository
import com.linkops.core.infrastructure.network.AssetLinksClient
import com.linkops.core.infrastructure.network.AssetLinksResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class AssetLinksRepositoryImpl(
    private val assetLinksClient: AssetLinksClient,
    private val json: Json
) : AssetLinksRepository {

    override suspend fun validateAssetLinks(domain: String): Result<AssetLinksValidation> {
        val url = "https://$domain/.well-known/assetlinks.json"

        return runCatching {
            when (val response = assetLinksClient.fetch(domain)) {
                is AssetLinksResponse.Success -> {
                    try {
                        val content = json.decodeFromString<List<AssetLinksDto>>(response.content)
                        val assetLinksContent = content.map { dto ->
                            AssetLinksContent.Statement(
                                relation = dto.relation,
                                target = AssetLinksContent.Statement.Target(
                                    namespace = dto.target.namespace,
                                    packageName = dto.target.package_name,
                                    sha256CertFingerprints = dto.target.sha256_cert_fingerprints
                                )
                            )
                        }

                        val status = if (response.wasRedirected) {
                            ValidationStatus.REDIRECT
                        } else {
                            ValidationStatus.VALID
                        }

                        AssetLinksValidation(
                            domain = domain,
                            url = url,
                            status = status,
                            issues = if (response.wasRedirected) {
                                listOf(ValidationIssue(
                                    severity = ValidationIssue.Severity.WARNING,
                                    message = "Domain was redirected"
                                ))
                            } else emptyList(),
                            content = AssetLinksContent(assetLinksContent)
                        )
                    } catch (e: Exception) {
                        AssetLinksValidation(
                            domain = domain,
                            url = url,
                            status = ValidationStatus.INVALID_JSON,
                            issues = listOf(ValidationIssue(
                                severity = ValidationIssue.Severity.ERROR,
                                message = "Invalid JSON format",
                                details = e.message
                            ))
                        )
                    }
                }

                is AssetLinksResponse.NotFound -> {
                    AssetLinksValidation(
                        domain = domain,
                        url = url,
                        status = ValidationStatus.NOT_FOUND,
                        issues = listOf(ValidationIssue(
                            severity = ValidationIssue.Severity.ERROR,
                            message = "assetlinks.json not found"
                        ))
                    )
                }

                is AssetLinksResponse.Redirect -> {
                    AssetLinksValidation(
                        domain = domain,
                        url = url,
                        status = ValidationStatus.REDIRECT,
                        issues = listOf(ValidationIssue(
                            severity = ValidationIssue.Severity.WARNING,
                            message = "Redirected to ${response.location}"
                        ))
                    )
                }

                is AssetLinksResponse.Error -> {
                    AssetLinksValidation(
                        domain = domain,
                        url = url,
                        status = ValidationStatus.NETWORK_ERROR,
                        issues = listOf(ValidationIssue(
                            severity = ValidationIssue.Severity.ERROR,
                            message = response.message
                        ))
                    )
                }

                is AssetLinksResponse.NetworkError -> {
                    AssetLinksValidation(
                        domain = domain,
                        url = url,
                        status = ValidationStatus.NETWORK_ERROR,
                        issues = listOf(ValidationIssue(
                            severity = ValidationIssue.Severity.ERROR,
                            message = response.message
                        ))
                    )
                }
            }
        }
    }

    override suspend fun checkFingerprintMatch(
        domain: String,
        expectedFingerprints: List<String>
    ): Result<Boolean> {
        return validateAssetLinks(domain).mapCatching { validation ->
            if (validation.content == null) return@mapCatching false

            val actualFingerprints = validation.content.statements
                .flatMap { it.target.sha256CertFingerprints }
                .map { it.uppercase().replace(":", "") }

            val normalizedExpected = expectedFingerprints
                .map { it.uppercase().replace(":", "") }

            normalizedExpected.all { it in actualFingerprints }
        }
    }
}
```

#### 2.3.2 Mapper

**/core/data/src/main/kotlin/mapper/DeviceMapper.kt**
```kotlin
package com.linkops.core.data.mapper

import com.linkops.core.domain.model.Device

/**
 * ADB 출력 → Domain Model 변환
 */
class DeviceMapper {
    /**
     * `adb devices -l` 출력 파싱
     *
     * 예시:
     * List of devices attached
     * emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emulator64_arm64 transport_id:1
     * 192.168.1.100:5555     device product:OnePlus7Pro model:GM1917 device:OnePlus7Pro transport_id:2
     */
    fun parseDeviceList(output: String): List<Device> {
        return output.lines()
            .drop(1) // "List of devices attached" 제거
            .filter { it.isNotBlank() && it.contains("device") }
            .mapNotNull { line ->
                try {
                    parseDeviceLine(line)
                } catch (e: Exception) {
                    null
                }
            }
    }

    private fun parseDeviceLine(line: String): Device {
        val parts = line.split(Regex("\\s+"))
        val serialNumber = parts[0]
        val state = parseDeviceState(parts.getOrNull(1) ?: "unknown")

        // 속성 파싱 (model:xxx product:xxx 형식)
        val attributes = parts.drop(2).associate { attr ->
            val (key, value) = attr.split(":")
            key to value
        }

        val model = attributes["model"] ?: "Unknown"
        val connectionType = when {
            serialNumber.contains("emulator") -> Device.ConnectionType.EMULATOR
            serialNumber.contains(":") -> Device.ConnectionType.WIFI
            else -> Device.ConnectionType.USB
        }

        return Device(
            serialNumber = serialNumber,
            model = model,
            osVersion = "Unknown", // 별도 명령어로 조회 필요
            sdkLevel = 0,
            connectionType = connectionType,
            state = state
        )
    }

    private fun parseDeviceState(state: String): Device.DeviceState {
        return when (state.lowercase()) {
            "device" -> Device.DeviceState.ONLINE
            "offline" -> Device.DeviceState.OFFLINE
            "unauthorized" -> Device.DeviceState.UNAUTHORIZED
            else -> Device.DeviceState.UNKNOWN
        }
    }
}
```

---

### 2.4 Presentation Layer (presentation)

#### 2.4.1 ViewModel 예시

**/presentation/src/main/kotlin/ui/screen/dashboard/DashboardViewModel.kt**
```kotlin
package com.linkops.presentation.ui.screen.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.linkops.core.domain.model.AppLink
import com.linkops.core.domain.model.Device
import com.linkops.core.domain.model.IntentConfig
import com.linkops.core.domain.usecase.applink.GetAppLinksUseCase
import com.linkops.core.domain.usecase.applink.FireIntentUseCase
import com.linkops.core.domain.usecase.applink.ForceReverifyUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getAppLinksUseCase: GetAppLinksUseCase,
    private val fireIntentUseCase: FireIntentUseCase,
    private val forceReverifyUseCase: ForceReverifyUseCase
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState

    var selectedDevice: Device? by mutableStateOf(null)
        private set

    fun loadAppLinks(device: Device) {
        selectedDevice = device
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading

            getAppLinksUseCase(device.serialNumber)
                .onSuccess { appLinks ->
                    _uiState.value = DashboardUiState.Success(appLinks)
                }
                .onFailure { error ->
                    _uiState.value = DashboardUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun fireIntent(config: IntentConfig) {
        val device = selectedDevice ?: return

        viewModelScope.launch {
            fireIntentUseCase(device.serialNumber, config)
                .collect { log ->
                    // Logcat 출력 처리
                    println(log)
                }
        }
    }

    fun forceReverify(packageName: String) {
        val device = selectedDevice ?: return

        viewModelScope.launch {
            forceReverifyUseCase(device.serialNumber, packageName)
                .onSuccess {
                    // 재검증 시작 후 다시 로드
                    loadAppLinks(device)
                }
                .onFailure { error ->
                    _uiState.value = DashboardUiState.Error(error.message ?: "Reverify failed")
                }
        }
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val appLinks: List<AppLink>) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
```

#### 2.4.2 Screen 예시

**/presentation/src/main/kotlin/ui/screen/dashboard/DashboardScreen.kt**
```kotlin
package com.linkops.presentation.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linkops.core.domain.model.AppLink
import com.linkops.core.domain.model.Device
import com.linkops.presentation.ui.component.AppLinkItem
import com.linkops.presentation.ui.component.IntentFireDialog
import com.linkops.presentation.ui.component.LoadingOverlay

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    selectedDevice: Device?
) {
    val uiState by viewModel.uiState.collectAsState()
    var showIntentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDevice) {
        selectedDevice?.let { viewModel.loadAppLinks(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                LoadingOverlay()
            }

            is DashboardUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 헤더
                    TopAppBar(
                        title = { Text("App Link Dashboard") },
                        actions = {
                            Button(onClick = { showIntentDialog = true }) {
                                Text("Fire Intent")
                            }
                        }
                    )

                    // 앱 링크 목록
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.appLinks) { appLink ->
                            AppLinkItem(
                                appLink = appLink,
                                onReverify = { viewModel.forceReverify(appLink.packageName) }
                            )
                        }
                    }
                }
            }

            is DashboardUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.message}", color = MaterialTheme.colors.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { selectedDevice?.let { viewModel.loadAppLinks(it) } }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }

        if (showIntentDialog) {
            IntentFireDialog(
                onDismiss = { showIntentDialog = false },
                onFire = { config ->
                    viewModel.fireIntent(config)
                    showIntentDialog = false
                }
            )
        }
    }
}
```

---

## 3. 데이터 플로우 다이어그램

### 3.1 디바이스 감지 플로우

```
┌─────────────┐
│ UI (Screen) │
└──────┬──────┘
       │ observeDevices()
       ▼
┌──────────────┐
│  ViewModel   │
└──────┬───────┘
       │ invoke()
       ▼
┌─────────────────────┐
│ DetectDevicesUseCase │
└──────┬──────────────┘
       │ observeDevices()
       ▼
┌──────────────────┐
│ DeviceRepository │
└──────┬───────────┘
       │ executeStream("devices -l")
       ▼
┌─────────────────┐
│ AdbShellExecutor │
└──────┬──────────┘
       │ ProcessBuilder
       ▼
┌──────────┐
│ ADB CLI  │
└──────────┘
       │
       ▼
┌──────────┐
│  Device  │ (USB/WiFi)
└──────────┘

반환 Flow:
Device → AdbShellExecutor → DeviceRepository → UseCase → ViewModel → UI
```

### 3.2 앱 링크 조회 플로우

```
┌─────────────┐
│ UI (Screen) │ loadAppLinks(device)
└──────┬──────┘
       │
       ▼
┌──────────────┐
│  ViewModel   │ getAppLinksUseCase(serialNumber)
└──────┬───────┘
       │
       ▼
┌──────────────────┐
│ GetAppLinksUseCase │
└──────┬───────────┘
       │
       ▼
┌─────────────────┐
│ AppLinkRepository │
└──────┬──────────┘
       │ 1. getDevice() → SDK level 확인
       │ 2. strategyFactory.create(sdkLevel)
       │ 3. strategy.getAppLinksCommand()
       │ 4. adbExecutor.executeOnDevice()
       ▼
┌──────────────────┐
│ AdbShellExecutor │
└──────┬───────────┘
       │ adb -s <serial> shell pm get-app-links
       ▼
┌──────────┐
│  Device  │
└──────────┘
       │ stdout
       ▼
┌──────────────────┐
│ GetAppLinksParser │ parse(output)
└──────┬───────────┘
       │
       ▼
┌──────────────┐
│ List<AppLink> │ → ViewModel → UI
└───────────────┘
```

### 3.3 AssetLinks 검증 플로우

```
┌─────────────┐
│ UI (Screen) │ validateDomain(domain)
└──────┬──────┘
       │
       ▼
┌──────────────┐
│  ViewModel   │
└──────┬───────┘
       │
       ▼
┌─────────────────────────┐
│ ValidateAssetLinksUseCase │
└──────┬──────────────────┘
       │
       ▼
┌──────────────────────┐
│ AssetLinksRepository │
└──────┬───────────────┘
       │ assetLinksClient.fetch(domain)
       ▼
┌──────────────────┐
│ AssetLinksClient │
└──────┬───────────┘
       │ Ktor HTTP GET
       ▼
┌────────────────────────────────────┐
│ https://domain/.well-known/        │
│        assetlinks.json              │
└──────┬─────────────────────────────┘
       │ JSON response
       ▼
┌──────────────────────┐
│ AssetLinksRepository │ JSON 파싱 + 검증
└──────┬───────────────┘
       │
       ▼
┌─────────────────────┐
│ AssetLinksValidation │ → ViewModel → UI
└─────────────────────┘
```

---

## 4. 핵심 인터페이스 및 추상화

### 4.1 계층 간 경계 인터페이스

```kotlin
// Domain ← Data 경계
interface DeviceRepository
interface AppLinkRepository
interface AssetLinksRepository
interface ManifestRepository

// Data ← Infrastructure 경계
interface AdbClient
interface NetworkClient
interface FileSystemClient

// Infrastructure 내부 전략 패턴
interface AdbCommandStrategy
interface AdbOutputParser
```

### 4.2 공통 Result 타입

**/core/domain/src/main/kotlin/common/Result.kt**
```kotlin
package com.linkops.core.domain.common

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: DomainError) : Result<Nothing>()

    fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Failure -> this
        }
    }

    fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    fun onFailure(action: (DomainError) -> Unit): Result<T> {
        if (this is Failure) action(error)
        return this
    }
}

sealed class DomainError {
    data class AdbError(val message: String) : DomainError()
    data class NetworkError(val message: String) : DomainError()
    data class ParseError(val message: String) : DomainError()
    data class DeviceNotFound(val serialNumber: String) : DomainError()
    object Unknown : DomainError()
}
```

---

## 5. 의존성 주입 전략

### 5.1 DI 컨테이너 (Simple Factory Pattern)

Kotlin Compose Desktop은 모바일과 달리 Hilt/Koin이 필수가 아니므로, 간단한 Factory 패턴으로 시작합니다.

**/di/src/main/kotlin/AppContainer.kt**
```kotlin
package com.linkops.di

import com.linkops.core.domain.repository.*
import com.linkops.core.domain.usecase.device.*
import com.linkops.core.domain.usecase.applink.*
import com.linkops.core.domain.usecase.diagnostics.*
import com.linkops.core.data.repository.*
import com.linkops.core.infrastructure.adb.*
import com.linkops.core.infrastructure.network.*
import com.linkops.presentation.ui.screen.dashboard.DashboardViewModel
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * 전역 의존성 컨테이너
 */
object AppContainer {
    // Infrastructure
    private val adbBinaryManager by lazy { AdbBinaryManager() }
    private val adbShellExecutor by lazy { AdbShellExecutor(adbBinaryManager) }
    private val strategyFactory by lazy { AdbCommandStrategyFactory() }
    private val getAppLinksParser by lazy { GetAppLinksParser() }
    private val dumpsysParser by lazy { DumpsysParser() }

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    private val httpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    private val assetLinksClient by lazy { AssetLinksClient(httpClient, json) }

    // Data
    private val deviceMapper by lazy { DeviceMapper() }

    // Repositories
    private val deviceRepository: DeviceRepository by lazy {
        DeviceRepositoryImpl(adbShellExecutor, deviceMapper)
    }

    private val appLinkRepository: AppLinkRepository by lazy {
        AppLinkRepositoryImpl(
            adbShellExecutor,
            strategyFactory,
            getAppLinksParser,
            dumpsysParser
        )
    }

    private val assetLinksRepository: AssetLinksRepository by lazy {
        AssetLinksRepositoryImpl(assetLinksClient, json)
    }

    // Use Cases
    val detectDevicesUseCase by lazy { DetectDevicesUseCase(deviceRepository) }
    val getAppLinksUseCase by lazy { GetAppLinksUseCase(appLinkRepository) }
    val fireIntentUseCase by lazy { FireIntentUseCase(appLinkRepository) }
    val forceReverifyUseCase by lazy { ForceReverifyUseCase(appLinkRepository) }
    val validateAssetLinksUseCase by lazy { ValidateAssetLinksUseCase(assetLinksRepository) }

    // ViewModels
    fun provideDashboardViewModel(): DashboardViewModel {
        return DashboardViewModel(
            getAppLinksUseCase,
            fireIntentUseCase,
            forceReverifyUseCase
        )
    }
}
```

### 5.2 향후 확장 (Koin 적용 시)

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.insert-koin:koin-core:3.5.0")
}

// di/AppModule.kt
val appModule = module {
    // Infrastructure
    single { AdbBinaryManager() }
    single { AdbShellExecutor(get()) }
    single { AdbCommandStrategyFactory() }

    // Repositories
    single<DeviceRepository> { DeviceRepositoryImpl(get(), get()) }
    single<AppLinkRepository> { AppLinkRepositoryImpl(get(), get(), get(), get()) }

    // Use Cases
    factory { DetectDevicesUseCase(get()) }
    factory { GetAppLinksUseCase(get()) }

    // ViewModels
    factory { DashboardViewModel(get(), get(), get()) }
}
```

---

## 6. 에러 핸들링 패턴

### 6.1 계층별 에러 처리 전략

| 계층 | 에러 타입 | 처리 방식 |
|------|----------|-----------|
| **Infrastructure** | IOException, ProcessException | → `Result.failure(AdbError(...))` |
| **Data** | ParsingException, NetworkException | → `Result.failure(ParseError(...))` |
| **Domain** | DomainError (sealed class) | UseCase에서 전파 |
| **Presentation** | DomainError | UiState.Error로 변환 |

### 6.2 에러 핸들링 예시

**/core/infrastructure/src/main/kotlin/adb/AdbShellExecutor.kt**
```kotlin
suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        // 실행 로직
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            Result.Failure(DomainError.AdbError(e.message ?: "Unknown ADB error"))
        }
    )
}
```

**/presentation/src/main/kotlin/ui/screen/dashboard/DashboardViewModel.kt**
```kotlin
fun loadAppLinks(device: Device) {
    viewModelScope.launch {
        _uiState.value = DashboardUiState.Loading

        getAppLinksUseCase(device.serialNumber)
            .onSuccess { appLinks ->
                _uiState.value = DashboardUiState.Success(appLinks)
            }
            .onFailure { error ->
                _uiState.value = DashboardUiState.Error(
                    message = when (error) {
                        is DomainError.AdbError -> "ADB 오류: ${error.message}"
                        is DomainError.DeviceNotFound -> "디바이스를 찾을 수 없습니다"
                        else -> "알 수 없는 오류"
                    }
                )
            }
    }
}
```

---

## 7. 테스팅 전략

### 7.1 테스트 피라미드

```
         ┌─────────┐
         │   E2E   │  (5%) - Compose UI Tests
         └─────────┘
       ┌─────────────┐
       │ Integration │  (25%) - Repository + Infrastructure
       └─────────────┘
    ┌──────────────────┐
    │   Unit Tests     │  (70%) - UseCase, ViewModel, Parser
    └──────────────────┘
```

### 7.2 계층별 테스트 전략

#### 7.2.1 Domain Layer (Unit Tests)

**/core/domain/src/test/kotlin/usecase/GetAppLinksUseCaseTest.kt**
```kotlin
class GetAppLinksUseCaseTest {
    private val mockRepository = mockk<AppLinkRepository>()
    private val useCase = GetAppLinksUseCase(mockRepository)

    @Test
    fun `should return app links when repository succeeds`() = runTest {
        // Given
        val expected = listOf(AppLink("com.example", emptyList()))
        coEvery { mockRepository.getAppLinks(any()) } returns Result.Success(expected)

        // When
        val result = useCase("serial123")

        // Then
        assertTrue(result is Result.Success)
        assertEquals(expected, (result as Result.Success).data)
    }
}
```

#### 7.2.2 Infrastructure Layer (Unit Tests)

**/core/infrastructure/src/test/kotlin/adb/parser/GetAppLinksParserTest.kt**
```kotlin
class GetAppLinksParserTest {
    private val parser = GetAppLinksParser()

    @Test
    fun `should parse Android 12 output correctly`() {
        // Given
        val input = """
            com.example.app:
              ID: 12345678-1234-1234-1234-123456789012
              Signatures: [AB:CD:EF]
              Domain verification state:
                example.com: verified
                test.example.com: none
        """.trimIndent()

        // When
        val result = parser.parse(input)

        // Then
        assertEquals(1, result.size)
        assertEquals("com.example.app", result[0].packageName)
        assertEquals(2, result[0].domains.size)
        assertEquals(VerificationState.VERIFIED, result[0].domains[0].verificationState)
    }
}
```

#### 7.2.3 Data Layer (Integration Tests)

**/core/data/src/test/kotlin/repository/AppLinkRepositoryImplTest.kt**
```kotlin
class AppLinkRepositoryImplTest {
    private val fakeAdbExecutor = FakeAdbShellExecutor()
    private val repository = AppLinkRepositoryImpl(
        fakeAdbExecutor,
        AdbCommandStrategyFactory(),
        GetAppLinksParser(),
        DumpsysParser()
    )

    @Test
    fun `should use Android 12 strategy when SDK is 31`() = runTest {
        // Given
        fakeAdbExecutor.mockOutput("getprop ro.build.version.sdk", "31")
        fakeAdbExecutor.mockOutput("pm get-app-links", ANDROID_12_OUTPUT)

        // When
        val result = repository.getAppLinks("serial123")

        // Then
        assertTrue(result is Result.Success)
        assertTrue(fakeAdbExecutor.executedCommands.contains("pm get-app-links"))
    }
}

class FakeAdbShellExecutor : AdbShellExecutor {
    val executedCommands = mutableListOf<String>()
    private val mockOutputs = mutableMapOf<String, String>()

    fun mockOutput(command: String, output: String) {
        mockOutputs[command] = output
    }

    override suspend fun executeOnDevice(serial: String, command: String): Result<String> {
        executedCommands.add(command)
        return Result.Success(mockOutputs[command] ?: "")
    }
}
```

#### 7.2.4 Presentation Layer (ViewModel Tests)

**/presentation/src/test/kotlin/ui/screen/dashboard/DashboardViewModelTest.kt**
```kotlin
class DashboardViewModelTest {
    private val mockGetAppLinksUseCase = mockk<GetAppLinksUseCase>()
    private val viewModel = DashboardViewModel(
        mockGetAppLinksUseCase,
        mockk(),
        mockk()
    )

    @Test
    fun `should update UI state to Success when use case succeeds`() = runTest {
        // Given
        val appLinks = listOf(AppLink("com.example", emptyList()))
        coEvery { mockGetAppLinksUseCase(any()) } returns Result.Success(appLinks)

        // When
        viewModel.loadAppLinks(Device("serial123", ...))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        assertEquals(appLinks, (state as DashboardUiState.Success).appLinks)
    }
}
```

### 7.3 테스트 의존성 (build.gradle.kts)

```kotlin
dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0") // Flow 테스트
}
```

---

## 8. 빌드 시퀀스 및 구현 순서

### Phase 1: 프로젝트 초기 설정 ✅ COMPLETED

#### 체크리스트
- [x] 1.1 Gradle 프로젝트 생성 및 모듈 구조 설정
  - ~~buildSrc 설정 (Dependencies.kt, BuildConfig.kt)~~ → libs.versions.toml 사용
  - settings.gradle.kts 작성 ✅
  - composeApp 모듈 build.gradle.kts 작성 ✅
- [x] 1.2 Compose Desktop 기본 설정
  - /composeApp/src/jvmMain/kotlin/.../main.kt 생성 ✅
  - 기본 Window 및 MaterialTheme 설정 ✅
- [x] 1.3 Git 저장소 초기화
  - .gitignore 설정 ✅ (Claude 설정 + doc 폴더 제외)
  - README.md 작성 ✅

**검증 기준**: `./gradlew :composeApp:run` 실행 시 창이 열림 ✅

---

### Phase 2: Infrastructure Layer - ADB 통신

#### 체크리스트
- [x] 2.1 ADB 바이너리 관리 ✅
  - /composeApp/.../infrastructure/adb/AdbBinaryManager.kt 구현 ✅
  - 시스템 ADB 감지 로직 (which/where 명령어) ✅
  - ADB 다운로드 로직 (ZIP 압축 해제 포함) ✅
- [ ] 2.2 ADB Shell Executor
  - /core/infrastructure/.../AdbShellExecutor.kt 구현
  - ProcessBuilder 기반 명령어 실행
  - Flow 기반 스트리밍 실행
- [ ] 2.3 단위 테스트 작성
  - AdbBinaryManagerTest
  - AdbShellExecutorTest (Fake ProcessBuilder 사용)

**검증 기준**: `adb devices` 명령어 실행 성공

---

### Phase 3: Domain Layer - 기본 모델 및 UseCase

#### 체크리스트
- [ ] 3.1 Domain Models 정의
  - Device.kt
  - AppLink.kt
  - IntentConfig.kt
- [ ] 3.2 Repository Interfaces 정의
  - DeviceRepository.kt
  - AppLinkRepository.kt
- [ ] 3.3 UseCase 구현
  - DetectDevicesUseCase.kt
  - GetAppLinksUseCase.kt
  - FireIntentUseCase.kt
- [ ] 3.4 단위 테스트 작성
  - 각 UseCase별 성공/실패 케이스 테스트

**검증 기준**: UseCase 테스트 100% 통과

---

### Phase 4: Data Layer - Repository 구현

#### 체크리스트
- [ ] 4.1 Parser 구현
  - GetAppLinksParser.kt (Android 12+)
  - DumpsysParser.kt (Android 11 이하)
  - DeviceMapper.kt
- [ ] 4.2 Strategy Pattern 구현
  - AdbCommandStrategy.kt
  - Android11Strategy.kt
  - Android12PlusStrategy.kt
  - AdbCommandStrategyFactory.kt
- [ ] 4.3 Repository 구현
  - DeviceRepositoryImpl.kt
  - AppLinkRepositoryImpl.kt
- [ ] 4.4 통합 테스트 작성
  - FakeAdbShellExecutor 생성
  - Repository 통합 테스트

**검증 기준**: 실제 Android 디바이스에서 `pm get-app-links` 파싱 성공

---

### Phase 5: Presentation Layer - 기본 UI

#### 체크리스트
- [ ] 5.1 테마 및 디자인 시스템
  - Color.kt
  - Typography.kt
  - Theme.kt
- [ ] 5.2 재사용 컴포넌트
  - DeviceCard.kt
  - AppLinkItem.kt
  - StatusBadge.kt
  - LoadingOverlay.kt
- [ ] 5.3 화면 구현 (MVP)
  - DeviceSelectionScreen.kt + ViewModel
  - DashboardScreen.kt + ViewModel
- [ ] 5.4 Navigation 설정
  - NavGraph.kt
  - Screen.kt

**검증 기준**: 디바이스 선택 → 앱 링크 목록 조회 플로우 동작

---

### Phase 6: Feature - Intent Fire

#### 체크리스트
- [ ] 6.1 IntentFireDialog 컴포넌트
  - URI 입력 필드
  - Action 선택 (VIEW, SEND 등)
  - Flag 체크박스 (NEW_TASK, CLEAR_TOP 등)
- [ ] 6.2 FireIntentUseCase 통합
  - DashboardViewModel에 fireIntent() 추가
  - Logcat 스트리밍 UI 표시
- [ ] 6.3 테스트
  - Intent 실행 후 logcat 출력 확인

**검증 기준**: 커스텀 URI로 Intent 발사 성공

---

### Phase 7: Feature - Force Re-verification

#### 체크리스트
- [ ] 7.1 ForceReverifyUseCase 구현
- [ ] 7.2 UI 통합
  - AppLinkItem에 "Re-verify" 버튼 추가
  - 재검증 후 자동 새로고침
- [ ] 7.3 Logcat 모니터링
  - observeVerificationLogs() 구현
  - 검증 로그 필터링 (ActivityTaskManager, IntentFilter)

**검증 기준**: 재검증 후 도메인 상태 변경 확인

---

### Phase 8: Feature - AssetLinks Diagnostics

#### 체크리스트
- [ ] 8.1 Network Layer
  - KtorClientFactory.kt
  - AssetLinksClient.kt
- [ ] 8.2 AssetLinks 모델
  - AssetLinks.kt
  - ValidationStatus.kt
  - ValidationIssue.kt
- [ ] 8.3 Repository 구현
  - AssetLinksRepositoryImpl.kt
- [ ] 8.4 UseCase 및 ViewModel
  - ValidateAssetLinksUseCase.kt
  - DiagnosticsViewModel.kt
- [ ] 8.5 DiagnosticsScreen
  - 도메인 입력 필드
  - JSON 뷰어
  - 이슈 목록 표시

**검증 기준**: assetlinks.json 가져오기 및 SHA-256 매칭 검증

---

### Phase 9: Feature - Manifest Static Analysis

#### 체크리스트
- [ ] 9.1 파일 시스템 레이어
  - ApkParser.kt (aapt2 또는 ZipFile 사용)
  - ManifestParser.kt (XML 파싱)
- [ ] 9.2 ManifestDeepLink 모델
- [ ] 9.3 ManifestRepository 구현
- [ ] 9.4 UI
  - ManifestAnalyzerScreen.kt
  - 드래그 앤 드롭 지원 (Compose Desktop FileDrop API)
  - Deep Link 목록 표시

**검증 기준**: APK 또는 프로젝트 폴더에서 AndroidManifest.xml 파싱 성공

---

### Phase 10: Feature - Team Sync (.linkops 파일)

#### 체크리스트
- [ ] 10.1 .linkops 파일 포맷 정의 (JSON)
  ```json
  {
    "version": "1.0",
    "links": [
      {
        "name": "Open Product Page",
        "uri": "myapp://product/123",
        "action": "VIEW",
        "flags": ["ACTIVITY_NEW_TASK"]
      }
    ]
  }
  ```
- [ ] 10.2 파일 저장/불러오기 UseCase
- [ ] 10.3 UI 통합
  - "Save As" 버튼
  - "Open" 버튼

**검증 기준**: .linkops 파일 저장 및 불러오기 성공

---

### Phase 11: Packaging & Distribution

#### 체크리스트
- [ ] 11.1 Compose Desktop Packaging 설정
  ```kotlin
  compose.desktop {
      application {
          mainClass = "MainKt"
          nativeDistributions {
              targetFormats(Dmg, Msi, Deb)
              packageName = "LinkOps"
              packageVersion = "1.0.0"
          }
      }
  }
  ```
- [ ] 11.2 macOS .dmg 빌드
- [ ] 11.3 Windows .msi 빌드
- [ ] 11.4 Linux .deb 빌드
- [ ] 11.5 CI/CD 설정 (GitHub Actions)

**검증 기준**: 각 플랫폼에서 설치 파일 실행 성공

---

### Phase 12: 최적화 및 안정화

#### 체크리스트
- [ ] 12.1 성능 최적화
  - ADB 명령어 캐싱
  - Flow debounce 적용
- [ ] 12.2 에러 핸들링 개선
  - 사용자 친화적 에러 메시지
  - Retry 메커니즘
- [ ] 12.3 접근성 개선
  - 키보드 네비게이션
  - 다크 모드 지원
- [ ] 12.4 E2E 테스트 작성
  - Compose UI Test로 주요 플로우 검증

**검증 기준**: 모든 기능 통합 테스트 통과

---

## 9. 기술적 고려사항

### 9.1 ADB 바이너리 번들링 전략

**옵션 A: 런타임 다운로드 (권장)**
- 장점: 설치 파일 크기 감소 (5MB → 50MB 차이)
- 단점: 초기 실행 시 네트워크 필요
- 구현: 첫 실행 시 Google SDK 저장소에서 다운로드

**옵션 B: 바이너리 임베딩**
- 장점: 오프라인 환경에서 즉시 사용 가능
- 단점: 설치 파일 크기 증가
- 구현: `resources/adb/` 폴더에 플랫폼별 바이너리 포함

### 9.2 Coroutine Context 전략

```kotlin
// ViewModel
private val viewModelScope = CoroutineScope(
    SupervisorJob() + Dispatchers.Main
)

// Repository (IO 작업)
suspend fun execute(...): Result<T> = withContext(Dispatchers.IO) {
    // ADB 명령어 실행
}

// UI (Main 스레드)
LaunchedEffect(key) {
    // collectAsState로 자동 구독
}
```

### 9.3 메모리 관리

- `Flow`는 `stateIn()` 대신 `collectAsState()` 사용 (메모리 누수 방지)
- ViewModel은 `viewModelScope.cancel()` 호출 (Window 닫힐 때)
- ADB Process는 `destroy()` 명시적 호출

---

## 10. 파일 경로 요약

| 컴포넌트 | 파일 경로 | 책임 |
|---------|----------|------|
| **Domain Models** | `/core/domain/src/main/kotlin/model/Device.kt` | 디바이스 도메인 모델 |
| **Use Cases** | `/core/domain/src/main/kotlin/usecase/applink/GetAppLinksUseCase.kt` | 앱 링크 조회 비즈니스 로직 |
| **Repository Interface** | `/core/domain/src/main/kotlin/repository/AppLinkRepository.kt` | 앱 링크 데이터 소스 추상화 |
| **Repository Impl** | `/core/data/src/main/kotlin/repository/AppLinkRepositoryImpl.kt` | Repository 구현 |
| **ADB Executor** | `/core/infrastructure/src/main/kotlin/adb/AdbShellExecutor.kt` | ADB 명령어 실행 |
| **Parser** | `/core/infrastructure/src/main/kotlin/adb/parser/GetAppLinksParser.kt` | ADB 출력 파싱 |
| **Strategy** | `/core/infrastructure/src/main/kotlin/adb/strategy/Android12PlusStrategy.kt` | Android 버전별 명령어 전략 |
| **ViewModel** | `/presentation/src/main/kotlin/ui/screen/dashboard/DashboardViewModel.kt` | UI 상태 관리 |
| **Screen** | `/presentation/src/main/kotlin/ui/screen/dashboard/DashboardScreen.kt` | Compose UI |
| **DI Container** | `/di/src/main/kotlin/AppContainer.kt` | 의존성 주입 |
| **Main** | `/desktop/src/main/kotlin/Main.kt` | 진입점 |

---

## 11. 다음 단계

1. **즉시 시작 가능한 작업**: Phase 1 (프로젝트 초기 설정)
2. **우선순위 높은 기술적 검증**: ADB 바이너리 다운로드 및 실행 (Phase 2)
3. **MVP 목표**: Phase 5 완료 시 기본 앱 링크 조회 기능 동작

---

이 문서는 LinkOps 프로젝트의 **완전한 구현 로드맵**입니다. 각 Phase는 독립적으로 구현 가능하며, 테스트 가능한 단위로 설계되었습니다. 추가 질문이나 특정 컴포넌트의 상세 설계가 필요하면 요청해 주세요.

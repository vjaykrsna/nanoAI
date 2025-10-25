@file:Suppress("TooManyFunctions", "LongMethod", "CyclomaticComplexMethod", "LongParameterList")

package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainTestHelper.loadClass
import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainTestHelper.loadEnumConstant
import com.vjaykrsna.nanoai.feature.uiux.domain.UiUxDomainTestHelper.primaryConstructor
import java.lang.reflect.Constructor
import java.lang.reflect.Proxy
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.runCatching
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.Instant

internal object UiUxDomainReflection {
  private const val USER_PROFILE = "com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile"
  private const val LAYOUT_SNAPSHOT = "com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot"
  private const val UI_STATE_SNAPSHOT =
    "com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot"
  private const val UI_PREFERENCES =
    "com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot"
  private const val THEME_ENUM = "com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference"
  private const val VISUAL_DENSITY_ENUM =
    "com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity"
  private const val SCREEN_TYPE_ENUM = "com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType"
  private const val DEFAULT_CONSTRUCTOR_MARKER = "kotlin.jvm.internal.DefaultConstructorMarker"

  fun themePreference(name: String): Any = loadEnumConstant(THEME_ENUM, name)

  fun visualDensity(name: String): Any = loadEnumConstant(VISUAL_DENSITY_ENUM, name)

  fun screenType(name: String): Any = loadEnumConstant(SCREEN_TYPE_ENUM, name)

  fun newLayoutSnapshot(
    id: String = "layout-1",
    name: String = "Daily Focus",
    lastOpenedScreen: String = "home",
    pinnedTools: List<String> = listOf("tool-1"),
    isCompact: Boolean = false,
  ): Any {
    val ctor = primaryConstructor(loadClass(LAYOUT_SNAPSHOT))
    return ctor.newInstance(id, name, lastOpenedScreen, pinnedTools, isCompact)
  }

  fun newUiStateSnapshot(
    userId: String = "user-123",
    expandedPanels: List<String> = emptyList(),
    recentActions: List<String> = emptyList(),
    sidebarCollapsed: Boolean = false,
    leftDrawerOpen: Boolean = false,
    rightDrawerOpen: Boolean = false,
    activeModeRoute: String = "home",
    activeRightPanel: String? = null,
    paletteVisible: Boolean = false,
  ): Any {
    val ctor = primaryConstructor(loadClass(UI_STATE_SNAPSHOT))
    return ctor.newInstance(
      userId,
      expandedPanels,
      recentActions,
      sidebarCollapsed,
      leftDrawerOpen,
      rightDrawerOpen,
      activeModeRoute,
      activeRightPanel,
      paletteVisible,
    )
  }

  fun newUiPreferences(
    themePreference: Any = themePreference("LIGHT"),
    visualDensity: Any = visualDensity("DEFAULT"),
    pinnedTools: List<String> = emptyList(),
    commandPaletteRecents: List<String> = emptyList(),
    connectivityBannerLastDismissed: Instant? = null,
    voiceInputEnabled: Boolean = false,
  ): Any {
    val clazz = loadClass(UI_PREFERENCES)
    val ctor = preferredConstructor(clazz)
    return when (ctor.parameterCount) {
      0 -> {
        val instance = ctor.newInstance()
        invokeCopy(
          instance,
          themePreference,
          visualDensity,
          pinnedTools,
          commandPaletteRecents,
          connectivityBannerLastDismissed,
          voiceInputEnabled,
        )
      }
      5 ->
        ctor.newInstance(
          themePreference,
          visualDensity,
          pinnedTools,
          commandPaletteRecents,
          connectivityBannerLastDismissed,
        )
      6 ->
        ctor.newInstance(
          themePreference,
          visualDensity,
          pinnedTools,
          commandPaletteRecents,
          connectivityBannerLastDismissed,
          voiceInputEnabled,
        )
      else -> error("Unsupported UiPreferencesSnapshot constructor arity ${ctor.parameterCount}")
    }
  }

  fun newUserProfile(
    id: String = "user-123",
    displayName: String? = "Taylor",
    themePreference: Any = themePreference("LIGHT"),
    visualDensity: Any = visualDensity("DEFAULT"),
    lastOpenedScreen: Any = screenType("HOME"),
    compactMode: Boolean = false,
    pinnedTools: List<String> = emptyList(),
    savedLayouts: List<Any> = emptyList(),
  ): Any {
    val ctor = primaryConstructor(loadClass(USER_PROFILE))
    return ctor.newInstance(
      id,
      displayName,
      themePreference,
      visualDensity,
      lastOpenedScreen,
      compactMode,
      pinnedTools,
      savedLayouts,
    )
  }

  fun copyUiPreferences(
    original: Any?,
    themePreference: Any? = null,
    visualDensity: Any? = null,
    pinnedTools: List<String>? = null,
    commandPaletteRecents: List<String>? = null,
    connectivityBannerLastDismissed: Instant? = null,
  ): Any {
    val baseline = original ?: newUiPreferences()
    val clazz = baseline.javaClass
    val ctor = preferredConstructor(clazz)

    val resolvedTheme =
      coerceThemePreference(themePreference)
        ?: coerceThemePreference(getProperty(baseline, "themePreference"))
        ?: error("themePreference must not be null")
    val resolvedDensity =
      (visualDensity ?: getProperty(baseline, "visualDensity"))
        ?: error("visualDensity must not be null")

    val resolvedPinned: List<String> =
      pinnedTools
        ?: run {
          val raw = getProperty(baseline, "pinnedTools") as? List<*>
          raw?.mapNotNull { it?.toString() } ?: emptyList()
        }

    val resolvedCommandRecents: List<String> =
      commandPaletteRecents
        ?: run {
          val raw =
            runCatching { getProperty(baseline, "commandPaletteRecents") as? List<*> }.getOrNull()
          raw?.mapNotNull { it?.toString() } ?: emptyList()
        }
    val resolvedConnectivityDismissed =
      connectivityBannerLastDismissed
        ?: runCatching { getProperty(baseline, "connectivityBannerLastDismissed") as Instant? }
          .getOrNull()

    return when (ctor.parameterCount) {
      0 ->
        invokeCopy(
          baseline,
          resolvedTheme,
          resolvedDensity,
          resolvedPinned,
          resolvedCommandRecents,
          resolvedConnectivityDismissed,
        )
      5 ->
        ctor.newInstance(
          resolvedTheme,
          resolvedDensity,
          resolvedPinned,
          resolvedCommandRecents,
          resolvedConnectivityDismissed,
        )
      6 ->
        ctor.newInstance(
          resolvedTheme,
          resolvedDensity,
          resolvedPinned,
          resolvedCommandRecents,
          resolvedConnectivityDismissed,
          false, // voiceInputEnabled defaults to false
        )
      else -> error("Unsupported UiPreferencesSnapshot constructor arity ${ctor.parameterCount}")
    }
  }

  fun updateLayoutCompact(layout: Any, isCompact: Boolean): Any {
    val clazz = layout.javaClass
    val ctor = primaryConstructor(clazz)
    val id = getProperty(layout, "id") as? String ?: ""
    val name = getProperty(layout, "name") as? String ?: ""
    val lastOpened = getProperty(layout, "lastOpenedScreen") as? String ?: ""

    val pinned =
      (getProperty(layout, "pinnedTools") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    return ctor.newInstance(id, name, lastOpened, pinned, isCompact)
  }

  fun getProperty(instance: Any, property: String): Any? {
    val capitalized =
      property.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
      }
    val candidates = listOf("get$capitalized", "is$capitalized")
    val method =
      candidates
        .asSequence()
        .mapNotNull { name -> instance.javaClass.methods.firstOrNull { it.name == name } }
        .firstOrNull() ?: error("Property $property not found on ${instance.javaClass.name}")
    return method.invoke(instance)
  }

  private fun preferredConstructor(clazz: Class<*>): Constructor<*> {
    val constructors =
      clazz.declaredConstructors
        .filterNot { constructor ->
          constructor.parameterTypes.any { type -> type.name == DEFAULT_CONSTRUCTOR_MARKER }
        }
        .sortedByDescending { it.parameterCount }
    val resolved = constructors.firstOrNull()?.apply { isAccessible = true }
    return resolved
      ?: clazz.declaredConstructors.firstOrNull()?.apply { isAccessible = true }
      ?: error("No accessible constructor found for ${clazz.name}")
  }

  private fun coerceThemePreference(candidate: Any?): Any? =
    when (candidate) {
      null -> null
      is String -> themePreference(candidate)
      else -> candidate
    }

  private fun invokeCopy(
    baseline: Any,
    themePreference: Any,
    visualDensity: Any,
    pinnedTools: List<String>,
    commandPaletteRecents: List<String>,
    connectivityBannerLastDismissed: Instant?,
    voiceInputEnabled: Boolean = false,
  ): Any {
    val method =
      baseline.javaClass.methods.firstOrNull { method ->
        method.name == "copy" && method.parameterCount >= 5
      } ?: error("UiPreferencesSnapshot copy method not found")
    return when (method.parameterCount) {
      5 ->
        method.invoke(
          baseline,
          themePreference,
          visualDensity,
          pinnedTools,
          commandPaletteRecents,
          connectivityBannerLastDismissed,
        )
      6 ->
        method.invoke(
          baseline,
          themePreference,
          visualDensity,
          pinnedTools,
          commandPaletteRecents,
          connectivityBannerLastDismissed,
          voiceInputEnabled,
        )
      else -> error("Unsupported UiPreferencesSnapshot#copy arity ${method.parameterCount}")
    }
  }
}

internal class UserProfileRepositorySpy {
  val profileFlow = MutableStateFlow<Any?>(null)
  val preferencesFlow = MutableStateFlow<Any?>(null)
  val layoutSnapshotsFlow = MutableStateFlow<List<Any>>(emptyList())
  val uiStateFlow = MutableStateFlow<Any?>(null)
  val offlineStatusFlow = MutableStateFlow(false)
  val themeEvents = MutableSharedFlow<Any>(replay = 1)
  val invocations = CopyOnWriteArrayList<String>()
  var lastCompactToggle: Boolean? = null

  fun asProxy(): Any {
    val repositoryClass =
      loadClass("com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository")
    return Proxy.newProxyInstance(repositoryClass.classLoader, arrayOf(repositoryClass)) {
      _,
      method,
      args ->
      val name = method.name
      when {
        name.startsWith("observe", ignoreCase = true) -> handleObserve(name)
        name.contains("refresh", ignoreCase = true) -> {
          invocations += name
          Unit
        }
        name.contains("theme", ignoreCase = true) -> {
          invocations += name
          val themeArg =
            when {
              args == null || args.isEmpty() -> null
              args.size >= 2 -> args[1]
              else -> args.firstOrNull()
            }
          val resolvedTheme =
            when (themeArg) {
              is String ->
                runCatching { UiUxDomainReflection.themePreference(themeArg) }
                  .getOrDefault(themeArg)
              else -> themeArg
            }
          preferencesFlow.value =
            UiUxDomainReflection.copyUiPreferences(
              preferencesFlow.value,
              themePreference = resolvedTheme,
            )
          resolvedTheme?.let { themeEvents.tryEmit(it) }
          Unit
        }
        name.contains("notify", ignoreCase = true) -> {
          invocations += name
          args?.firstOrNull()?.let { themeEvents.tryEmit(it) }
          Unit
        }
        name.contains("compact", ignoreCase = true) -> {
          invocations += name
          val enabled = args?.firstOrNull { it is Boolean } as? Boolean
          if (enabled == null) {
            Unit
          } else {
            lastCompactToggle = enabled
            preferencesFlow.value =
              UiUxDomainReflection.copyUiPreferences(
                preferencesFlow.value,
                visualDensity =
                  UiUxDomainReflection.visualDensity(if (enabled) "COMPACT" else "DEFAULT"),
              )
            layoutSnapshotsFlow.value =
              layoutSnapshotsFlow.value.map {
                UiUxDomainReflection.updateLayoutCompact(it, enabled)
              }
            Unit
          }
        }
        else -> {
          invocations += name
          Unit
        }
      }
    }
  }

  private fun handleObserve(methodName: String): Any =
    when {
      methodName.contains("Profile", ignoreCase = true) -> profileFlow
      methodName.contains("Layout", ignoreCase = true) -> layoutSnapshotsFlow
      methodName.contains("State", ignoreCase = true) ||
        methodName.contains("UiState", ignoreCase = true) -> uiStateFlow
      methodName.contains("Preference", ignoreCase = true) ||
        methodName.contains("Ui", ignoreCase = true) -> preferencesFlow
      methodName.contains("Offline", ignoreCase = true) ||
        methodName.contains("Network", ignoreCase = true) -> offlineStatusFlow
      methodName.contains("Theme", ignoreCase = true) -> themeEvents
      else -> error("Unknown observe method: $methodName")
    }
}

internal fun instantiateUiUxUseCase(
  className: String,
  repository: Any,
  dispatcher: CoroutineDispatcher,
): Any {
  val clazz = UiUxDomainTestHelper.loadClass(className)
  val constructors = clazz.constructors.sortedBy { it.parameterCount }
  constructors.forEach { constructor ->
    val args = mutableListOf<Any?>()
    var supported = true
    constructor.parameterTypes.forEach { parameter ->
      when {
        parameter.isAssignableFrom(repository.javaClass) -> args += repository
        parameter.name.contains("UserProfileRepository") -> args += repository
        parameter.name == CoroutineDispatcher::class.java.name -> args += dispatcher
        parameter.name == "kotlinx.coroutines.CoroutineDispatcher" -> args += dispatcher
        parameter.name == "kotlinx.coroutines.CoroutineScope" -> args += TestScope(dispatcher)
        parameter.name == "kotlin.coroutines.CoroutineContext" -> args += dispatcher
        else -> supported = false
      }
    }
    if (supported && args.size == constructor.parameterCount) {
      return constructor.newInstance(*args.toTypedArray())
    }
  }
  error("Unable to instantiate $className with repository/dispatcher test doubles")
}

internal class AnalyticsRecorder {
  val events = mutableListOf<String>()

  fun asProxy(parameterType: Class<*>): Any {
    if (!parameterType.isInterface) {
      error("Analytics dependency ${parameterType.name} must be an interface for testing")
    }

    return Proxy.newProxyInstance(parameterType.classLoader, arrayOf(parameterType)) {
      _,
      method,
      args ->
      val payload = args?.firstOrNull { it is String } as? String
      payload?.let { events += it }
      when (method.returnType) {
        Boolean::class.javaPrimitiveType,
        Boolean::class.java -> false
        Int::class.javaPrimitiveType,
        Int::class.java -> 0
        Long::class.javaPrimitiveType,
        Long::class.java -> 0L
        else -> null
      }
    }
  }
}

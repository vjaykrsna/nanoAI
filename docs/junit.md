# JUnit Evolution: Improvements from JUnit 4 to JUnit 5, and JUnit 5 to JUnit 6

This document outlines the key improvements and new features introduced in JUnit 5 over JUnit 4, and subsequently in JUnit 6 over JUnit 5. Understanding these advancements helps in migrating existing tests and leveraging new capabilities for better test quality, maintainability, and performance.

## JUnit 5 Improvements Over JUnit 4

JUnit 5 represents a complete rewrite of the testing framework, introducing a modular architecture and modern Java features. Here are the major improvements:

### 1. Modular Architecture
- **JUnit Platform**: Foundation for launching testing frameworks on the JVM. Enables IDEs and build tools to discover and execute tests.
- **JUnit Jupiter**: Programming model for writing tests and extensions.
- **JUnit Vintage**: Test engine for running JUnit 3 and 4 tests on the JUnit Platform.

### 2. Enhanced Annotations
- `@Test`, `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll` (replacing `@Before`, `@After`, etc.)
- `@DisplayName`: Human-readable test names
- `@Nested`: Organize test classes hierarchically
- `@Tag`: Categorize tests for selective execution
- `@Disabled`: Replace `@Ignore`

### 3. Improved Assertions
- `assertAll()`: Group multiple assertions that all need to pass
- `assertThrows()`: Verify exceptions are thrown
- `assertTimeout()` and `assertTimeoutPreemptively()`: Test execution time limits
- Support for custom assertions via `Assertions.assertThat()`

### 4. Parameterized Tests
- `@ParameterizedTest` with various sources (`@ValueSource`, `@EnumSource`, `@MethodSource`, etc.)
- More flexible than JUnit 4's `@RunWith(Parameterized.class)`

### 5. Extensions Model
- Replaced JUnit 4's `@RunWith` and `@Rule` with a more powerful extension mechanism
- `@ExtendWith`: Register extensions for custom behavior
- Built-in extensions for Spring, Mockito, etc.

### 6. Dynamic Tests
- `@TestFactory`: Generate tests dynamically at runtime
- Useful for data-driven tests with varying numbers of test cases

### 7. Test Discovery and Execution
- Better support for parallel test execution
- Improved test discovery mechanisms
- Integration with modern build tools and IDEs

### 8. Java 8+ Features
- Lambda expressions in assertions and test methods
- Stream API support
- Better integration with modern Java constructs

### 9. Dependency Injection
- `@TestInfo` and `@TestReporter` for accessing test metadata
- Constructor and method parameter injection

### 10. Better Exception Handling
- More descriptive error messages
- Stack trace filtering for cleaner output

## Migration from JUnit 4 to JUnit 5

### Key Changes:
1. Update dependencies: Replace `junit:junit` with `org.junit.jupiter:junit-jupiter`
2. Change annotations: `@Before` → `@BeforeEach`, `@After` → `@AfterEach`, etc.
3. Update assertions: Use new assertion methods
4. Replace runners: Use `@ExtendWith` instead of `@RunWith`
5. Update parameterized tests: Use `@ParameterizedTest` with sources
6. Handle test suites: Use `@Suite` and `@SelectPackages`

### Example Migration:

**JUnit 4:**
```java
@RunWith(MockitoJUnitRunner.class)
public class ExampleTest {
    @Before
    public void setUp() {
        // setup
    }

    @Test
    public void testExample() {
        assertEquals(2, 1 + 1);
    }
}
```

**JUnit 5:**
```java
@ExtendWith(MockitoExtension.class)
class ExampleTest {
    @BeforeEach
    void setUp() {
        // setup
    }

    @Test
    void testExample() {
        assertEquals(2, 1 + 1);
    }
}
```

## JUnit 6 Improvements Over JUnit 5

JUnit 6, released in September 2025, introduces several enhancements while maintaining backward compatibility with JUnit 5. Key improvements include:

### 1. Updated Baseline Requirements
- **Minimum Java version**: Now requires Java 17 (upgraded from Java 8)
- **Minimum Kotlin version**: Now requires Kotlin 2.2
- **Single version numbering**: Platform, Jupiter, and Vintage now share the same version number

### 2. Nullability Annotations
- All JUnit modules now use [JSpecify](https://jspecify.dev/) annotations to clearly indicate nullable and non-nullable types
- Improves type safety and IDE support for nullability analysis

### 3. Enhanced Test Execution Control
- **Cancellation support**: Tests can now be cancelled via `CancellationToken`
- **Fail-fast mode**: New `--fail-fast` option for ConsoleLauncher stops execution after first failure
- **Better parallel execution**: Improved performance for large test suites

### 4. Improved CSV Parameterized Tests
- Migration from univocity-parsers to [FastCSV](https://fastcsv.org/) library
- Better error reporting for malformed CSV input
- Automatic line separator detection
- Consistent handling of headers and whitespace

### 5. Kotlin Enhancements
- Support for Kotlin `suspend` modifier on test and lifecycle methods
- Better integration with Kotlin coroutines
- Updated contracts for `assertTimeout` functions

### 6. Extension and API Improvements
- New `computeIfAbsent()` methods in stores for better nullability handling
- Enhanced parameterized test display names with quoted arguments
- Better inheritance of `@TestMethodOrder` and `@TestClassOrder` in nested classes
- New `MethodOrderer.Default` and `ClassOrderer.Default` for nested classes

### 7. Platform Enhancements
- Integration of JFR functionality directly in `junit-platform-launcher`
- Removal of deprecated modules (`junit-platform-runner`, `junit-platform-jfr`)
- New Launcher API with `LauncherExecutionRequest` for future extensibility
- Deterministic ordering of nested classes

### 8. Better Error Reporting
- Non-printable characters in display names are replaced with readable alternatives (e.g., `\n` → `<LF>`)
- Pruned stack traces up to test methods
- More descriptive error messages

### 9. Ecosystem Updates
- Updated support for modern build tools and IDEs
- Better integration with CI/CD pipelines
- Enhanced XML and JSON reporting formats

### 10. Deprecations and Removals
- Removed various deprecated APIs and behaviors
- JUnit Vintage is now deprecated (intended for migration only)
- Cleaner codebase with removed legacy support

## Migration from JUnit 5 to JUnit 6

### Key Changes:
1. **Update Java/Kotlin versions**: Ensure project uses Java 17+ and Kotlin 2.2+
2. **Update dependencies**: Change JUnit version to 6.0.0
3. **Review deprecated APIs**: Update any usage of removed deprecated methods
4. **Update parameterized tests**: Benefit from improved CSV handling (no code changes needed)
5. **Leverage new features**: Consider using cancellation tokens or suspend functions where applicable

### Example Migration:

**JUnit 5:**
```java
@Test
void testWithTimeout() {
    assertTimeout(Duration.ofSeconds(1), () -> {
        // test code
    });
}
```

**JUnit 6:**
```java
@Test
void testWithTimeout() {
    assertTimeout(Duration.ofSeconds(1), () -> {
        // test code with improved error reporting
    });
}

// New: Kotlin suspend support
@Test
suspend fun testSuspendFunction() {
    // Coroutine-based test
}
```

## Benefits of Upgrading to JUnit 6

- **Performance**: Faster test discovery and execution
- **Modern Java Support**: Full compatibility with Java 21+ features
- **Better Type Safety**: JSpecify annotations improve nullability handling
- **Enhanced Developer Experience**: Better error messages and IDE integration
- **Future-Proof**: Removes deprecated APIs and aligns with modern JVM ecosystem
- **Kotlin Integration**: Native support for suspend functions and coroutines

## Benefits of Upgrading

### From JUnit 4 to 5:
- Modern Java support
- Better test organization
- Improved assertions and error reporting
- Enhanced IDE integration
- Future-proof architecture

### From JUnit 5 to 6:
- Performance improvements
- Better Java 21+ support
- Enhanced testing capabilities
- Improved developer experience
- Access to latest features and bug fixes

## Best Practices

1. **Gradual Migration**: Migrate tests incrementally rather than all at once
2. **Leverage New Features**: Take advantage of parameterized tests, nested tests, and dynamic tests
3. **Update Build Scripts**: Ensure proper configuration for parallel execution and reporting
4. **Training**: Educate team members on new features and best practices
5. **Continuous Integration**: Update CI pipelines to take advantage of new reporting features

## Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [JUnit 5 Migration Guide](https://junit.org/junit5/docs/current/user-guide/#migrating-from-junit4)
- [JUnit 6 Release Notes](https://docs.junit.org/6.0.0/release-notes/)
- [JUnit 6 User Guide](https://docs.junit.org/6.0.0/user-guide/)
- [JUnit GitHub Repository](https://github.com/junit-team/junit5)
- [JSpecify Nullability Annotations](https://jspecify.dev/)
- [FastCSV Library](https://fastcsv.org/)

---

*Last updated: October 26, 2025*

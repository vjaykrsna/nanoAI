package com.vjaykrsna.nanoai.testing

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult

/**
 * Test utilities for NanoAIResult assertions. Provides convenient methods to test NanoAIResult
 * instances in unit tests.
 */

/** Assert that the result is a success and return the value */
fun <T> NanoAIResult<T>.assertSuccess(): T {
  assertThat(this).isInstanceOf(NanoAIResult.Success::class.java)
  return (this as NanoAIResult.Success<T>).value
}

/** Assert that the result is a success */
fun <T> NanoAIResult<T>.assertIsSuccess() {
  assertThat(this).isInstanceOf(NanoAIResult.Success::class.java)
}

/** Assert that the result is a recoverable error and return the error */
fun <T> NanoAIResult<T>.assertRecoverableError(): NanoAIResult.RecoverableError {
  assertThat(this).isInstanceOf(NanoAIResult.RecoverableError::class.java)
  return this as NanoAIResult.RecoverableError
}

/** Assert that the result is a fatal error and return the error */
fun <T> NanoAIResult<T>.assertFatalError(): NanoAIResult.FatalError {
  assertThat(this).isInstanceOf(NanoAIResult.FatalError::class.java)
  return this as NanoAIResult.FatalError
}

/** Assert that the result is a recoverable error with the specified message */
fun <T> NanoAIResult<T>.assertRecoverableErrorWithMessage(expectedMessage: String) {
  val error = assertRecoverableError()
  assertThat(error.message).isEqualTo(expectedMessage)
}

/** Assert that the result is a recoverable error with the specified exception type */
inline fun <reified T : Throwable> NanoAIResult<*>.assertRecoverableErrorWithException() {
  val error = assertRecoverableError()
  assertThat(error.cause).isInstanceOf(T::class.java)
}

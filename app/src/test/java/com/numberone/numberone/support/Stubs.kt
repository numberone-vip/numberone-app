package com.numberone.numberone.support

import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

/**
 * A suspend interface function that returns Result is called expecting the underlying value,
 * not the wrapper. Unwrap the stubbed Result so success and failure reach the caller correctly.
 */
private val resultValueField by lazy {
    Result::class.java.declaredFields.first { !Modifier.isStatic(it.modifiers) }.apply { isAccessible = true }
}

fun unwrapResult(value: Any?): Any? =
    if (value is Result<*>) resultValueField.get(value) else value

/**
 * Builds an implementation of interface [T] at runtime. [answer] receives the called method
 * name and returns the value to hand back. Unexpected calls should throw inside [answer], so
 * a test fails loudly instead of silently receiving null.
 */
inline fun <reified T : Any> stubOf(crossinline answer: (String) -> Any?): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        when (method.declaringClass) {
            Any::class.java -> when (method.name) {
                "toString" -> "stub:${T::class.java.simpleName}"
                "hashCode" -> 0
                else -> false
            }
            // Kotlin mangles names of functions returning an inline class (Result), e.g. signIn-0E7RQCE.
            else -> unwrapResult(answer(method.name.substringBefore('-')))
        }
    } as T

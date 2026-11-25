package com.numberone.numberone.support

import java.lang.reflect.Proxy

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
            else -> answer(method.name)
        }
    } as T

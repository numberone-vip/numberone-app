package com.numberone.numberone.core.security

import com.numberone.numberone.core.localization.tr
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Fingerprint sign-in.
 *
 * The saved password is encrypted with an AES-GCM key that lives in the Android Keystore and
 * can only be used right after a successful fingerprint match (BiometricPrompt + CryptoObject).
 * Without the fingerprint the stored blob cannot be decrypted. If the device's fingerprints
 * change, Android invalidates the key and the user simply signs in with a password again.
 */
object BiometricAuthManager {
    private const val PREFS = "number_one_biometric_login"
    private const val KEY_EMAIL = "email"
    private const val KEY_CIPHER = "cipher_text"
    private const val KEY_IV = "iv"
    private const val KEY_DECLINED = "offer_declined"
    private const val KEY_ALIAS = "number_one_biometric_login_key"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** True when the device has a strong biometric enrolled and ready. */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun isEnabled(context: Context): Boolean = prefs(context).contains(KEY_CIPHER)

    fun isEnabledFor(context: Context, email: String): Boolean =
        isEnabled(context) && prefs(context).getString(KEY_EMAIL, null).equals(email.trim(), ignoreCase = true)

    fun isOfferDeclined(context: Context): Boolean = prefs(context).getBoolean(KEY_DECLINED, false)

    fun setOfferDeclined(context: Context, declined: Boolean) {
        prefs(context).edit().putBoolean(KEY_DECLINED, declined).apply()
    }

    /** Forgets the saved credentials and deletes the Keystore key. */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_EMAIL).remove(KEY_CIPHER).remove(KEY_IV).apply()
        try {
            deleteKey()
        } catch (e: Exception) {
            // Nothing to delete or Keystore unavailable: the stored blob is already gone.
        }
    }

    /** Saves [email]/[password] after the user confirms with their fingerprint. */
    fun enable(activity: FragmentActivity, email: String, password: String, onDone: (Boolean) -> Unit) {
        val cipher = try {
            deleteKey()
            Cipher.getInstance(TRANSFORMATION).also { it.init(Cipher.ENCRYPT_MODE, createKey()) }
        } catch (e: Exception) {
            onDone(false)
            return
        }
        showPrompt(
            activity = activity,
            cipher = cipher,
            title = tr("تفعيل الدخول بالبصمة"),
            subtitle = tr("أكّد بصمتك لحفظ بيانات الدخول مشفّرة على جهازك"),
            onSuccess = { authed ->
                val saved = try {
                    val encrypted = authed.doFinal(password.toByteArray(Charsets.UTF_8))
                    prefs(activity).edit()
                        .putString(KEY_EMAIL, email.trim())
                        .putString(KEY_CIPHER, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                        .putString(KEY_IV, Base64.encodeToString(authed.iv, Base64.NO_WRAP))
                        .putBoolean(KEY_DECLINED, false)
                        .commit()
                } catch (e: Exception) {
                    false
                }
                if (!saved) clear(activity)
                onDone(saved)
            },
            onFailure = { onDone(false) }
        )
    }

    /** Asks for the fingerprint and, on success, returns the saved email and password. */
    fun unlock(activity: FragmentActivity, onSuccess: (String, String) -> Unit, onError: (String) -> Unit) {
        val store = prefs(activity)
        val email = store.getString(KEY_EMAIL, null)
        val cipherText = store.getString(KEY_CIPHER, null)
        val iv = store.getString(KEY_IV, null)
        if (email == null || cipherText == null || iv == null) {
            onError(tr("لم يتم تفعيل الدخول بالبصمة بعد."))
            return
        }
        val cipher = try {
            Cipher.getInstance(TRANSFORMATION).also {
                it.init(Cipher.DECRYPT_MODE, existingKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            clear(activity)
            onError(tr("تغيّرت بصمات الجهاز. سجّل الدخول بكلمة المرور ثم فعّل البصمة من جديد."))
            return
        } catch (e: Exception) {
            clear(activity)
            onError(tr("تعذر استخدام الدخول بالبصمة. سجّل الدخول بكلمة المرور."))
            return
        }
        showPrompt(
            activity = activity,
            cipher = cipher,
            title = tr("الدخول بالبصمة"),
            subtitle = tr("ضع إصبعك على المستشعر للمتابعة"),
            onSuccess = { authed ->
                val password: String? = try {
                    String(authed.doFinal(Base64.decode(cipherText, Base64.NO_WRAP)), Charsets.UTF_8)
                } catch (e: Exception) {
                    null
                }
                if (password == null) onError(tr("تعذر التحقق بالبصمة. حاول مرة أخرى.")) else onSuccess(email, password)
            },
            onFailure = { code ->
                when (code) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> Unit
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                        onError(tr("تم إيقاف البصمة مؤقتاً بسبب محاولات كثيرة. حاول لاحقاً أو استخدم كلمة المرور."))
                    else -> onError(tr("تعذر التحقق بالبصمة. حاول مرة أخرى."))
                }
            }
        )
    }

    private fun showPrompt(
        activity: FragmentActivity,
        cipher: Cipher,
        title: String,
        subtitle: String,
        onSuccess: (Cipher) -> Unit,
        onFailure: (Int) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val authed = result.cryptoObject?.cipher
                if (authed == null) onFailure(BiometricPrompt.ERROR_VENDOR) else onSuccess(authed)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFailure(errorCode)
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(tr("إلغاء"))
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun deleteKey() {
        val store = keyStore()
        if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
    }

    private fun existingKey(): SecretKey = keyStore().getKey(KEY_ALIAS, null) as SecretKey

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        return generator.generateKey()
    }
}

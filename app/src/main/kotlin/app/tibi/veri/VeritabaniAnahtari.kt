package app.tibi.veri

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SQLCipher parolası: 32 rastgele bayt. Parola, Android Keystore'daki (uygulama dışına çıkmayan)
 * AES anahtarıyla şifrelenip uygulamanın özel tercih dosyasında saklanır.
 */
class VeritabaniAnahtari(private val context: Context) {
    private val tercih get() = context.getSharedPreferences("tibi_anahtar", Context.MODE_PRIVATE)

    fun parola(): ByteArray {
        tercih.getString(ALAN, null)?.let { return coz(Base64.decode(it, Base64.NO_WRAP)) }
        val yeni = ByteArray(32).also { SecureRandom().nextBytes(it) }
        tercih.edit().putString(ALAN, Base64.encodeToString(sifrele(yeni), Base64.NO_WRAP)).commit()
        return yeni
    }

    private fun keystoreAnahtari(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(TAKMA_AD, null) as? SecretKey)?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        kg.init(
            KeyGenParameterSpec.Builder(TAKMA_AD, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    /** Çıktı: 12 bayt IV + şifreli metin. */
    private fun sifrele(duz: ByteArray): ByteArray {
        val c = Cipher.getInstance(DONUSUM).apply { init(Cipher.ENCRYPT_MODE, keystoreAnahtari()) }
        return c.iv + c.doFinal(duz)
    }

    private fun coz(veri: ByteArray): ByteArray {
        val c = Cipher.getInstance(DONUSUM)
        c.init(Cipher.DECRYPT_MODE, keystoreAnahtari(), GCMParameterSpec(128, veri, 0, 12))
        return c.doFinal(veri, 12, veri.size - 12)
    }

    private companion object {
        const val TAKMA_AD = "tibi_veritabani_anahtari"
        const val ALAN = "sifreli_parola"
        const val DONUSUM = "AES/GCM/NoPadding"
    }
}

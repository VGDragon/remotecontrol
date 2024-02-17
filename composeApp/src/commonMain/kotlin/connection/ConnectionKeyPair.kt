package connection

import com.google.gson.Gson
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
import javax.crypto.Cipher


class ConnectionKeyPair (val keyOwner: String,
                         val keyAlias: String,
                         val keyCryptoMethode: String = ConnectionKeyPair.baseKeyCryptoMethode,
                         val keySize: Int = 2048){
    var ownPublicKey: String = ""
    var ownPrivateKey: String = ""

    var publicKeyTarget: String = ""


    fun generateKeyPair(): ConnectionKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(keyCryptoMethode)
        keyPairGenerator.initialize(keySize)
        val keyPair = keyPairGenerator.generateKeyPair()
        ownPublicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        ownPrivateKey = Base64.getEncoder().encodeToString(keyPair.private.encoded)

        return this
    }
    fun encrypt(message: String): String {
        val publicKey: PublicKey = java.security.KeyFactory.getInstance("RSA")
            .generatePublic(java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(ownPublicKey)))

        val cipher = Cipher.getInstance(keyCryptoMethode)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    fun decrypt(encryptedMessage: String): String {
        val privateKey: PrivateKey = java.security.KeyFactory.getInstance("RSA")
            .generatePrivate(java.security.spec.PKCS8EncodedKeySpec(Base64.getDecoder().decode(ownPrivateKey)))

        val cipher = Cipher.getInstance(keyCryptoMethode)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage))
        return String(decryptedBytes, Charsets.UTF_8)
    }
    fun saveKeyPair(){
        // Save the key pair to a file
        val folder = java.io.File(GlobalVariables.keyPairsFolder())
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = java.io.File(GlobalVariables.keyPairsFolder(), keyAlias)
        file.writeText(Gson().toJson(this))
    }
    fun deleteKeyPair(){
        val file = java.io.File(GlobalVariables.keyPairsFolder(), keyAlias)
        if (file.exists()){
            file.delete()
        }
    }

    companion object {
        fun loadFile(keyAlias: String): ConnectionKeyPair? {
            val file = java.io.File(GlobalVariables.keyPairsFolder(), keyAlias)
            if (!file.exists())
                return null
            return Gson().fromJson(file.readText(), ConnectionKeyPair::class.java)
        }
        val baseKeyCryptoMethode = "RSA"
    }

}
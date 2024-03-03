package connection

import com.google.gson.Gson
import java.io.File
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.util.*
import javax.crypto.Cipher


class ConnectionKeyPair (val keyOwner: String,
                         val keyAlias: String,
                         val keyCryptoMethode: String = ConnectionKeyPair.baseKeyCryptoMethode,
                         val keyCryptoMethodeInstance: String = ConnectionKeyPair.baseKeyCryptoMethodeInstance,
                         val keySize: Int = 2048){
    var ownPublicKey: String = ""
    var ownPrivateKey: String = ""

    var privateKeyTarget: String = ""


    fun generateKeyPair(): ConnectionKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(keyCryptoMethode)
        keyPairGenerator.initialize(keySize)
        val keyPair = keyPairGenerator.generateKeyPair()
        ownPublicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        ownPrivateKey = Base64.getEncoder().encodeToString(keyPair.private.encoded)

        return this
    }
    fun encrypt(message: String): String {
        val publicKey: PublicKey = java.security.KeyFactory.getInstance(keyCryptoMethode)
            .generatePublic(java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(ownPublicKey)))

        val cipher = Cipher.getInstance(keyCryptoMethodeInstance)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        // cut message
        val messageList: MutableList<String> = mutableListOf()
        var messageCopy: String = message
        while (messageCopy.length > 0){
            val cutedMessage = if (messageCopy.length < 100){
                val tempMessage = messageCopy.substring(0, messageCopy.length)
                messageCopy = ""
                tempMessage
            } else {
                val tempMessage = messageCopy.substring(0, 100)
                messageCopy = messageCopy.substring(100)
                tempMessage
            }
            val encryptedBytes = cipher.doFinal(cutedMessage.toByteArray(Charsets.UTF_8))
            messageList.add(Base64.getEncoder().encodeToString(encryptedBytes))
        }
        // crate string from the messageList
        return messageList.joinToString(separator = " \n")
    }

    fun decrypt(encryptedMessage: String): String {
        val privateKey: PrivateKey = java.security.KeyFactory.getInstance(keyCryptoMethode)
            .generatePrivate(java.security.spec.PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyTarget)))

        val cipher = Cipher.getInstance(keyCryptoMethodeInstance)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        // cut message
        val messageList = encryptedMessage.split(" \n")
        val decryptedMessageList: MutableList<String> = mutableListOf()
        for (tempEncryptedMessage in messageList){
            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(tempEncryptedMessage))
            decryptedMessageList.add(String(decryptedBytes, Charsets.UTF_8))
        }
        return decryptedMessageList.joinToString(separator = "")
    }
    fun saveKeyPair(fileEnding: String = ""){
        // Save the key pair to a file
        val folder = File(GlobalVariables.keyPairsFolder())
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(GlobalVariables.keyPairsFolder(), keyAlias + fileEnding)
        file.writeText(Gson().toJson(this))
    }
    fun deleteKeyPair(fileEnding: String = ""){
        val file = File(GlobalVariables.keyPairsFolder(), keyAlias + fileEnding)
        if (file.exists()){
            file.delete()
        }
    }

    companion object {
        fun loadFile(keyAlias: String, fileEnding: String = ""): ConnectionKeyPair? {
            val file = File(GlobalVariables.keyPairsFolder(), keyAlias + fileEnding)
            if (!file.exists())
                return null
            return Gson().fromJson(file.readText(), ConnectionKeyPair::class.java)
        }
        val baseKeyCryptoMethode = "RSA"
        val baseKeyCryptoMethodeInstance = "RSA/ECB/OAEPWithSHA1AndMGF1Padding"
    }

}
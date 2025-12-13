package com.metelci.ardunakon.data

import android.content.Context
import android.util.Log
import com.metelci.ardunakon.model.ButtonConfig
import com.metelci.ardunakon.model.defaultButtonConfigs
import com.metelci.ardunakon.security.AuthRequiredException
import com.metelci.ardunakon.security.CryptoEngine
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val buttonConfigs: List<ButtonConfig>,
    val sensitivity: Float = 1.0f // 0.1 to 2.0
)

class ProfileManager(
    private val context: Context,
    private val securityManager: CryptoEngine = com.metelci.ardunakon.security.SecurityManager()
) {

    private val fileName = "profiles.json"

    suspend fun saveProfiles(profiles: List<Profile>) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            profiles.forEach { profile ->
                val profileObj = JSONObject()
                profileObj.put("id", profile.id)
                profileObj.put("name", profile.name)
                profileObj.put("sensitivity", profile.sensitivity.toDouble())

                val buttonsArray = JSONArray()
                profile.buttonConfigs.forEach { btn ->
                    val btnObj = JSONObject()
                    btnObj.put("id", btn.id)
                    btnObj.put("label", btn.label)
                    btnObj.put("command", btn.command)
                    btnObj.put("color", btn.colorHex)
                    buttonsArray.put(btnObj)
                }
                profileObj.put("buttons", buttonsArray)
                jsonArray.put(profileObj)
            }

            val jsonString = jsonArray.toString()
            val encryptedData = securityManager.encrypt(jsonString)

            val file = File(context.filesDir, fileName)
            file.writeText(encryptedData)
        } catch (e: AuthRequiredException) {
            throw e
        } catch (e: Exception) {
            Log.e("ProfileManager", "Error saving profiles", e)
        }
    }

    suspend fun loadProfiles(): List<Profile> = withContext(Dispatchers.IO) {
        val profiles = mutableListOf<Profile>()
        val file = File(context.filesDir, fileName)

        if (!file.exists()) {
            return@withContext createDefaultProfiles()
        }

        try {
            val fileContent = file.readText()

            // Attempt to decrypt
            val jsonString = securityManager.decrypt(fileContent)

            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val profileObj = jsonArray.getJSONObject(i)

                // Handle legacy profiles without ID
                val id = if (profileObj.has("id")) profileObj.getString("id") else UUID.randomUUID().toString()
                val name = profileObj.getString("name")
                val sensitivity = profileObj.optDouble("sensitivity", 1.0).toFloat()

                val buttons = mutableListOf<ButtonConfig>()
                val buttonsArray = profileObj.getJSONArray("buttons")

                for (j in 0 until buttonsArray.length()) {
                    val btnObj = buttonsArray.getJSONObject(j)
                    buttons.add(
                        ButtonConfig(
                            id = btnObj.getInt("id"),
                            label = btnObj.getString("label"),
                            command = btnObj.getString("command"),
                            colorHex = btnObj.optLong("color", 0xFF90CAF9)
                        )
                    )
                }
                profiles.add(Profile(id, name, buttons, sensitivity))
            }
        } catch (e: Exception) {
            if (e is AuthRequiredException) throw e
            Log.e("ProfileManager", "Error loading profiles", e)
            return@withContext createDefaultProfiles()
        }

        return@withContext profiles
    }

    fun createDefaultProfiles(): List<Profile> = listOf(
        Profile(
            name = "Rover (Car Mode)",
            buttonConfigs = defaultButtonConfigs,
            sensitivity = 1.0f
        )
    )
}

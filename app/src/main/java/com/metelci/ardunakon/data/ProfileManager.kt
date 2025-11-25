package com.metelci.ardunakon.data

import android.content.Context
import android.util.Log
import com.metelci.ardunakon.model.ButtonConfig
import com.metelci.ardunakon.model.defaultButtonConfigs
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

import java.util.UUID

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val buttonConfigs: List<ButtonConfig>,
    val isThrottleUnidirectional: Boolean = false, // false = -100 to 100 (Car), true = 0 to 100 (Drone/ESC)
    val sensitivity: Float = 1.0f // 0.1 to 2.0
)

class ProfileManager(private val context: Context) {

    private val fileName = "profiles.json"
    private val securityManager = com.metelci.ardunakon.security.SecurityManager()

    fun saveProfiles(profiles: List<Profile>) {
        try {
            val jsonArray = JSONArray()
            profiles.forEach { profile ->
                val profileObj = JSONObject()
                profileObj.put("id", profile.id)
                profileObj.put("name", profile.name)
                profileObj.put("isThrottleUnidirectional", profile.isThrottleUnidirectional)
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
        } catch (e: Exception) {
            Log.e("ProfileManager", "Error saving profiles", e)
        }
    }

    fun loadProfiles(): List<Profile> {
        val profiles = mutableListOf<Profile>()
        val file = File(context.filesDir, fileName)
        
        if (!file.exists()) {
            return createDefaultProfiles()
        }

        try {
            val fileContent = file.readText()
            
            // Attempt to decrypt
            val jsonString = try {
                securityManager.decrypt(fileContent)
            } catch (e: Exception) {
                // Decryption failed, likely legacy plain text
                Log.w("ProfileManager", "Decryption failed, assuming plain text migration", e)
                fileContent
            }
            
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val profileObj = jsonArray.getJSONObject(i)
                
                // Handle legacy profiles without ID
                val id = if (profileObj.has("id")) profileObj.getString("id") else UUID.randomUUID().toString()
                val name = profileObj.getString("name")
                val isUnidirectional = profileObj.optBoolean("isThrottleUnidirectional", false)
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
                profiles.add(Profile(id, name, buttons, isUnidirectional, sensitivity))
            }
            
            // If we successfully loaded plain text, save it back as encrypted immediately
            if (fileContent == jsonString) {
                saveProfiles(profiles)
            }
            
        } catch (e: Exception) {
            Log.e("ProfileManager", "Error loading profiles", e)
            return createDefaultProfiles()
        }
        
        return profiles
    }
    
    fun createDefaultProfiles(): List<Profile> {
        return listOf(
            Profile(
                name = "Rover (Car Mode)", 
                buttonConfigs = defaultButtonConfigs, 
                isThrottleUnidirectional = false,
                sensitivity = 1.0f
            ),
            Profile(
                name = "Drone/Boat (ESC Mode)", 
                buttonConfigs = defaultButtonConfigs.map { it.copy(label = "Aux ${it.id}") }, 
                isThrottleUnidirectional = true,
                sensitivity = 1.0f
            )
        )
    }
}

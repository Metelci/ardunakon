package com.metelci.ardunakon.data

import android.content.Context
import android.util.Log
import com.metelci.ardunakon.model.ButtonConfig
import com.metelci.ardunakon.model.defaultButtonConfigs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

import java.util.UUID

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val buttonConfigs: List<ButtonConfig>,
    val isThrottleUnidirectional: Boolean = false, // false = -100 to 100 (Car), true = 0 to 100 (Drone/ESC)
    val sensitivity: Float = 1.0f, // 0.1 to 2.0
    val auxAssignments: List<AuxAssignment> = emptyList()
)

data class AuxAssignment(
    val id: Int,
    val slot: Int,
    val servoId: Int,
    val role: String = ""
)

class ProfileManager(private val context: Context) {

    private val fileName = "profiles.json"
    private val securityManager = com.metelci.ardunakon.security.SecurityManager()

    suspend fun saveProfiles(profiles: List<Profile>) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            profiles.forEach { profile ->
                val profileObj = JSONObject()
                profileObj.put("id", profile.id)
                profileObj.put("name", profile.name)
                profileObj.put("isThrottleUnidirectional", profile.isThrottleUnidirectional)
                profileObj.put("sensitivity", profile.sensitivity.toDouble())
                val assignmentsArray = JSONArray()
                profile.auxAssignments.forEach { assign ->
                    val assignObj = JSONObject()
                    assignObj.put("id", assign.id)
                    assignObj.put("slot", assign.slot)
                    assignObj.put("servoId", assign.servoId)
                    assignObj.put("role", assign.role)
                    assignmentsArray.put(assignObj)
                }
                profileObj.put("auxAssignments", assignmentsArray)

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

    suspend fun loadProfiles(): List<Profile> = withContext(Dispatchers.IO) {
        val profiles = mutableListOf<Profile>()
        val file = File(context.filesDir, fileName)

        if (!file.exists()) {
            return@withContext createDefaultProfiles()
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
                val assignments = mutableListOf<AuxAssignment>()
                if (profileObj.has("auxAssignments")) {
                    val assignmentsArray = profileObj.getJSONArray("auxAssignments")
                    for (k in 0 until assignmentsArray.length()) {
                        val assignObj = assignmentsArray.getJSONObject(k)
                        assignments.add(
                            AuxAssignment(
                                id = assignObj.getInt("id"),
                                slot = assignObj.optInt("slot", 0),
                                servoId = assignObj.optInt("servoId", assignObj.getInt("id")),
                                role = assignObj.optString("role", "")
                            )
                        )
                    }
                }

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
                profiles.add(Profile(id, name, buttons, isUnidirectional, sensitivity, assignments))
            }

            // If we successfully loaded plain text, save it back as encrypted immediately
            if (fileContent == jsonString) {
                saveProfiles(profiles)
            }

        } catch (e: Exception) {
            Log.e("ProfileManager", "Error loading profiles", e)
            return@withContext createDefaultProfiles()
        }

        return@withContext profiles
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

package com.example.rpg

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Attributes(
    @Json(name = "strength") val strength: Int,       // FOR
    @Json(name = "agility") val agility: Int,         // AGI
    @Json(name = "intelligence") val intelligence: Int, // INT
    @Json(name = "vitality") val vitality: Int,       // VIT
    @Json(name = "perception") val perception: Int,   // PER
    @Json(name = "willpower") val willpower: Int      // WIL
)

@JsonClass(generateAdapter = true)
data class Item(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "type") val type: String = "Item", // Weapon, Armor, Consumable, KeyItem
    @Json(name = "effect") val effect: String? = null,
    @Json(name = "value") val value: Int = 0
)

@JsonClass(generateAdapter = true)
data class Skill(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "cost") val cost: Int = 0,
    @Json(name = "type") val type: String = "Physical", // Physical, Lunar Magic, Gravity Magic, Mental
    @Json(name = "power") val power: Int = 10
)

@JsonClass(generateAdapter = true)
data class PlayerState(
    @Json(name = "name") val name: String = "",
    @Json(name = "gender") val gender: String = "",
    @Json(name = "sexuality") val sexuality: String = "",
    @Json(name = "race") val race: String = "",
    @Json(name = "className") val className: String = "",
    @Json(name = "subclass") val subclass: String = "",
    @Json(name = "appearance") val appearance: String = "",
    @Json(name = "level") val level: Int = 1,
    @Json(name = "experience") val experience: Int = 0,
    @Json(name = "maxExperience") val maxExperience: Int = 100,
    @Json(name = "hp") val hp: Int = 80,
    @Json(name = "maxHp") val maxHp: Int = 80,
    @Json(name = "mp") val mp: Int = 40,
    @Json(name = "maxMp") val maxMp: Int = 40,
    @Json(name = "gold") val gold: Int = 50,
    @Json(name = "attributes") val attributes: Attributes = Attributes(10, 10, 10, 10, 10, 10),
    @Json(name = "unassignedPoints") val unassignedPoints: Int = 5,
    @Json(name = "inventory") val inventory: List<Item> = emptyList(),
    @Json(name = "equippedWeapon") val equippedWeapon: Item? = null,
    @Json(name = "equippedArmor") val equippedArmor: Item? = null,
    @Json(name = "grimoire") val grimoire: List<Skill> = emptyList(),
    @Json(name = "titles") val titles: List<String> = emptyList(),
    @Json(name = "scars") val scars: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class NpcState(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "affinity") val affinity: Int = 0, // -100 to 100
    @Json(name = "emotion") val emotion: String = "Neutro", // Medo, Raiva, Confiança, Ambição
    @Json(name = "memory") val memory: List<String> = emptyList(),
    @Json(name = "activeRole") val activeRole: String = ""
)

@JsonClass(generateAdapter = true)
data class WorldState(
    @Json(name = "region") val region: String = "Reino Sombrio",
    @Json(name = "timeOfDay") val timeOfDay: String = "Manhã", // Manhã, Tarde, Noite, Madrugada
    @Json(name = "rotLevel") val rotLevel: Int = 5, // 0 to 100 (percentage of Podridão)
    @Json(name = "locationDescription") val locationDescription: String = "Uma floresta densa envolta em névoa lunar estagnada.",
    @Json(name = "activeQuests") val activeQuests: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LogEntry(
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "speaker") val speaker: String, // "Narrador", "Jogador", "NPC: [Nome]"
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class GameState(
    @Json(name = "id") val id: Int = 1,
    @Json(name = "creationStep") val creationStep: String = "NOT_STARTED", // NOT_STARTED, NOME, APARENCIA, GENERO, SEXUALIDADE, RACE, CLASS, SUBCLASS, RUNNING
    @Json(name = "playerState") val playerState: PlayerState = PlayerState(),
    @Json(name = "worldState") val worldState: WorldState = WorldState(),
    @Json(name = "npcs") val npcs: List<NpcState> = emptyList(),
    @Json(name = "history") val history: List<LogEntry> = emptyList(),
    @Json(name = "options") val options: List<String> = emptyList() // Curated options for the player based on state
)

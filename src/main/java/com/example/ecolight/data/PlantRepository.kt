package com.example.ecolight.data

import kotlinx.coroutines.delay

data class PlantLightInfo(
    val name: String,
    val minLux: Int,
    val maxLux: Int,
    val description: String
)

object PlantRepository {
    
    // Simulating an internet database or API
    private val plantDatabase = mapOf(
        // --- High Light / Direct Sun Plants (> 5000 lux) ---
        "cactus" to PlantLightInfo("Cactus", 5000, 50000, "Necesita luz directa y muy brillante."),
        "suculenta" to PlantLightInfo("Suculenta", 5000, 50000, "Requiere mucha luz, preferiblemente sol directo."),
        "succulent" to PlantLightInfo("Succulent", 5000, 50000, "Requires lots of light, preferably direct sun."),
        "lavanda" to PlantLightInfo("Lavanda", 10000, 100000, "Sol directo intenso."),
        "lavender" to PlantLightInfo("Lavender", 10000, 100000, "Intense direct sun."),
        "albahaca" to PlantLightInfo("Albahaca", 5000, 50000, "Sol directo al menos 6 horas."),
        "basil" to PlantLightInfo("Basil", 5000, 50000, "Direct sun at least 6 hours."),
        "romero" to PlantLightInfo("Romero", 10000, 80000, "Necesita pleno sol para crecer bien."),
        "rosemary" to PlantLightInfo("Rosemary", 10000, 80000, "Needs full sun to thrive."),
        "geranio" to PlantLightInfo("Geranio", 4000, 30000, "Luz solar directa o muy brillante."),
        "geranium" to PlantLightInfo("Geranium", 4000, 30000, "Direct sunlight or very bright light."),
        "olivo" to PlantLightInfo("Olivo", 10000, 90000, "Sol directo, ideal para exteriores soleados."),
        "olive tree" to PlantLightInfo("Olive Tree", 10000, 90000, "Direct sun, ideal for sunny outdoors."),
        "tomate" to PlantLightInfo("Tomate", 20000, 100000, "Requiere mucha luz solar para dar fruto."),
        "tomato" to PlantLightInfo("Tomato", 20000, 100000, "Requires lots of sunlight to fruit."),

        // --- Bright Indirect Light / Partial Shade (1000 - 5000 lux) ---
        "monstera" to PlantLightInfo("Monstera (Costilla de Adán)", 1000, 4000, "Prefiere luz indirecta brillante."),
        "ficus" to PlantLightInfo("Ficus", 2000, 10000, "Necesita luz brillante pero indirecta."),
        "ficus elastica" to PlantLightInfo("Ficus Elástica (Hule)", 1500, 5000, "Luz brillante indirecta, tolera algo de sombra."),
        "orquidea" to PlantLightInfo("Orquídea", 1500, 3500, "Luz filtrada o indirecta brillante."),
        "orchid" to PlantLightInfo("Orchid", 1500, 3500, "Filtered or bright indirect light."),
        "aloe vera" to PlantLightInfo("Aloe Vera", 4000, 15000, "Luz brillante, algo de sol directo está bien."),
        "jade" to PlantLightInfo("Árbol de Jade", 3000, 10000, "Luz brillante, algunas horas de sol directo."),
        "photos" to PlantLightInfo("Potos", 800, 3000, "Prefiere luz media, pero tolera baja luz."),
        "pothos" to PlantLightInfo("Pothos", 800, 3000, "Prefers medium light, but tolerates low light."),
        "dracaena" to PlantLightInfo("Dracaena (Palo de Brasil)", 1000, 3000, "Luz filtrada, evitar sol directo que quema las hojas."),
        "croton" to PlantLightInfo("Crotón", 2000, 8000, "Necesita luz brillante para mantener sus colores."),
        "violeta africana" to PlantLightInfo("Violeta Africana", 1000, 2500, "Luz indirecta media, ideal cerca de ventanas."),
        "african violet" to PlantLightInfo("African Violet", 1000, 2500, "Medium indirect light, ideal near windows."),
        "begonia" to PlantLightInfo("Begonia", 1000, 3000, "Luz brillante indirecta, sombra parcial."),
        "bambu" to PlantLightInfo("Bambú de la Suerte", 1000, 3000, "Luz brillante filtrada."),
        "lucky bamboo" to PlantLightInfo("Lucky Bamboo", 1000, 3000, "Bright filtered light."),
        "hiedra" to PlantLightInfo("Hiedra Inglesa", 1000, 4000, "Luz indirecta media a brillante."),
        "english ivy" to PlantLightInfo("English Ivy", 1000, 4000, "Medium to bright indirect light."),
        "pilea" to PlantLightInfo("Pilea (Planta China del Dinero)", 1500, 4000, "Luz brillante indirecta."),

        // --- Low Light / Shade (< 1000 lux) ---
        "helecho" to PlantLightInfo("Helecho", 500, 2500, "Prefiere sombra o luz indirecta baja."),
        "fern" to PlantLightInfo("Fern", 500, 2500, "Prefers shade or low indirect light."),
        "sansevieria" to PlantLightInfo("Sansevieria (Lengua de suegra)", 500, 5000, "Muy tolerante, desde sombra hasta luz brillante."),
        "snake plant" to PlantLightInfo("Snake Plant", 500, 5000, "Very tolerant, low to bright light."),
        "espatifilo" to PlantLightInfo("Espatifilo (Cuna de Moisés)", 500, 2000, "Luz baja a media, evitar sol directo."),
        "peace lily" to PlantLightInfo("Peace Lily", 500, 2000, "Low to medium light, avoid direct sun."),
        "calathea" to PlantLightInfo("Calathea", 400, 1500, "Sombra parcial, luz indirecta baja."),
        "zamioculca" to PlantLightInfo("Zamioculca (ZZ Plant)", 300, 2500, "Tolera muy poca luz, excelente para oficinas."),
        "zz plant" to PlantLightInfo("ZZ Plant", 300, 2500, "Tolerates very low light, great for offices."),
        "dieffenbachia" to PlantLightInfo("Dieffenbachia (Lotería)", 500, 2000, "Luz filtrada baja a media."),
        "aglaonema" to PlantLightInfo("Aglaonema", 500, 2000, "Tolera poca luz, aunque los colores mejoran con más luz."),
        "cinta" to PlantLightInfo("Cinta (Malamadre/Spider Plant)", 800, 2500, "Se adapta bien a luz media y baja."),
        "spider plant" to PlantLightInfo("Spider Plant", 800, 2500, "Adapts well to medium and low light."),
        "filodendro" to PlantLightInfo("Filodendro", 500, 2500, "Luz indirecta media o baja."),
        "philodendron" to PlantLightInfo("Philodendron", 500, 2500, "Medium or low indirect light."),
        "bromelia" to PlantLightInfo("Bromelia", 800, 3000, "Luz indirecta, tolera sombra parcial.")
    )

    // Simulate an async internet search
    suspend fun searchPlantRequirements(query: String): PlantLightInfo? {
        // Simulate network latency
        delay(1000) // Reduced delay slightly for better UX
        
        val normalizedQuery = query.lowercase().trim()
        
        // Try to find partial matches
        return plantDatabase.entries.find { normalizedQuery.contains(it.key) || it.key.contains(normalizedQuery) }?.value
    }
}
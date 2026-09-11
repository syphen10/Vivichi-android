package com.vivichi.app.data

object DefaultContent {
    val defaultHabits: List<Habit> = listOf(
        Habit("h1", "Brush Teeth AM", "🪥", 3, "low", "07:30", true, false),
        Habit("h2", "Drink Water", "💧", 3, "low", "08:00", true, false),
        Habit("h3", "Eat Breakfast", "🍳", 3, "low", "08:30", true, false),
        Habit("h4", "Lunch", "🥗", 3, "low", "13:00", true, false),
        Habit("h5", "Study / Work", "📚", 10, "high", "09:00", true, false),
        Habit("h6", "Exercise", "🏃", 10, "high", "17:00", false, false),
        Habit("h7", "Read", "📖", 5, "medium", "20:00", false, false),
        Habit("h8", "Brush Teeth PM", "🪥", 3, "low", "21:30", true, false),
        Habit("h9", "Sleep On Time", "😴", 5, "medium", "23:00", true, false)
    )
}

data class PetSpecies(val id: String, val name: String, val emoji: String)

val PETS = listOf(
    PetSpecies("cat", "Kitty", "🐱"),
    PetSpecies("bunny", "BunBun", "🐰"),
    PetSpecies("fox", "Foxy", "🦊"),
    PetSpecies("panda", "Panda", "🐼"),
    PetSpecies("peng", "Pengu", "🐧"),
    PetSpecies("bear", "Ted", "🐻"),
    PetSpecies("dragon", "Dragon", "🐉"),
    PetSpecies("dog", "Doggo", "🐶")
)

fun petEmoji(species: String) = PETS.find { it.id == species }?.emoji ?: "🐾"
fun petSpeciesName(species: String) = PETS.find { it.id == species }?.name ?: species

data class OutfitInfo(val id: String, val name: String, val emoji: String, val level: Int, val seasonal: Boolean = false, val season: String? = null)

val OUTFITS = listOf(
    OutfitInfo("default", "Default", "⭐", 1),
    OutfitInfo("cozy", "Cozy", "🧣", 3),
    OutfitInfo("sporty", "Sporty", "⚡", 5),
    OutfitInfo("royal", "Royal", "👑", 10),
    OutfitInfo("cosmic", "Cosmic", "🌌", 15),
    OutfitInfo("golden", "Golden", "✨", 20)
)

val SEASONAL_OUTFITS = listOf(
    OutfitInfo("spring", "Spring", "🌸", 1, true, "spring"),
    OutfitInfo("summer", "Summer", "☀️", 1, true, "summer"),
    OutfitInfo("autumn", "Autumn", "🍁", 1, true, "autumn"),
    OutfitInfo("winter", "Winter", "❄️", 1, true, "winter")
)

data class TitleInfo(
    val id: String,
    val name: String,
    val emoji: String,
    val requirement: String,
    val check: (streak: Int, bestStreak: Int, totalXP: Int, totalDone: Int, level: Int, achievements: Achievements, cemeterySize: Int) -> Boolean
)

val TITLES = listOf(
    TitleInfo("t1", "Just Starting", "🌱", "Begin") { _, _, _, _, _, _, _ -> true },
    TitleInfo("t2", "3-Day Warrior", "⚔️", "3 day streak") { s, b, _, _, _, _, _ -> s >= 3 || b >= 3 },
    TitleInfo("t3", "Week Strong", "🔥", "7 day streak") { s, b, _, _, _, _, _ -> s >= 7 || b >= 7 },
    TitleInfo("t4", "Fortnight Beast", "💪", "14 day streak") { s, b, _, _, _, _, _ -> s >= 14 || b >= 14 },
    TitleInfo("t5", "Month Legend", "🏆", "30 day streak") { s, b, _, _, _, _, _ -> s >= 30 || b >= 30 },
    TitleInfo("t6", "XP Rookie", "⭐", "100 XP") { _, _, xp, _, _, _, _ -> xp >= 100 },
    TitleInfo("t7", "XP Hunter", "🎯", "500 XP") { _, _, xp, _, _, _, _ -> xp >= 500 },
    TitleInfo("t8", "Habit Machine", "⚙️", "50 habits") { _, _, _, d, _, _, _ -> d >= 50 },
    TitleInfo("t9", "Level 5", "🔮", "Reach Lv5") { _, _, _, _, l, _, _ -> l >= 5 },
    TitleInfo("t10", "Elite", "👑", "Reach Lv10") { _, _, _, _, l, _, _ -> l >= 10 },
    TitleInfo("t11", "Early Bird", "🐦", "Before 8am") { _, _, _, _, _, a, _ -> a.earlyBird },
    TitleInfo("t12", "Night Owl", "🦉", "After 11pm") { _, _, _, _, _, a, _ -> a.nightOwl },
    TitleInfo("t13", "Survivor", "💀", "Revive after death") { _, _, _, _, _, _, c -> c > 0 }
)

val PLAY_MESSAGES: Map<String, List<String>> = mapOf(
    "feed" to listOf(
        "*inhales entire meal in 0.3 seconds* ...more? 😋",
        "Okay I wasn't hungry but NOW I AM. Give me everything 🤤",
        "I would commit crimes for another bite of that 😳",
        "This is what happiness tastes like. This IS happiness.",
        "Michelin star. Right here. You. Michelin star. 🌟",
        "I am not crying, there's just gravy in my eyes 😭",
        "New life goal: eat this every day until I die.",
        "I have never respected a meal more than this one.",
        "Someone alert the chef. The chef is you. You are the chef. 👨‍🍳",
        "I'm saving room for seconds, thirds, and a small dessert war.",
        "This bite has changed me as a creature. Spiritually.",
        "I will be telling my future kids about this exact bite."
    ),
    "pet" to listOf(
        "Don't. Stop. I will actually cry if you stop. 😭",
        "Your hands are magic and I'm filing a patent 🐾",
        "I am completely liquid right now. You've done this to me.",
        "My whole body is vibrating and I'm not even embarrassed",
        "I've never trusted anyone more than I trust you right now 🥹",
        "10/10 pets. No notes. Do it again.",
        "I have officially melted into the floor. Send help. Actually don't.",
        "This is the safest I have ever felt in my entire life.",
        "My brain has stopped producing thoughts. Only bliss remains.",
        "I could nap for nine years right now. Nine.",
        "You found the exact spot. HOW did you find the exact spot.",
        "I am purring in a language that hasn't been invented yet."
    ),
    "play" to listOf(
        "I AM BUILT DIFFERENT. WATCH THIS— *trips immediately* 🐾",
        "AGAIN!! AGAIN!! I'M NOT EVEN TIRED— *panting heavily* 🐝",
        "I am speed. I am power. I am— *crashes into wall* …fine.",
        "The energy. The ENERGY. I am ALIVE right now!!",
        "I am going feral in the best possible way right now 🐾",
        "Coach says I have potential. Coach is also me.",
        "Nobody move. I'm about to do something incredible— *falls over*",
        "This is the Olympics and I have already won gold 🥇",
        "I've unlocked a new personal best in Being Ridiculous.",
        "Zero regrets. Several bruises. Worth it.",
        "Watch me do the thing again but slightly worse this time!",
        "I could do this literally forever. Ask me again in five minutes."
    ),
    "hug" to listOf(
        "*grabs on and REFUSES to let go* You live here now. This is home.",
        "I am storing this hug for winter. Thank you for your service. 🫢",
        "Whatever was wrong before is no longer wrong. The hug fixed it.",
        "My heart grew three sizes just now. Medically speaking.",
        "You are officially my favourite creature on this planet 🥹",
        "This is now a permanent installation. I do not intend to move.",
        "I felt that in my whole soul, not just my arms.",
        "Okay but why does this fix everything instantly",
        "Warning: emotional support hug detected. Proceeding to melt.",
        "I'm going to remember this hug for the rest of the week. Maybe longer.",
        "You give the best hugs in this timeline and several others.",
        "This is my new favourite place to exist."
    ),
    "sing" to listOf(
        "*completely wrong tune* LA LA LAAAA— is this not Beyoncé??",
        "The range!! The RANGE!! Did you catch that? Replay it in your mind.",
        "I don't need autotune. Autotune needs ME.",
        "Five Grammys. That's my minimum prediction for this performance.",
        "This is called artistic freedom 🎤",
        "That note was intentional. Every note is intentional.",
        "I'm basically a professional at this point. Basically.",
        "Encore! Encore! Nobody asked but here's another verse—",
        "I felt the whole room go quiet. That's how you know it worked.",
        "Critics will call this 'bold'. I call it 'correct'.",
        "Someone should really be recording this for the history books.",
        "That was pitch perfect. In my own personal pitch. Which I invented."
    ),
    "tickle" to listOf(
        "NO NO NO HAHAHA— I'm fine— HAHAHA I'M NOT FINE 😂",
        "OKAY I SURRENDER I SURRENDER HAHAHA 🤣",
        "My sides!! MY SIDES!! I can't— I physically cannot—",
        "I am a dignified creature— HAHAHA— a dignified— HAHAHA",
        "That's it. You've unlocked the chaos. The chaos is OUT now. 🤣",
        "STOP— okay don't stop— okay maybe stop— HAHAHA",
        "I am WHEEZING. Actually wheezing. This is a medical emergency.",
        "Ten out of ten, would get tickled again, please stop for now",
        "I've lost all muscle control. This is your fault entirely.",
        "You've found my one true weakness and I respect it.",
        "I'm going to need a full minute to recover from that one.",
        "Rude. Effective. But rude. Do it again."
    )
)

val REACT_EMOJI: Map<String, String> = mapOf(
    "feed" to "😋", "pet" to "🥰", "play" to "🤤",
    "hug" to "🥹", "sing" to "🎤", "tickle" to "🤣"
)

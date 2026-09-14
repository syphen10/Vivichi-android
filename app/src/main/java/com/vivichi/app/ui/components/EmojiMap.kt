package com.vivichi.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

// Maps the raw Unicode emoji still stored in data/DataStore (species, habit icons, outfits,
// titles, playground actions) to a bundled Twemoji asset. Data keeps storing plain Unicode
// strings for backward compatibility with already-saved state; only rendering changes.
val EMOJI_MAP: Map<String, EmojiName> = mapOf(
    "🐱" to EmojiName.CAT, "🐰" to EmojiName.RABBIT, "🦊" to EmojiName.FOX, "🐼" to EmojiName.PANDA,
    "🐧" to EmojiName.PENGUIN, "🐻" to EmojiName.BEAR, "🐉" to EmojiName.DRAGON, "🐶" to EmojiName.DOG,

    "🪥" to EmojiName.TOOTHBRUSH, "💧" to EmojiName.DROPLET, "🍳" to EmojiName.FRIED_EGG,
    "🥗" to EmojiName.SALAD, "📚" to EmojiName.BOOKS, "🏃" to EmojiName.RUNNER, "📖" to EmojiName.OPEN_BOOK,
    "😴" to EmojiName.SLEEPING,

    "⭐" to EmojiName.STAR, "🧣" to EmojiName.SCARF, "⚡" to EmojiName.ZAP, "👑" to EmojiName.CROWN,
    "🌌" to EmojiName.MILKY_WAY, "✨" to EmojiName.SPARKLES, "🌸" to EmojiName.CHERRY_BLOSSOM,
    "☀️" to EmojiName.SUN, "🍁" to EmojiName.MAPLE_LEAF, "❄️" to EmojiName.SNOWFLAKE,

    "🌱" to EmojiName.SEEDLING, "⚔️" to EmojiName.CROSSED_SWORDS, "🔥" to EmojiName.FIRE,
    "💪" to EmojiName.MUSCLE, "🏆" to EmojiName.TROPHY, "🎯" to EmojiName.DART, "⚙️" to EmojiName.GEAR,
    "🔮" to EmojiName.CRYSTAL_BALL, "🐦" to EmojiName.BIRD, "🦉" to EmojiName.OWL, "💀" to EmojiName.SKULL,

    "🍖" to EmojiName.MEAT, "💗" to EmojiName.GROWING_HEART, "🎾" to EmojiName.TENNIS,
    "🫂" to EmojiName.HUG_FACES, "🎵" to EmojiName.MUSICAL_NOTE, "🪶" to EmojiName.FEATHER,

    "😋" to EmojiName.YUM, "🥰" to EmojiName.SMILING_HEARTS, "🤤" to EmojiName.DROOLING,
    "🥹" to EmojiName.HOLDING_TEARS, "🎤" to EmojiName.MICROPHONE, "🤣" to EmojiName.ROFL,

    "💚" to EmojiName.GREEN_HEART, "💛" to EmojiName.YELLOW_HEART, "🧡" to EmojiName.ORANGE_HEART,
    "❤️" to EmojiName.RED_HEART, "🐾" to EmojiName.PAW_PRINTS, "⏰" to EmojiName.ALARM_CLOCK,

    "✅" to EmojiName.CHECK_BUTTON, "🌍" to EmojiName.GLOBE, "⚠️" to EmojiName.WARNING,
    "🔒" to EmojiName.LOCKED, "✓" to EmojiName.CHECK_BUTTON, "✏️" to EmojiName.PENCIL, "🔔" to EmojiName.BELL,

    "🧘" to EmojiName.LOTUS, "🧴" to EmojiName.LOTION, "🍎" to EmojiName.APPLE, "🎨" to EmojiName.PALETTE,
    "🎮" to EmojiName.VIDEO_GAME, "🎹" to EmojiName.PIANO, "🧹" to EmojiName.BROOM, "🌿" to EmojiName.HERB,
    "📓" to EmojiName.NOTEBOOK, "📝" to EmojiName.MEMO, "💼" to EmojiName.BRIEFCASE, "🥤" to EmojiName.CUP_STRAW,
    "🍵" to EmojiName.TEA, "🧠" to EmojiName.BRAIN, "📷" to EmojiName.CAMERA, "💊" to EmojiName.PILL,
    "📞" to EmojiName.TELEPHONE, "📅" to EmojiName.CALENDAR, "☕" to EmojiName.COFFEE,
    "🪴" to EmojiName.POTTED_PLANT, "🚿" to EmojiName.SHOWER, "🎧" to EmojiName.HEADPHONES,
    "🥳" to EmojiName.PARTY_FACE, "😄" to EmojiName.GRIN, "🤩" to EmojiName.STAR_STRUCK,
    "😎" to EmojiName.SUNGLASSES, "🎺" to EmojiName.TRUMPET, "🎸" to EmojiName.GUITAR, "🎁" to EmojiName.GIFT,

    "🐹" to EmojiName.HAMSTER, "🐸" to EmojiName.FROG, "🦁" to EmojiName.LION, "🐯" to EmojiName.TIGER,
    "🐨" to EmojiName.KOALA, "🦦" to EmojiName.OTTER, "🦄" to EmojiName.UNICORN,

    "🌊" to EmojiName.WAVE, "🍃" to EmojiName.LEAVES, "🌅" to EmojiName.SUNSET, "🍭" to EmojiName.LOLLIPOP,
    "💜" to EmojiName.PURPLE_HEART, "🌈" to EmojiName.RAINBOW, "🪐" to EmojiName.PLANET,
    "🎀" to EmojiName.RIBBON, "🦋" to EmojiName.BUTTERFLY,

    "🪙" to EmojiName.COIN, "💎" to EmojiName.GEM, "📺" to EmojiName.TV
)

/** Drop-in replacement for `Text(emoji, fontSize = X.sp)` — renders the bundled Twemoji asset
 * when known, otherwise falls back to the raw Unicode glyph (e.g. an old custom habit icon). */
@Composable
fun EmojiGlyph(raw: String, size: Dp, modifier: Modifier = Modifier) {
    val name = EMOJI_MAP[raw]
    if (name != null) {
        EmojiIcon(name, size, modifier)
    } else {
        Text(raw, fontSize = size.value.sp, modifier = modifier)
    }
}

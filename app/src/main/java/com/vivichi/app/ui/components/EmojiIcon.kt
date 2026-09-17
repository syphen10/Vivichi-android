package com.vivichi.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.vivichi.app.R

/**
 * A bundled Twemoji SVG (res/raw), rendered via Coil's SVG decoder. Used instead of raw Unicode
 * emoji glyphs so the art looks the same, polished, and consistent on every device — the OS's
 * own emoji font varies a lot by manufacturer/Android version and can look dated or generic.
 *
 * Twemoji graphics by Twitter, licensed CC-BY 4.0 (https://github.com/twitter/twemoji).
 */
@Composable
fun EmojiIcon(name: EmojiName, size: Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = name.resId,
        contentDescription = null,
        modifier = modifier.size(size)
    )
}

/**
 * "Satisfied" variant of each pet (eyes closed in a happy curve, with blush), generated from the
 * Twemoji art by painting over the eyes. Used in the Playground whenever the pet reacts.
 */
fun happyPetRes(species: String): Int? = when (species) {
    "cat" -> R.raw.emo_cat_happy
    "dog" -> R.raw.emo_dog_happy
    "bunny" -> R.raw.emo_rabbit_happy
    "fox" -> R.raw.emo_fox_happy
    "panda" -> R.raw.emo_panda_happy
    "peng" -> R.raw.emo_penguin_happy
    "bear" -> R.raw.emo_bear_happy
    "dragon" -> R.raw.emo_dragon_happy
    "hamster" -> R.raw.emo_hamster_happy
    "frog" -> R.raw.emo_frog_happy
    "lion" -> R.raw.emo_lion_happy
    "tiger" -> R.raw.emo_tiger_happy
    "koala" -> R.raw.emo_koala_happy
    "owl" -> R.raw.emo_owl_happy
    "otter" -> R.raw.emo_otter_happy
    "unicorn" -> R.raw.emo_unicorn_happy
    else -> null
}

enum class EmojiName(val resId: Int) {
    CAT(R.raw.emo_cat), RABBIT(R.raw.emo_rabbit), FOX(R.raw.emo_fox), PANDA(R.raw.emo_panda),
    PENGUIN(R.raw.emo_penguin), BEAR(R.raw.emo_bear), DRAGON(R.raw.emo_dragon), DOG(R.raw.emo_dog),

    TOOTHBRUSH(R.raw.emo_toothbrush), DROPLET(R.raw.emo_droplet), FRIED_EGG(R.raw.emo_fried_egg),
    SALAD(R.raw.emo_salad), BOOKS(R.raw.emo_books), RUNNER(R.raw.emo_runner), OPEN_BOOK(R.raw.emo_open_book),
    SLEEPING(R.raw.emo_sleeping),

    STAR(R.raw.emo_star), SCARF(R.raw.emo_scarf), ZAP(R.raw.emo_zap), CROWN(R.raw.emo_crown),
    MILKY_WAY(R.raw.emo_milky_way), SPARKLES(R.raw.emo_sparkles), CHERRY_BLOSSOM(R.raw.emo_cherry_blossom),
    SUN(R.raw.emo_sun), MAPLE_LEAF(R.raw.emo_maple_leaf), SNOWFLAKE(R.raw.emo_snowflake),

    SEEDLING(R.raw.emo_seedling), CROSSED_SWORDS(R.raw.emo_crossed_swords), FIRE(R.raw.emo_fire),
    MUSCLE(R.raw.emo_muscle), TROPHY(R.raw.emo_trophy), DART(R.raw.emo_dart), GEAR(R.raw.emo_gear),
    CRYSTAL_BALL(R.raw.emo_crystal_ball), BIRD(R.raw.emo_bird), OWL(R.raw.emo_owl), SKULL(R.raw.emo_skull),

    MEAT(R.raw.emo_meat), GROWING_HEART(R.raw.emo_growing_heart), TENNIS(R.raw.emo_tennis),
    HUG_FACES(R.raw.emo_hug_faces), MUSICAL_NOTE(R.raw.emo_musical_note), FEATHER(R.raw.emo_feather),

    YUM(R.raw.emo_yum), SMILING_HEARTS(R.raw.emo_smiling_hearts), DROOLING(R.raw.emo_drooling),
    HOLDING_TEARS(R.raw.emo_holding_tears), MICROPHONE(R.raw.emo_microphone), ROFL(R.raw.emo_rofl),

    GREEN_HEART(R.raw.emo_green_heart), YELLOW_HEART(R.raw.emo_yellow_heart), ORANGE_HEART(R.raw.emo_orange_heart),
    RED_HEART(R.raw.emo_red_heart), PAW_PRINTS(R.raw.emo_paw_prints), ALARM_CLOCK(R.raw.emo_alarm_clock),

    CHECK_BUTTON(R.raw.emo_check_button), GLOBE(R.raw.emo_globe), WARNING(R.raw.emo_warning),
    LOCKED(R.raw.emo_locked), PENCIL(R.raw.emo_pencil), BELL(R.raw.emo_bell),

    LOTUS(R.raw.emo_lotus), LOTION(R.raw.emo_lotion), APPLE(R.raw.emo_apple), PALETTE(R.raw.emo_palette),
    VIDEO_GAME(R.raw.emo_video_game), PIANO(R.raw.emo_piano), BROOM(R.raw.emo_broom), HERB(R.raw.emo_herb),
    NOTEBOOK(R.raw.emo_notebook), MEMO(R.raw.emo_memo), BRIEFCASE(R.raw.emo_briefcase), CUP_STRAW(R.raw.emo_cup_straw),
    TEA(R.raw.emo_tea), BRAIN(R.raw.emo_brain), CAMERA(R.raw.emo_camera), PILL(R.raw.emo_pill),
    TELEPHONE(R.raw.emo_telephone), CALENDAR(R.raw.emo_calendar), COFFEE(R.raw.emo_coffee),
    POTTED_PLANT(R.raw.emo_potted_plant), SHOWER(R.raw.emo_shower), HEADPHONES(R.raw.emo_headphones),
    PARTY_FACE(R.raw.emo_party_face), GRIN(R.raw.emo_grin), STAR_STRUCK(R.raw.emo_star_struck),
    SUNGLASSES(R.raw.emo_sunglasses), TRUMPET(R.raw.emo_trumpet), GUITAR(R.raw.emo_guitar), GIFT(R.raw.emo_gift),

    HAMSTER(R.raw.emo_hamster), FROG(R.raw.emo_frog), LION(R.raw.emo_lion), TIGER(R.raw.emo_tiger),
    KOALA(R.raw.emo_koala), OTTER(R.raw.emo_otter), UNICORN(R.raw.emo_unicorn),

    WAVE(R.raw.emo_wave), LEAVES(R.raw.emo_leaves), SUNSET(R.raw.emo_sunset), LOLLIPOP(R.raw.emo_lollipop),
    PURPLE_HEART(R.raw.emo_purple_heart), RAINBOW(R.raw.emo_rainbow), PLANET(R.raw.emo_planet),
    RIBBON(R.raw.emo_ribbon), BUTTERFLY(R.raw.emo_butterfly),

    COIN(R.raw.emo_coin), GEM(R.raw.emo_gem), TV(R.raw.emo_tv)
}

package com.evyr.rads.ui.theme

import androidx.compose.ui.graphics.Color

// --- Warm/organic palette ---
// Replaces the amber-terminal/device-panel look. Light values are the real
// hex codes from the original Gut Check PWA (gut-check-v3-1.html's :root
// CSS vars), not approximated from screenshots. Dark is an original
// companion palette in the same family -- the PWA stubbed a dark-mode
// toggle but never actually defined dark colors.

// Light (source: gut-check-v3-1.html :root)
val CreamBg = Color(0xFFF5EAD8)          // --bg
val CreamCard = Color(0xFFFDF3E7)        // --surface
val CreamCard2 = Color(0xFFF0E2CA)       // --surface2
val CreamCardBorder = Color(0xFFD9C4A8)  // --border
val ClayText = Color(0xFF2D1F10)         // --text
val ClayTextSoft = Color(0xFF6B4F35)     // --text-soft
val ClayTextDim = Color(0xFF9A7A5A)      // --muted
val Terracotta = Color(0xFFD4704A)       // --accent
val TerracottaSoft = Color(0x24D4704A)   // --accent-soft
val WarnAmber = Color(0xFFD4932A)        // --warn
val WarnAmberSoft = Color(0x21D4932A)    // --warn-soft
val WarnRust = Color(0xFFD95F5F)         // --danger
val WarnRustSoft = Color(0x21D95F5F)     // --danger-soft
val SageAccent = Color(0xFF6AB87A)       // --green
val SageAccentSoft = Color(0x216AB87A)   // --green-soft
val InfoBlue = Color(0xFF7AB8C8)         // --blue

// Dark (original companion, same family)
val EarthBg = Color(0xFF1F1912)
val EarthCard = Color(0xFF2A2118)
val EarthCard2 = Color(0xFF332818)
val EarthCardBorder = Color(0xFF3C3122)
val SandText = Color(0xFFEDE2D3)
val SandTextSoft = Color(0xFFC4AE94)
val SandTextDim = Color(0xFFA8987F)
val TerracottaBright = Color(0xFFE08A47)
val TerracottaBrightSoft = Color(0x24E08A47)
val WarnAmberBright = Color(0xFFE0AA47)
val WarnAmberBrightSoft = Color(0x21E0AA47)
val WarnRustBright = Color(0xFFD96B4C)
val WarnRustBrightSoft = Color(0x21D96B4C)
val SageAccentBright = Color(0xFF8FB07F)
val SageAccentBrightSoft = Color(0x218FB07F)
val InfoBlueBright = Color(0xFF8FC8D6)

// --- Per-meal accent tints ---
// One color family per meal slot so the accordion cards on the log screen
// read as visually distinct at a glance, rather than four identical cards.
// Light values: soft pastel card bg + a slightly stronger border + a matching
// icon-chip bg, all pulled toward the base warm/organic palette rather than
// arbitrary hues. Dark: same families, darkened for the dark surface.

// Breakfast — warm amber
val MealBreakfastBg = Color(0xFFFDEEE0)
val MealBreakfastBorder = Color(0xFFF0D4B8)
val MealBreakfastChip = Color(0xFFFBD9AE)
val MealBreakfastText = Color(0xFFA5732E)
val MealBreakfastBgDark = Color(0xFF2E2416)
val MealBreakfastBorderDark = Color(0xFF4A3A22)
val MealBreakfastChipDark = Color(0xFF5C4626)
val MealBreakfastTextDark = Color(0xFFE0B370)

// Lunch — sage green
val MealLunchBg = Color(0xFFEAF3E0)
val MealLunchBorder = Color(0xFFD3E4C0)
val MealLunchChip = Color(0xFFD3E4C0)
val MealLunchText = Color(0xFF6B8A50)
val MealLunchBgDark = Color(0xFF212A18)
val MealLunchBorderDark = Color(0xFF35472A)
val MealLunchChipDark = Color(0xFF3F5230)
val MealLunchTextDark = Color(0xFFA9CC8E)

// Dinner — dusty terracotta
val MealDinnerBg = Color(0xFFFBE5DD)
val MealDinnerBorder = Color(0xFFF0C9BA)
val MealDinnerChip = Color(0xFFF5D4C4)
val MealDinnerText = Color(0xFFB5674A)
val MealDinnerBgDark = Color(0xFF2E2019)
val MealDinnerBorderDark = Color(0xFF4A3428)
val MealDinnerChipDark = Color(0xFF5C4030)
val MealDinnerTextDark = Color(0xFFE0997A)

// Snack — dusty rose
val MealSnackBg = Color(0xFFF5E3EA)
val MealSnackBorder = Color(0xFFE8C7D6)
val MealSnackChip = Color(0xFFEBCEDA)
val MealSnackText = Color(0xFFA5567D)
val MealSnackBgDark = Color(0xFF2A2027)
val MealSnackBorderDark = Color(0xFF443040)
val MealSnackChipDark = Color(0xFF553B4F)
val MealSnackTextDark = Color(0xFFDB94B8)




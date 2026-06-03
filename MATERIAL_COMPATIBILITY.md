# Material Design Compatibility Guide

**Project Material Version:** Material Components 1.11.0 (Material Design 2)

## ⚠️ Prohibited Attributes & Components

### Material Design 3-Only Attributes (DO NOT USE)
These attributes exist in MD3 but NOT in MD2 (Material 1.11.0):

| Attribute | Used In | Status | Notes |
|-----------|---------|--------|-------|
| `app:shapeAppearanceOverride` | MaterialButton, MaterialCardView | ❌ M3 ONLY | Use `app:cornerRadius` instead |
| `app:itemActiveIndicatorColor` | BottomNavigationView | ❌ M3 ONLY | Removed from this project |
| `app:itemStateLayerColor` | Navigation components | ❌ M3 ONLY | Not needed for M2 |
| `app:chipContentPaddingWithoutIcon` | Chip | ❌ M3 ONLY | Use standard padding |
| `app:progressIndicatorIndeterminateAnimationType` | ProgressIndicator | ❌ M3 ONLY | Not available |
| `app:rippleColor` (on newer views) | Various | ⚠️ Check | May not work as expected |

### Material Design 3-Only Components (DO NOT USE)
| Component | Status | Alternative |
|-----------|--------|-------------|
| `MaterialTimePicker` | ❌ M3 ONLY | Use TimePickerDialog |
| `MaterialDatePicker` | ❌ M3 ONLY | Use DatePickerDialog |
| `NavigationRail` | ⚠️ Limited in M2 | Use NavigationView |
| `SearchView` (material) | ❌ M3 ONLY | Use AppCompat SearchView |

## ✅ Safe Alternatives for Common M3 Needs

### Circular Buttons
❌ **Don't do:**
```xml
<com.google.android.material.button.MaterialButton
    app:shapeAppearanceOverride="@style/ShapeAppearance.Circle"
    ... />
```

✅ **Do this instead:**
```xml
<com.google.android.material.button.MaterialButton
    android:layout_width="116dp"
    android:layout_height="116dp"
    app:cornerRadius="58dp"  <!-- 50% of dimension = circle -->
    ... />
```

### Status Bar Color Indicator
❌ **Don't do:**
```xml
<com.google.android.material.bottomnavigation.BottomNavigationView
    app:itemActiveIndicatorColor="#1A00A8FF"
    ... />
```

✅ **Do this instead:**
Use `app:itemIconTint` and `app:itemTextColor` with a color selector file that changes based on selection state.

### Elevated/Flat Card Distinction
❌ **Don't do:** Use `shapeAppearance` to control elevation

✅ **Do this instead:**
```xml
<com.google.android.material.card.MaterialCardView
    app:cardElevation="0dp"
    app:strokeWidth="1dp"
    app:strokeColor="@color/divider"
    ... />
```

## 🔍 Pre-Build Validation Checklist

Before building, verify:

1. **No M3-only attributes in layouts:**
   ```bash
   grep -r "shapeAppearanceOverride\|itemActiveIndicatorColor\|itemStateLayerColor\|rippleColor" app/src/main/res/layout/
   ```

2. **No M3-only imports in Java:**
   ```bash
   grep -r "import.*material3\|import.*google.android.material.timepicker\|import.*DatePicker" app/src/main/java/
   ```

3. **Verify Material version:**
   ```bash
   cat gradle/libs.versions.toml | grep "material = "
   # Should be 1.11.0 (M2), NOT 1.12.0+ (M3)
   ```

## 📋 All Attributes Safe in M2

### MaterialButton Safe Attributes
- `app:cornerRadius` ✅
- `app:icon` ✅
- `app:iconSize` ✅
- `app:iconGravity` ✅
- `app:iconTint` ✅
- `app:strokeWidth` ✅
- `app:strokeColor` ✅
- `app:elevation` ✅
- `app:backgroundTint` ✅

### MaterialCardView Safe Attributes
- `app:cardCornerRadius` ✅
- `app:cardElevation` ✅
- `app:cardBackgroundColor` ✅
- `app:strokeWidth` ✅
- `app:strokeColor` ✅

### BottomNavigationView Safe Attributes
- `app:itemIconTint` ✅
- `app:itemTextColor` ✅
- `app:itemBackground` ✅
- (NOT `app:itemActiveIndicatorColor`)

## 🚨 Known Incompatibilities in This Project

### Fixed Issues
1. ✅ `itemActiveIndicatorColor` removed from `activity_main.xml`
2. ✅ `shapeAppearanceOverride` replaced with `cornerRadius` in `activity_login.xml` btn_biometria
3. ✅ Missing `TextAppearance.MotoReviso` style (parent required)

### Future-Proofing
If upgrading to Material 3 (1.12.0+), these will need to be reverted:
- Add back `shapeAppearanceOverride` definitions for advanced shapes
- Use `itemActiveIndicatorColor` for better visual indicators
- Migrate from custom color selectors to built-in state layer colors

## 📚 References
- [Material Components for Android - GitHub](https://github.com/material-components/material-components-android/releases)
- [Material 1.11.0 Release Notes](https://github.com/material-components/material-components-android/releases/tag/1.11.0)
- [Material Design 2 Spec](https://material.io/design/introduction/)
- [Material Design 3 Spec](https://m3.material.io/) (for comparison only)

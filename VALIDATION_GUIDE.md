# Validation Guide: Preventing Runtime Crashes

## 🎯 Common Runtime Errors & Prevention

### 1. NullPointerException on findViewById()
**Error:** `NullPointerException: Attempt to invoke virtual method on a null object reference`
- **Cause:** `findViewById(R.id.xxx)` returns `null` because the ID doesn't exist in the XML layout
- **Prevention:** Use the ID Validator script before building

### 2. ClassCastException on findViewById()
**Error:** `ClassCastException: View cannot be cast to Button`
- **Cause:** The ID exists in layout but the view type doesn't match the Java declaration
- **Prevention:** Cross-reference Java type with XML element type (e.g., `MaterialButton btnX` must find `<MaterialButton ... android:id="@+id/btn_x"`)

### 3. Missing Drawable/Color Resource
**Error:** `resource drawable/ic_xxx (aka package:drawable/ic_xxx) not found`
- **Cause:** Layout or Java references a drawable that doesn't exist
- **Prevention:** Use the Resource Validator script

---

## 🔍 ID Validator Script

### Purpose
Automatically checks that every `findViewById(R.id.xxx)` call in Java has a matching `android:id="@+id/xxx"` in the corresponding layout file.

### Usage

#### Bash/Unix (macOS/Linux)
```bash
bash scripts/validate_ids.sh
```

#### Python (any platform)
```bash
python scripts/validate_ids.py
# or python3 scripts/validate_ids.py
```

### Expected Output
**Success:**
```
✅ All IDs are correctly defined!
   Every findViewById() call has a matching android:id in the layout.
```

**Failure:**
```
❌ LoginActivity (activity_login.xml) - ID 'R.id.btn_biometria' NOT FOUND in layout
❌ ManutencoesFragment (fragment_manutencoes.xml) - ID 'R.id.btn_adicionar_manutencao' NOT FOUND in layout
```

### How It Works
1. Scans all `*.xml` layout files → extracts all `android:id="@+id/..."` definitions
2. Scans all `*.java` files → extracts all `findViewById(R.id....)` calls
3. Infers the layout file name from the Java class name (e.g., `LoginActivity` → `activity_login`)
4. Compares: if ID exists in Java but not in layout → **ERROR**

---

## ✅ Pre-Build Checklist

Before committing code that references views, run:

```bash
# 1. Validate all IDs
bash scripts/validate_ids.sh

# 2. Validate Material Design 2 compatibility
grep -r "shapeAppearanceOverride\|itemActiveIndicatorColor\|itemStateLayerColor" app/src/main/res/layout/

# 3. Validate that all referenced resources exist
# Check for missing drawables
grep -rho '@drawable/[a-z_]*' app/src/main/res/layout/ | sort -u > /tmp/used_drawables.txt
ls app/src/main/res/drawable*.xml | xargs -I {} basename {} | sed 's/\.xml//' | sort -u > /tmp/defined_drawables.txt
comm -23 /tmp/used_drawables.txt /tmp/defined_drawables.txt

# Check for missing colors
grep -rho '@color/[a-z_]*' app/src/main/res/layout/ | sort -u > /tmp/used_colors.txt
grep -rho 'name="[a-z_]*"' app/src/main/res/values/colors.xml | sed 's/name="//' | sed 's/"//' | sort -u > /tmp/defined_colors.txt
comm -23 /tmp/used_colors.txt /tmp/defined_colors.txt
```

---

## 🛠️ When Adding/Changing a Layout

### Checklist for Layout Changes
- [ ] After modifying a `.xml` layout file, run `bash scripts/validate_ids.sh`
- [ ] Verify the inferred layout name is correct (it infers from Java class name)
- [ ] If adding a new view with `android:id="@+id/xxx"`, add corresponding `findViewById()` in Java **or** remove from layout if not used
- [ ] If adding `findViewById(R.id.xxx)` in Java, add the ID to the layout first

### Checklist for New Fragment/Activity Classes
1. Create/modify the `.xml` layout file with all required IDs
2. Create/modify the `.java` file with all `findViewById()` calls
3. Ensure IDs match exactly (case-sensitive)
4. Run ID validator: `bash scripts/validate_ids.sh`
5. Commit both files together

---

## 📋 Common ID Mapping Examples

### Activities
| Java Class | Inferred Layout | Expected Root View |
|------------|-----------------|-------------------|
| `LoginActivity` | `activity_login.xml` | `FrameLayout` or `LinearLayout` |
| `MainActivity` | `activity_main.xml` | `LinearLayout` |
| `CadastroVeiculoActivity` | `activity_cadastro_veiculo.xml` | `LinearLayout` |

### Fragments
| Java Class | Inferred Layout | Expected Root View |
|------------|-----------------|-------------------|
| `VeiculosFragment` | `fragment_veiculos.xml` | `CoordinatorLayout` or `LinearLayout` |
| `ManutencoesFragment` | `fragment_manutencoes.xml` | `CoordinatorLayout` |
| `ConfiguracoesFragment` | `fragment_configuracoes.xml` | `LinearLayout` |

---

## 🚨 Preventing the NullPointerException Crash

### Root Cause Chain
```
1. Layout file fragment_manutencoes.xml doesn't define android:id="@+id/btn_adicionar_manutencao"
    ↓
2. Java ManutencoesFragment.java calls findViewById(R.id.btn_adicionar_manutencao)
    ↓
3. findViewById() returns null (ID not found)
    ↓
4. Code tries to call .setOnClickListener() on null
    ↓
5. NullPointerException → App Crash
```

### Fix Applied
✅ Added FAB to `fragment_manutencoes.xml` with `android:id="@+id/btn_adicionar_manutencao"`

### Validation Proof
Run:
```bash
bash scripts/validate_ids.sh
```

Should now show:
```
✅ All IDs are correctly defined!
```

---

## 🔗 Integration with CI/CD (Future)

When adding CI/CD pipeline, add validation step:

```yaml
# GitHub Actions example
- name: Validate view IDs
  run: bash scripts/validate_ids.sh

- name: Check Material Design 2 compatibility
  run: |
    if grep -r "shapeAppearanceOverride\|itemActiveIndicatorColor" app/src/main/res/layout/; then
      echo "❌ Found M3-only attributes"
      exit 1
    fi
```

---

## 📚 Additional Resources
- [Android findViewById() Documentation](https://developer.android.com/reference/android/view/View#findViewById(int))
- [Null Safety in Kotlin](https://kotlinlang.org/docs/null-safety.html) (Future: migrate to Kotlin)
- [View Binding (Modern Alternative)](https://developer.android.com/topic/libraries/view-binding)

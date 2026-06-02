# Firebase Authentication Debugging Guide

## Problem
Getting error: "Você precisa estar autenticado para fazer upload de imagem" when trying to save edited vehicle with image.

This means `FirebaseAuth.getInstance().getCurrentUser()` is returning `null`.

## How to Debug

### Step 1: Check User Authentication Status

Before trying to edit vehicle:
1. **Open app normally** - You should see the login screen if not authenticated
2. **Sign in** - Use your test credentials
3. **Verify you're logged in** - Dashboard should show vehicle data

If you can see Dashboard, you ARE authenticated.

### Step 2: Capture Detailed Logs

We added logging to help diagnose the issue:

**When you open EditarVeiculoActivity:**
```
D/EditarVeiculoActivity: onCreate() called
D/EditarVeiculoActivity: Veiculo ID: [vehicle-id]
D/EditarVeiculoActivity: Current user on onCreate: [user@email.com or NULL]
```

**When you click Save:**
```
D/EditarVeiculoActivity: salvarAlteracoes() called
D/EditarVeiculoActivity: imagemUri is not null, checking authentication...
D/EditarVeiculoActivity: FirebaseAuth instance obtained
D/EditarVeiculoActivity: Current user: [uid or NULL]
```

### Step 3: Reproduce and Capture Logs

1. **Rebuild app**: `Build → Make Project`
2. **Clear app data** (optional but recommended):
   - Settings → Apps → MotoReviso → Storage → Clear All Data
3. **Restart app** and sign in again
4. **Open logcat** in Android Studio:
   - View → Tool Windows → Logcat
   - Filter: `EditarVeiculoActivity` or `motoreviso`
5. **Reproduce the error**:
   - Dashboard → Tap vehicle
   - Open vehicle details
   - Click "Editar Veículo"
   - Click "Alterar Foto"
   - Select an image
   - Click "Salvar"
6. **Copy all logs** from the moment you click Salvar until error appears

## What to Look For

### If User is Authenticated (Should Work)
```
D/EditarVeiculoActivity: Current user on onCreate: user@example.com
D/EditarVeiculoActivity: Current user: abc123def456
D/EditarVeiculoActivity: User authenticated as: user@example.com
D/EditarVeiculoActivity: Starting image upload: veiculo_...
```
→ Image upload should proceed

### If User is NOT Authenticated (Current Problem)
```
D/EditarVeiculoActivity: Current user on onCreate: NULL
D/EditarVeiculoActivity: Current user: NULL
W/EditarVeiculoActivity: User is not authenticated!
```
→ This is the problem!

## Possible Causes & Solutions

### Cause 1: Session Expired
**Solution**:
1. Sign out completely
2. Close app (swipe from recent apps)
3. Restart app
4. Sign in again
5. Try editing vehicle

### Cause 2: Multiple Firebase Instances
**Solution**:
- This shouldn't happen with `FirebaseAuth.getInstance()`
- But check if you're initializing Firebase multiple times
- Firebase should auto-initialize from `google-services.json`

### Cause 3: google-services.json Not Properly Synced
**Solution**:
1. In Android Studio: `File → Sync Now`
2. Wait for gradle sync to complete
3. Rebuild project: `Build → Clean Project`
4. Then rebuild: `Build → Make Project`

### Cause 4: Firebase Project Configuration Issue
**Solution**:
1. Go to Firebase Console
2. Verify your project
3. Verify SHA-1 fingerprint matches app signing key
4. Verify app package name is correct: `br.jss.motoreviso`
5. Download latest `google-services.json`
6. Replace in `app/` directory
7. Sync and rebuild

### Cause 5: Testing Permissions or Signing Issues
**Solution** (Nuclear option - clears all caches):
1. `Build → Clean Project`
2. `File → Invalidate Caches...` → Invalidate and Restart
3. Wait for indexing to complete
4. `Build → Make Project`
5. Run app fresh

## Test Results Template

When testing, capture this information:

```
## Test Run: [Date/Time]

### Setup
- Android Version: [e.g., Android 11]
- Device: [e.g., Pixel 5, Emulator]
- App Version: [from build.gradle]

### Before Edit
- Signed in as: [email or "NOT SIGNED IN"]
- Can see Dashboard: [Yes/No]
- Can see vehicles: [Yes/No]

### Authentication Check
- onCreate user: [email or NULL]
- salvarAlteracoes user: [email or NULL]

### Result
- Image selected: [Yes/No]
- Save clicked: [Yes/No]
- Error message: [Yes/No - what?]
- Image uploaded: [Yes/No]
- Image displayed on Dashboard: [Yes/No]

### Logs
[Paste relevant logcat output]
```

## Emergency Workaround

If authentication continues to fail, you can test without image upload:

1. Edit vehicle
2. DON'T select image (leave imagemUri null)
3. Click Save
4. This should work because it skips the auth check

This tells us:
- Firestore update works ✓
- The issue is specifically with image upload + auth ✗

## Expected Behavior (After Fix)

### If Authenticated ✓
```
1. Click Salvar
2. logcat shows: "User authenticated as: user@email.com"
3. Progress bar appears
4. Image uploads to Firebase Storage
5. Success toast: "Veículo atualizado com sucesso!"
6. Dashboard shows new image
```

### If Not Authenticated ✗
```
1. Click Salvar
2. logcat shows: "User is not authenticated!"
3. Toast: "Você precisa estar autenticado para fazer upload de imagem"
4. No upload attempted
5. Need to sign in and try again
```

## Files Modified

- `EditarVeiculoActivity.java` - Added detailed logging

## Next Steps

1. **Test and capture logs**
2. **Share full logcat output** for the error scenario
3. **Include**:
   - Android version
   - Whether you can see Dashboard
   - Full error message
   - Full logcat from onCreate through error

## Commit

```
da135a9 debug(editar-veiculo): add detailed logging for authentication debugging
```

---

Once you run this test and share the logs, I can identify exactly why authentication is failing!

# Firebase Storage 404 Fix

## Problem
```
StorageException: Object does not exist at location.
Code: -13010 HttpResult: 404
The server has terminated the upload session
```

**Root Cause**: Firebase Storage bucket not configured or security rules blocking access.

## Solution Steps

### Step 1: Verify Firebase Storage is Enabled

1. Go to **[Firebase Console](https://console.firebase.google.com/)**
2. Select your project: **MotoReviso**
3. Left sidebar → **Storage**
4. You should see: `gs://motoreviso-XXXXX.appspot.com`

**If Storage doesn't exist**:
- Click "Start" or "Get Started"
- Choose location: "us-central1"
- Accept terms
- Wait for bucket creation (~1 minute)

### Step 2: Set Storage Security Rules

Storage rules are blocking uploads. Fix them:

1. In Firebase Console → **Storage** tab
2. Click **Rules** (not Firestore)
3. Replace ALL content with this:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Allow anyone to read
    match /{allPaths=**} {
      allow read: if true;
    }
    
    // Allow authenticated users to write
    match /veiculo_images/{userId}/{allPaths=**} {
      allow write: if request.auth != null && 
                      request.auth.uid == userId;
    }
    
    // Fallback: deny everything else
    match /{allPaths=**} {
      allow write: if false;
    }
  }
}
```

4. Click **Publish** button
5. Confirm the changes

### Step 3: Verify App Check (Optional)

The warning in logs:
```
W  Error getting App Check token; using placeholder token
```

This is optional but good to fix. In Firebase Console:

1. Left sidebar → **App Check**
2. Click on your Android app
3. If not configured, click **Manage**
4. Register device (following prompts)
5. Enable for Storage (toggle on)

### Step 4: Rebuild and Test

```bash
1. Android Studio → Build → Clean Project
2. Build → Make Project  
3. Run app
4. Edit vehicle → Select image → Save
```

## Expected Result

**Before Fix**: 404 error
```
E  StorageException: Object does not exist at location
E  The server has terminated the upload session
```

**After Fix**: Success ✓
```
D  Starting image upload: veiculo_FfAoAg2xkyx2lRVhbL4A_1780437333536.jpg
I  Image upload completed, URL: https://firebasestorage.googleapis.com/...
I  Veículo atualizado com sucesso!
```

## Firebase Storage Structure

After fix, uploaded images will be organized:
```
gs://motoreviso-XXXXX.appspot.com/
└─ veiculo_images/
   └─ 8CrKergIhPfWk2AP3zfHhHWv3hD2/  (your user ID)
      ├─ veiculo_FfAoAg2xkyx2lRVhbL4A_1780437333536.jpg
      ├─ veiculo_FfAoAg2xkyx2lRVhbL4A_1780437333537.jpg
      └─ ... more images
```

## Troubleshooting

### Still Getting 404?

**Option A: Check bucket name in google-services.json**
```bash
grep -i "storage_bucket" app/google-services.json
```

Should output something like:
```
"storage_bucket": "motoreviso-XXXXX.appspot.com"
```

If missing or wrong, download fresh `google-services.json` from Firebase Console.

**Option B: Clear app cache**
```bash
1. Settings → Apps → MotoReviso → Storage → Clear All Data
2. Close app completely
3. Restart app
```

**Option C: Test with Firestore first**

Make sure Firestore is working:
1. Edit vehicle WITHOUT image
2. Click Save
3. If this works, then issue is specifically Storage

### Security Rules Syntax Error?

If you see rule errors after pasting, check:
- No extra spaces or tabs
- Proper bracket closing `}`
- Quotes are straight, not curly
- No comments (remove `//" type comments)

## Code Implementation

The app is trying to upload to:
```
veiculo_images/{userId}/{filename}
```

This path is controlled by `FirebaseManager.uploadImagemVeiculo()`:

```java
StorageReference reference = storage.getReference()
    .child("veiculo_images")           // Folder
    .child(userId)                      // User subfolder
    .child(nomeArquivo);               // Image file
```

With rules configured, this path is now allowed.

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| 404 error | Storage not enabled | Enable in Firebase Console |
| 403 Permission denied | Rules wrong | Update rules (paste above) |
| Connection timeout | Network issue | Check internet, retry |
| Image too large | File > 5MB | Compress image before upload |

## Verification Checklist

- [ ] Firebase Console shows Storage bucket exists
- [ ] Storage rules published successfully
- [ ] App Check token error gone (or fixed)
- [ ] google-services.json up to date
- [ ] App rebuilt and running fresh

## Next: Test Image Upload

1. **Open app → Dashboard**
2. **Tap vehicle card**
3. **Open Edit Vehicle**
4. **Click "Alterar Foto"**
5. **Select image from gallery**
6. **Click "Salvar"**
7. **Check logs**:
   - Should see: `Starting image upload:`
   - Should see: `Image upload completed, URL:`
   - Should see: `Veículo atualizado com sucesso!`
8. **Verify on Dashboard**:
   - Image should now display on vehicle card

## Git Commits

Current fix locations:
- `FirebaseManager.java` - Storage upload logic (no changes needed)
- `EditarVeiculoActivity.java` - Auth checks and image selection (already fixed)

## Summary

✅ **Solution**: Enable Firebase Storage and update Security Rules

The app code is correct. The problem is Firebase configuration:
1. Storage bucket must exist
2. Security rules must allow authenticated uploads
3. Rules must allow the path: `veiculo_images/{userId}/{filename}`

Once configured, image uploads will work! 🎉

---

**Estimated time**: 5 minutes to fix in Firebase Console

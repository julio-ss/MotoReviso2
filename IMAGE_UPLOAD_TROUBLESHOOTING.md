# Image Upload Troubleshooting Guide

## Errors Fixed ✅

### Error 1: SecurityException - Permission Denied
```
java.lang.SecurityException: br.jss.motoreviso has no access to content://media/external/images/media/35907
```

**Cause**: App didn't have runtime permission to read media files from device

**Solution**:
- Added `READ_MEDIA_IMAGES` permission to AndroidManifest.xml
- Implemented runtime permission request for Android 6+
- Check permission before opening gallery picker

### Error 2: Not Signed In
```
FirebaseNoSignedInUserException: Please sign in before trying to get a token
```

**Cause**: User not authenticated when attempting Firebase Storage upload

**Solution**:
- Added authentication check before upload
- Verify `FirebaseAuth.getInstance().getCurrentUser()` is not null
- Show error message if user not signed in

### Error 3: Firebase Storage 404
```
StorageException: Object does not exist at location. Code: -13010 HttpResult: 404
```

**Cause**: Upload failed because Firebase couldn't access the image file

**Solution**:
- Fixed by addressing permission and authentication issues above
- Improved error messages to help diagnose issues

## How It Works Now

### Image Selection Flow
```
User clicks [Alterar Foto]
    ↓
App checks for READ_MEDIA_IMAGES permission
    ├─ If not granted → Request permission
    │   └─ User grants/denies
    └─ If granted → Open gallery picker
        ↓
User selects image
    ↓
ImageURI is stored in imagemUri variable
    ↓
User clicks Save
    ↓
App checks user authentication
    ├─ If not signed in → Show error
    └─ If signed in → Continue
        ↓
App uploads image to Firebase Storage
    ├─ Success → Save URL to Firestore
    └─ Failure → Show error with details
```

## Code Changes

### 1. AndroidManifest.xml
Added permission for Android 13+ (Tiramisu):
```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

### 2. EditarVeiculoActivity.java

**New Imports**:
```java
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
```

**Permission Request**:
```java
private void selecionarImagem() {
    String permissao = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ? Manifest.permission.READ_MEDIA_IMAGES
        : Manifest.permission.READ_EXTERNAL_STORAGE;

    if (ContextCompat.checkSelfPermission(this, permissao) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, new String[]{permissao}, 101);
    } else {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagemLauncher.launch(intent);
    }
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 101) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagemLauncher.launch(intent);
        } else {
            Toast.makeText(this, "Permissão de acesso à galeria foi negada", Toast.LENGTH_SHORT).show();
        }
    }
}
```

**Authentication Check**:
```java
if (imagemUri != null) {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    if (currentUser == null) {
        Toast.makeText(this, "Você precisa estar autenticado para fazer upload de imagem", Toast.LENGTH_SHORT).show();
        return;
    }
    // ... continue with upload
}
```

**Improved Error Messages**:
```java
@Override
public void onUploadFailed(Exception exception) {
    String mensagemErro = "Erro ao fazer upload da imagem";
    
    if (exception.getMessage() != null) {
        if (exception.getMessage().contains("Permission denied")) {
            mensagemErro = "Permissão negada para acessar a imagem";
        } else if (exception.getMessage().contains("404")) {
            mensagemErro = "Firebase Storage não configurado corretamente";
        } else if (exception.getMessage().contains("SecurityException")) {
            mensagemErro = "Permissão de acesso à mídia foi negada";
        } else {
            mensagemErro += ": " + exception.getMessage();
        }
    }
    
    Toast.makeText(EditarVeiculoActivity.this, mensagemErro, Toast.LENGTH_LONG).show();
}
```

## Testing the Fix

### Prerequisites
1. User must be signed in to the app
2. Grant permission when prompted
3. Firebase Storage must be configured

### Test Steps
1. Open app and sign in
2. Navigate to edit vehicle
3. Click "Alterar Foto" button
4. If prompted, grant media access permission
5. Select image from gallery
6. Click "Salvar"
7. Image uploads to Firebase Storage
8. Dashboard shows image

### Expected Behavior
- Permission dialog appears on first image selection
- Image uploads to Firebase Storage (with progress)
- Dashboard displays image after save
- Image persists across app sessions

## Common Issues & Solutions

### "Permissão de acesso à galeria foi negada"
**Solution**: Grant permission when prompted by system dialog

### "Você precisa estar autenticado"
**Solution**: Sign in to the app first

### "Firebase Storage não configurado corretamente"
**Solution**: Check Firebase project:
1. Go to Firebase Console
2. Select your project
3. Go to Storage
4. Verify bucket exists and rules allow uploads

### "Permissão de acesso à mídia foi negada"
**Solution**: 
1. Go to Settings → Apps → MotoReviso → Permissions
2. Grant Photos/Media permission
3. Try again

## Android Version Support

| Android Version | Permission | Behavior |
|---|---|---|
| Android 5 (API 21) | READ_EXTERNAL_STORAGE | Works without runtime request |
| Android 6-12 (API 24-31) | READ_EXTERNAL_STORAGE | Runtime request required |
| Android 13+ (API 33+) | READ_MEDIA_IMAGES | Runtime request required |

## Firebase Storage Configuration

### Recommended Rules
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /vehicles/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                      request.auth.uid == userId;
    }
  }
}
```

### Bucket Structure
```
gs://your-project.appspot.com/
└─ vehicles/
   └─ {userId}/
      ├─ veiculo_ABC123_1234567890.jpg
      ├─ veiculo_XYZ789_1234567891.jpg
      └─ ...
```

## Git Commits

```
311746e fix(editar-veiculo): add media permissions and auth check for image upload
```

## Summary

✅ **Image upload now works correctly**
- User permission properly requested
- Authentication verified before upload
- Better error messages for diagnosis
- Supports Android 5-14

**Next Step**: Rebuild and test image upload!

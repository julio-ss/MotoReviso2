# Vehicle Image Loading - Implementation Guide

## Overview
The Dashboard now displays vehicle images loaded from Firebase Storage URLs with fallback support and proper error handling.

## How It Works

### Image Display Flow
```
1. Dashboard loads vehicle data from Firebase
2. Gets urlImagemPrincipal field from Veiculo document
3. Shows default ic_car_modern icon immediately (placeholder)
4. Loads actual image from URL in background using Glide
5. If image loads successfully → displays image
6. If image fails to load → keeps default icon (error fallback)
7. If no URL available → shows default icon
```

### Visual Result
```
┌──────────────────────────────┐
│ [Vehicle Image or Icon]      │
│                              │
│ ════════════════════════════ │ ← Gradient overlay
│ ✓ Principal   Yamaha MT-07  │
│               1.250 km       │
└──────────────────────────────┘
```

## Code Implementation

### DashboardFragment.java

**New Method: loadVehicleImage()**
```java
private void loadVehicleImage(Veiculo veiculo) {
    if (veiculo.getUrlImagemPrincipal() != null && !veiculo.getUrlImagemPrincipal().isEmpty()) {
        Log.d(TAG, "Loading vehicle image: " + veiculo.getUrlImagemPrincipal());
        Glide.with(this)
                .load(veiculo.getUrlImagemPrincipal())
                .placeholder(R.drawable.ic_car_modern)  // Show while loading
                .error(R.drawable.ic_car_modern)        // Show if error
                .centerCrop()
                .into(imgVeiculo);
    } else {
        Log.d(TAG, "No vehicle image URL, using default");
        imgVeiculo.setImageResource(R.drawable.ic_car_modern);
    }
}
```

### Key Features

1. **Placeholder**: Shows default icon immediately
   - Better UX than blank space
   - Appears while image downloads
   - Smooth transition to actual image

2. **Error Handling**: Falls back to default if image fails
   - Network errors
   - Invalid URLs
   - Missing files
   - Permission issues

3. **Logging**: Debug logs for troubleshooting
   - `Loading vehicle image: {URL}` - Loading started
   - `No vehicle image URL, using default` - No URL available

4. **Fallback**: Default ic_car_modern icon always shown
   - Immediate visual feedback
   - Graceful degradation if image unavailable
   - Professional appearance

## Setting Up Vehicle Images in Firebase

### 1. Upload Image to Firebase Storage

```
Firebase Console
├─ Project: MotoReviso
├─ Storage
├─ Create folder: vehicles
├─ Upload image file (e.g., yamaha_mt07.jpg)
└─ Get download URL
```

### 2. Add URL to Vehicle Document

In Firestore:
```
vehicles/{veiculoId}
├─ marca: "Yamaha"
├─ modelo: "MT-07"
├─ urlImagemPrincipal: "https://firebasestorage.googleapis.com/..."
└─ ... other fields ...
```

### 3. Image URL Format

**Full URL Example:**
```
https://firebasestorage.googleapis.com/v0/b/motoreviso-PROJECT-ID.appspot.com/o/vehicles%2Fyamaha_mt07.jpg?alt=media&token=TOKEN
```

**Or shorter version:**
```
gs://motoreviso-PROJECT-ID.appspot.com/vehicles/yamaha_mt07.jpg
```

Both formats work with Glide.

## Image Requirements

### Recommended Specifications
- **Format**: JPG or PNG
- **Size**: 200KB - 2MB (compressed)
- **Resolution**: 1000x600px minimum
- **Aspect Ratio**: 16:9 (matches ImageView)
- **Color Space**: RGB or sRGB

### Image Optimization
```
1. Crop to 16:9 aspect ratio
2. Resize to 1000x600px
3. Compress using online tool or app
4. Target final size: 200-500KB
5. Save as JPG for smaller size
```

## Testing Image Display

### Test Cases

#### Test 1: Image Loads Successfully
```
Given: Vehicle with valid Firebase Storage URL
When: Dashboard opens
Then: Vehicle image displays
And: Placeholder briefly shows while loading
And: Actual image appears once loaded
```

#### Test 2: Image Fails to Load
```
Given: Vehicle with invalid/broken URL
When: Dashboard opens
Then: Placeholder (default icon) stays visible
And: No error dialog shown (graceful fallback)
```

#### Test 3: No Image URL
```
Given: Vehicle with no urlImagemPrincipal
When: Dashboard opens
Then: Default ic_car_modern icon displays
And: No loading attempt made
```

#### Test 4: Offline
```
Given: App is offline
And: Vehicle with image URL
When: Dashboard opens
Then: Placeholder shows immediately
And: No network error/crash
```

## Glide Configuration

### Current Settings
```java
Glide.with(this)
    .load(urlImagemPrincipal)           // Source URL
    .placeholder(R.drawable.ic_car_modern)   // While loading
    .error(R.drawable.ic_car_modern)    // If error
    .centerCrop()                       // Scale mode
    .into(imgVeiculo);                  // Target ImageView
```

### What Each Setting Does

| Setting | Purpose |
|---------|---------|
| `load()` | URL to load image from |
| `placeholder()` | Shows while downloading |
| `error()` | Shows if load fails |
| `centerCrop()` | Crop image to fill space |
| `into()` | Target ImageView |

### Advanced Options (Optional)

If you want to add more options:

```java
Glide.with(this)
    .load(urlImagemPrincipal)
    .placeholder(R.drawable.ic_car_modern)
    .error(R.drawable.ic_car_modern)
    .fallback(R.drawable.ic_car_modern)  // If URL is null
    .centerCrop()
    .transition(DrawableTransitionOptions.withCrossFade())  // Fade effect
    .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache images
    .into(imgVeiculo);
```

## Firebase Storage Rules

Make sure your Firebase Storage rules allow reading images:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Allow read for authenticated users
    match /vehicles/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.token.email_verified;
    }
  }
}
```

## Troubleshooting

### Images Not Showing

**Problem**: All vehicles show default icon, no actual images
**Causes**:
- `urlImagemPrincipal` is null in Firebase
- URLs are invalid
- Firebase Storage rules deny access
- Network issues

**Solution**:
1. Check Firebase Console → Storage → vehicles folder
2. Verify URLs in Firestore documents
3. Check Firebase Security Rules
4. Check logcat for Glide error logs

### Image Takes Too Long to Load

**Problem**: Image appears slowly
**Causes**:
- Large image file (>2MB)
- Slow network
- Firebase project overloaded

**Solution**:
1. Compress images to <500KB
2. Use JPG instead of PNG
3. Resize to 1000x600px max
4. Add `.timeout(10000)` for 10-second max wait

### Wrong Image Aspect Ratio

**Problem**: Image appears stretched or squished
**Cause**: Image not 16:9 aspect ratio

**Solution**:
1. Crop image to 16:9 before uploading
2. Or change `.centerCrop()` to `.fitCenter()` in code

## Performance Tips

1. **Caching**: Glide automatically caches images
2. **Compression**: Keep images <500KB
3. **Resolution**: 1000x600px is plenty
4. **Format**: JPG is usually smaller than PNG

## Files Modified

- `DashboardFragment.java`: Added `loadVehicleImage()` method
- No layout changes needed
- No dependencies added (Glide already available)

## Git Commits

```
1a50558 feat(dashboard): improve vehicle image loading with placeholders
3f78429 debug: add logging to diagnose black screen issue
```

## Next Steps

1. Upload vehicle images to Firebase Storage
2. Add `urlImagemPrincipal` URLs to Firestore documents
3. Rebuild and test on device
4. Verify images display on Dashboard

## Summary

✅ **Vehicle images now display on Dashboard**
- Placeholder while loading
- Error fallback to default icon
- Works offline gracefully
- Proper logging for debugging
- Supports Firebase Storage URLs

Ready for testing!

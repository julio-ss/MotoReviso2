# Revision Scheduling Feature

## Overview
Users can now schedule the date for the next vehicle revision directly from the Dashboard. The scheduled date is saved to Firebase and displayed on the "Próxima Revisão" card.

## Features

### Date Picker Dialog
- Click "Agendar" button on the "Próxima Revisão" card
- DatePickerDialog opens with current date or previously scheduled date
- Default suggestion: 7 days from today
- Select desired date and confirm

### Persistent Storage
- Selected date is automatically saved to Firebase
- Stored in `Veiculo.dataProximaRevisao` field (timestamp in milliseconds)
- Persists across app sessions
- Available for all users of the vehicle

### Display Format
- Date format: `dd/MM/yyyy` (e.g., "15/06/2026")
- Shows "Data a agendar" if no date has been scheduled
- Shows "Agendada: 15/06/2026" once date is scheduled
- Updates immediately after selection

## User Flow

1. User opens Dashboard
2. Looks at "PRÓXIMA REVISÃO" card
3. Sees current status:
   - Circular progress ring (% to next revision)
   - Remaining KM
   - Current scheduled date (or "Data a agendar")
4. User clicks "Agendar" button
5. DatePickerDialog appears
6. User selects date from calendar
7. Date is saved to Firebase
8. Display updates to show scheduled date

## Technical Implementation

### Model Changes
**File**: `app/src/main/java/br/jss/motoreviso/models/Veiculo.java`

Added field:
```java
private Long dataProximaRevisao;  // Timestamp in milliseconds
```

Added methods:
```java
public Long getDataProximaRevisao() {
    return dataProximaRevisao;
}

public void setDataProximaRevisao(Long dataProximaRevisao) {
    this.dataProximaRevisao = dataProximaRevisao;
}
```

### Fragment Changes
**File**: `app/src/main/java/br/jss/motoreviso/fragments/DashboardFragment.java`

Added imports:
```java
import android.app.DatePickerDialog;
import android.widget.DatePicker;
import com.google.android.material.button.MaterialButton;
```

Added fields:
```java
private TextView textDataProximaRevisao;
private MaterialButton btnAgendarRevisao;
```

New methods:
```java
private void showDatePickerForRevision(Veiculo veiculo) {
    // Opens DatePickerDialog
    // Handles date selection
    // Saves to Firebase
    // Updates display
}
```

Updated method:
```java
private void updateRevisionCard(Veiculo veiculo) {
    // ... existing code ...
    
    // Show scheduled revision date
    if (veiculo.getDataProximaRevisao() != null && veiculo.getDataProximaRevisao() > 0) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dataFormatada = sdf.format(new Date(veiculo.getDataProximaRevisao()));
        textDataProximaRevisao.setText("Agendada: " + dataFormatada);
    } else {
        textDataProximaRevisao.setText("Data a agendar");
    }
    
    // Set button click listener
    btnAgendarRevisao.setOnClickListener(v -> {
        showDatePickerForRevision(veiculo);
    });
}
```

### Layout Changes
**File**: `app/src/main/res/layout/fragment_dashboard.xml`

Added views in "PRÓXIMA REVISÃO" card:
```xml
<TextView
    android:id="@+id/text_data_proxima_revisao"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Data a agendar"
    android:textSize="12sp"
    android:textColor="@color/text_secondary"
    android:layout_marginTop="4dp" />

<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_agendar_revisao"
    android:layout_width="wrap_content"
    android:layout_height="32dp"
    android:text="Agendar"
    android:textSize="12sp"
    android:layout_marginTop="8dp"
    app:cornerRadius="10dp"
    app:icon="@drawable/ic_calendar_modern"
    app:iconSize="16dp" />
```

### Firebase Integration
- Uses existing `atualizarVeiculo(String veiculoId, Veiculo veiculo)` method
- Automatically syncs to Firestore
- No additional database changes required
- Field is optional (null if no date scheduled)

## Data Storage

### Firebase Structure
```
vehicles/{veiculoId}
├── dataProximaRevisao: 1750396800000  (timestamp in milliseconds)
├── ... other fields ...
```

### Date Format
- Stored as: Long (milliseconds since epoch)
- Used as: `java.util.Calendar` for picker
- Displayed as: `SimpleDateFormat("dd/MM/yyyy")`

## Error Handling
- If Firebase save fails, error logged but dialog not dismissed
- User can retry by clicking "Agendar" again
- Previous selection is preserved
- No user-facing error dialog (logged in logcat)

## Testing Checklist

### Functionality
- [ ] Click "Agendar" opens DatePickerDialog
- [ ] DatePickerDialog shows correct initial date
- [ ] Can select different dates from calendar
- [ ] Selected date saves to Firebase
- [ ] Display updates immediately after selection
- [ ] Scheduled date persists after app restart

### Edge Cases
- [ ] First time scheduling (no prior date)
- [ ] Rescheduling to different date
- [ ] Scheduling to past date (should allow)
- [ ] Scheduling to far future date (2030+)
- [ ] Selecting same date again (should work)

### UI/UX
- [ ] Date format is correct (dd/MM/yyyy)
- [ ] Text color matches theme (secondary color)
- [ ] Button click is responsive
- [ ] Date picker calendar displays correctly
- [ ] No date picker appears on other card clicks

### Integration
- [ ] Works with existing Firebase connection
- [ ] Doesn't interfere with other Dashboard features
- [ ] Navigation from card still works (click anywhere but button)
- [ ] Other card buttons still functional

## Known Limitations
1. **Date Validation**: Allows scheduling to past dates
   - By design (user may want to record past maintenance)
   - Can be changed if needed

2. **Timezone**: Uses device local timezone
   - Consistent with other date handling in app
   - No timezone conversion

3. **Time Precision**: Only stores date, not time of day
   - Revision scheduling typically only needs date
   - Could be enhanced to include time if needed

## Future Enhancements
1. Add notification reminder (e.g., 3 days before scheduled date)
2. Add ability to clear scheduled date
3. Add color coding for overdue revisions
4. Export scheduled revisions to device calendar
5. Add notes field for revision details

## Compatibility
- **Android API**: 24+ (uses DatePickerDialog from android.app)
- **Material Design**: 2 (uses MaterialButton)
- **Firebase**: Uses existing integration
- **Device Timezone**: Uses system locale

## Migration Notes
- New field is optional (nullable in Firestore)
- Existing vehicles without scheduled dates work fine
- No data migration required
- Backwards compatible

## Files Modified
1. `app/src/main/java/br/jss/motoreviso/models/Veiculo.java` - Added field and methods
2. `app/src/main/java/br/jss/motoreviso/fragments/DashboardFragment.java` - Added UI and logic
3. `app/src/main/res/layout/fragment_dashboard.xml` - Added views

## Git Commit
```
854891c feat(dashboard): add revision scheduling with date picker
```

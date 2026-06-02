# Revision Scheduling - Visual Guide

## Dashboard Card Layout

### Before Scheduling (No Date Set)
```
┌─────────────────────────────────────────────────────────┐
│ PRÓXIMA REVISÃO                                         │
│                                                         │
│  ┌──────────┐              Revisão Geral              │
│  │   ██     │                                          │
│  │ ██    ██ │  35%        Data a agendar              │
│  │██        │   %                                      │
│  └──────────┘                                          │
│  1.550 km restantes        [📅 Agendar]               │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### After Scheduling (Date Set)
```
┌─────────────────────────────────────────────────────────┐
│ PRÓXIMA REVISÃO                                         │
│                                                         │
│  ┌──────────┐              Revisão Geral              │
│  │   ██     │                                          │
│  │ ██    ██ │  35%        Agendada: 15/06/2026       │
│  │██        │   %                                      │
│  └──────────┘                                          │
│  1.550 km restantes        [📅 Agendar]               │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### With Progress Ring Visualization
```
           Progress Ring (35% filled)
                    ╭─────╮
                  ╱       ╲
                 │   35%   │  ← Percentage value
                  ╲   %   ╱   ← Percentage symbol
                    ╰─────╯
                     1.550 km restantes

            Revision Info Section
            ┌──────────────────────┐
            │ Revisão Geral        │
            │                      │
            │ Agendada: 15/06/2026 │  ← Scheduled date display
            │ [📅 Agendar]         │  ← Button to reschedule
            └──────────────────────┘
```

## User Interaction Flow

### Scenario 1: First Time Scheduling

```
User sees dashboard
        ↓
Clicks [Agendar] button
        ↓
DatePickerDialog appears with calendar
        ↓
User selects date (e.g., June 15, 2026)
        ↓
Confirms selection
        ↓
Date saved to Firebase
        ↓
Display updates: "Agendada: 15/06/2026"
        ↓
User sees scheduled date on dashboard
```

### Scenario 2: Rescheduling

```
User sees scheduled date: "Agendada: 15/06/2026"
        ↓
Clicks [Agendar] button again
        ↓
DatePickerDialog appears with previously selected date (15/06/2026)
        ↓
User selects new date (e.g., July 10, 2026)
        ↓
Confirms selection
        ↓
Firebase updates with new date
        ↓
Display updates: "Agendada: 10/07/2026"
        ↓
User sees new scheduled date on dashboard
```

## DatePickerDialog Layout

### When Dialog Opens
```
┌─────────────────────────────────────┐
│  Select Revision Date               │
├─────────────────────────────────────┤
│                                     │
│         June 2026                   │
│                                     │
│  Sun Mon Tue Wed Thu Fri Sat        │
│                                     │
│   1   2   3   4   5   6   7        │
│   8   9  10  11  12  13  14        │
│  15 [16] 17  18  19  20  21        │  ← Highlighted: Today or current
│  22  23  24  25  26  27  28        │
│  29  30                             │
│                                     │
├─────────────────────────────────────┤
│  [Cancel]              [OK]         │
└─────────────────────────────────────┘
```

### User Selection
```
┌─────────────────────────────────────┐
│  Select Revision Date               │
├─────────────────────────────────────┤
│                                     │
│         June 2026                   │
│                                     │
│  Sun Mon Tue Wed Thu Fri Sat        │
│                                     │
│   1   2   3   4   5   6   7        │
│   8   9  10  11  12  13  14        │
│  15 [16] 17  18  19  20  21        │
│  22  23  24  25  26  27  28        │
│  29  30                             │
│                                     │
├─────────────────────────────────────┤
│  [Cancel]              [OK]         │
└─────────────────────────────────────┘

User selects date 22 (highlighted in dialog):

┌─────────────────────────────────────┐
│  Select Revision Date               │
├─────────────────────────────────────┤
│                                     │
│         June 2026                   │
│                                     │
│  Sun Mon Tue Wed Thu Fri Sat        │
│                                     │
│   1   2   3   4   5   6   7        │
│   8   9  10  11  12  13  14        │
│  15  16  17  18  19  20  21        │
│ [22] 23  24  25  26  27  28        │  ← Selected: 22
│  29  30                             │
│                                     │
├─────────────────────────────────────┤
│  [Cancel]              [OK]         │
└─────────────────────────────────────┘
```

## Display States

### State 1: No Date Scheduled
```
Text: "Data a agendar"
Color: Secondary (gray, #98A0A8)
Font: 12sp
Button: "Agendar" (visible)
```

### State 2: Date Scheduled
```
Text: "Agendada: 15/06/2026"
Color: Secondary (gray, #98A0A8)
Font: 12sp
Button: "Agendar" (visible for rescheduling)
```

## Color & Typography

### Text Properties
- **Field Label**: "PRÓXIMA REVISÃO"
  - Size: 11sp
  - Color: text_secondary (#98A0A8)
  - Style: Letter spacing 0.08

- **Title**: "Revisão Geral"
  - Size: 16sp
  - Style: Bold
  - Color: text_primary (#F2F5F7)

- **Scheduled Date**: "Agendada: 15/06/2026"
  - Size: 12sp
  - Color: text_secondary (#98A0A8)
  - Font: Regular (not bold)

- **Button**: "Agendar"
  - Size: 12sp
  - Icon: Calendar icon
  - Corner radius: 10dp

## Dark Theme Integration

```
Card Background:       #15171A (surface)
Text Primary:          #F2F5F7 (bright)
Text Secondary:        #98A0A8 (dimmed gray)
Button Background:     Accent color (#00A8FF) on press
Button Icon:           Calendar icon from Material Design
```

## Responsive Layout

### On Different Screen Sizes

#### Phone (400dp width)
```
[←]  1550 km         [Próx]
      Óleo Pneus      [Agendar]
    Revisão Geral
    Agendada: 15/06
```

#### Tablet (600dp width)
```
[←]  1550 km restantes           [Próxima Revisão]
      Óleo Pneus Freios Corrente
         Revisão Geral
         Agendada: 15/06/2026
         [Agendar]
```

## Accessibility

### Button
- **Label**: "Agendar" (clear action)
- **Icon**: Calendar icon (visual hint)
- **Size**: 32dp height (minimum 48dp touch target)
- **Focus**: Visible when focused via keyboard

### Text
- **Contrast**: Secondary color meets WCAG AA standard on dark background
- **Size**: 12sp + dark background = readable
- **Format**: Clear date format (dd/MM/yyyy) universally understandable

### DatePickerDialog
- **Native Control**: Uses system DatePickerDialog (accessible)
- **Navigation**: Arrow buttons for month/year navigation
- **Selection**: Tap to select, visible feedback
- **Confirmation**: Clear [Cancel] and [OK] buttons

## Example Flows

### Flow 1: Quick Schedule (2026)
```
Dashboard loaded
    ↓
See "Data a agendar"
    ↓
Tap [Agendar]
    ↓
DatePicker shows June 2026
    ↓
Tap date 15
    ↓
Tap [OK]
    ↓
Firebase saves 15/06/2026
    ↓
See "Agendada: 15/06/2026"
```

### Flow 2: Schedule Different Month
```
Dashboard loaded
    ↓
See "Data a agendar"
    ↓
Tap [Agendar]
    ↓
DatePicker shows June 2026 (current)
    ↓
Tap forward arrow to July
    ↓
Tap date 10
    ↓
Tap [OK]
    ↓
Firebase saves 10/07/2026
    ↓
See "Agendada: 10/07/2026"
```

### Flow 3: Reschedule
```
See "Agendada: 15/06/2026"
    ↓
Tap [Agendar]
    ↓
DatePicker shows June 15, 2026 (previous selection)
    ↓
Tap back arrow to May
    ↓
Tap date 20
    ↓
Tap [OK]
    ↓
Firebase updates to 20/05/2026
    ↓
See "Agendada: 20/05/2026"
```

## Material Design 2 Compliance

✅ **Components Used**:
- MaterialButton (rounded, with icon)
- TextViews (proper typography)
- DatePickerDialog (system control)
- Color palette from design system

✅ **Spacing**:
- 4dp margin between text elements
- 8dp margin above button
- 14dp padding in card
- Proper weight distribution

✅ **Typography**:
- 11sp for labels
- 12sp for secondary info
- 16sp for primary info
- Letter spacing for labels

✅ **Colors**:
- All colors from custom color palette
- Dark theme support
- Proper contrast ratios

## Testing Visual Elements

### Visual Checklist
- [ ] Date displays in correct format (dd/MM/yyyy)
- [ ] Text color matches secondary color palette
- [ ] Button shows calendar icon
- [ ] No overlapping text
- [ ] Proper spacing between elements
- [ ] DatePickerDialog appears correctly
- [ ] Calendar allows navigation
- [ ] Selected date is highlighted

### Example Test Cases

#### Test 1: Display Unscheduled
```
Given: New vehicle with no scheduled date
When: User opens Dashboard
Then: Text shows "Data a agendar"
And: Button shows "Agendar"
```

#### Test 2: Schedule Date
```
Given: User viewing Dashboard
When: User taps "Agendar"
Then: DatePickerDialog appears
And: User selects date 15/06/2026
And: User taps "OK"
Then: Display updates to "Agendada: 15/06/2026"
And: Firebase is updated
```

#### Test 3: Display Scheduled
```
Given: Vehicle with scheduled date 15/06/2026
When: User opens Dashboard
Then: Text shows "Agendada: 15/06/2026"
And: Button shows "Agendar" (for rescheduling)
```

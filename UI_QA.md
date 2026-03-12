# UI Visual QA Baseline Checklist

This document serves as the standard for manual screenshot testing and visual quality verification of HatchBase.
Always verify using the debug build (`./gradlew assembleDebug`).

## Top Screens to Check
1. **Nursery List Screen**
2. **Breeding Management Screen**
3. **Finance & Capital Screen**
4. **Hatch Planner Screen**
5. **Incubation Entry Screen**
6. **Task/Timeline Screen**
7. **Profile / Settings Screen**
8. **Add New Device Sheet/Dialog**
9. **Support Chat Screen (Hatchy Chat)**
10. **Device List Rows**

## What to Look For

### 1. Insets & Safe Drawing
- [ ] Toolbars do not overlap the status bar.
- [ ] Bottom navigation bars do not obscure list items or FABs.
- [ ] Forms / Inputs do not get covered by the `ime` (keyboard) when focused. No unexpected "violent jumps".

### 2. Typographic Density & Consistency
- [ ] No cramped line heights.
- [ ] Sticky headers smoothly transition surface/elevation without visual clipping.

### 3. Pressed State (Premium Press)
- [ ] Tapping list rows, device cards, or active chips triggers a subtle `0.98f` scale reduction and returns smoothly.
- [ ] Modals, bottom sheets, and dialogs also feature the same premium press scale for their actionable cards.
- [ ] `premiumClickable` scale should NEVER operate at the same time as an elevation animation.

### 4. Empty States
- [ ] Empty lists must use the standard `EmptyStatePanel`.
- [ ] Title, one sentence description, single action button, optional subtle icon.
- [ ] No illustrations or mascots.

### 5. Skeleton Loading Consistency
- [ ] `SkeletonCard` and `SkeletonListRow` perfectly match the layout geometry of the actual loaded components.
- [ ] Shimmer speed is slow and low contrast (1000ms translated gradient).

### 6. Scroll Friction
- [ ] Edge padding remains consistent across Breeding, Finance, and Nursery.
- [ ] Bottom padding on the last list item gracefully allows scrolling above nav bars / FABs.
- [ ] Overscroll behaves consistently without jarring background color flashes.

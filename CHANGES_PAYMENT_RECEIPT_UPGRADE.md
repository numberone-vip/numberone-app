# Payment & Receipt Experience Upgrade

## Receipt flow (customer + admin)
- `core/util/ReceiptFile.kt`: format is detected from the file's real bytes (not the picker's MIME), so HEIC/odd
  providers no longer fail with "unsupported format". Valid JPG/PNG/WEBP <= 5 MB is uploaded untouched; anything
  else decodable (HEIC, GIF, BMP, oversized screenshots) is converted to JPEG, EXIF-rotated, flattened on white and
  shrunk until it fits. Work runs off the main thread.
- `core/components/PaymentComponents.kt` (new): `rememberReceiptPicker` (system photo picker, no permission),
  `ReceiptDropZone`, `ReceiptPreviewCard` (memory-safe sampled preview, tap-to-zoom, replace/remove, explicit send),
  `ZoomableBox`/`FullScreenImageDialog`, `ReceiptViewerDialog` (admin: full-screen, pinch + double-tap zoom, loading/error
  states), `PaymentMethodCard`, `PaymentStepHeader`, `ChoicePill`, `CopyRow` (one-tap copy + haptic + check), `WarningStrip`.
- A failed upload now keeps the preview on screen so the customer can just press send again.
- `AdminScreen`: receipt dialog replaced by `ReceiptViewerDialog` (nothing else touched).

## Screens
- `WalletScreen`: method cards (Binance USDT / Sham Cash) replace tabs; guided steps; one-tap copy rows; active
  Sham Cash request card with transfer details + receipt step; request history kept.
- `CheckoutScreen`: cleaner service card (price pill, stock, wallet/transfer actions, optional balance hint).
- `StoreScreen`: pending USDT payment is now a 3-step flow (transfer details -> TXID -> receipt) using the same parts.

## Owner TODO
- `core/util/PaymentConfig.kt`: fill `SHAM_CASH_ACCOUNT` (and optional `SHAM_CASH_ACCOUNT_NAME`). Blank values hide the rows.

# 3-Tab Inner Architecture (Home)
- `HomeScreen` is now the tab shell (bottom bar forced LTR so the physical order is: الرئيسية | أرقامي | أرقام فورية API).
  Back from tab 2/3 returns to the dashboard.
- Tab 1 dashboard: wallet card + "شحن الرصيد", "رقمك النشط" card (one-tap copy, live expiry countdown, "عرض الرسائل")
  or the "+ احصل على رقم الآن" card, then action rows (الرسائل, طلباتي, اشتراكاتي, الحساب, الدعم الفني, لوحة الإدارة if admin).
- Tab 2 (أرقامي): the existing private/manual store (`StoreBody`, extracted from `StoreScreen`) with flags + duration tag.
- Tab 3: `VirtualNumbersScreen(embedded = true)` (same screen, no top bar, handles system insets via the shell).
- New: `AppViewModel.loadDashboard()`, `core/util/CountryFlag.kt`, `core/components/DashboardComponents.kt`.
- Routes (WALLET, PURCHASE, VIRTUAL_NUMBERS, ...) are unchanged and still work.

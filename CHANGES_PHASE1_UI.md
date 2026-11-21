# Phase 1 — Customer UI polish (Home, Messages, Store, Account)

- Home: avatar + greeting header, clearer balance card, 8 shortcuts reordered by importance, friendlier Arabic labels.
- Messages (SmsInboxScreen): inbox header with count, message bubble, one-tap OTP copy, error banner, helpful empty state.
- Store (StoreScreen/CheckoutScreen): wallet balance on top, service rows with icon/price/stock, stacked full-width pay buttons (no more cramped two-column buttons in Arabic), cleaner notice.
- Account (SettingsScreen, titled "حسابي"): profile card with email; language/theme kept; logout as a full-width button. `userEmail` passed from AppNavGraph.
- New shared kit: core/components/ScreenKit.kt (InitialAvatar, IconBadge).

Not changed: data layer, view models, auth, wallet/virtual-numbers/admin screens.
Not built: no Gradle run was possible offline — please sync/build once on your machine.
Note: notification preferences exist in AppPreferences but nothing reads them yet, so no toggles were added.

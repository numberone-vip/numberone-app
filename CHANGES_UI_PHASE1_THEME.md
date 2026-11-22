# UI Phase 1 - Theme unification + top notifications

1. Theme: Color.kt / Theme.kt now use the LoginScreen identity (Deep Navy #0A0F1F / #151B33, Royal Indigo #4338CA, Purple #6D28D9, accent #818CF8) for both dark and light schemes, including surfaceContainer* so the bottom NavigationBar, top bars, cards and dialogs match.
2. BalanceHeroCard: navy->indigo->purple gradient, corner light bloom, glass edge, indigo glow. BrandMark uses the same gradient. Wallet badge is frosted glass; Home "top up" button is white on indigo; bottom nav has an indigo selected indicator.
3. Notifications: new TopSnackbarHost (NumberOneComponents.kt) - floating gradient pill that slides in from the top (TopCenter, status-bar padding). VirtualNumbersScreen no longer uses the bottom Scaffold snackbarHost.

No backend / RPC / repository code was touched.

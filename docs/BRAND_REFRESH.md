# Number One visual and navigation refresh

The Android customer app palette now matches the administration web identity:

- Midnight navy: \`#091321\`
- Royal gold: \`#E7BD72\`
- Electric teal: \`#49D6B0\`
- Supporting blue: \`#5D8CFF\`

The launcher uses Android adaptive icon layers and a monochrome themed-icon layer, with a legacy vector fallback. Android 12+ uses the matching navy system splash screen.

Customer tabs are managed by Navigation Compose with saved tab back-stack state. The existing Arabic-first login, support content, customer store, wallet, orders and subscriptions remain intact.

This refresh does not alter Supabase tables, RPC signatures, customer records, payment settings, the BEP20 wallet address, or Android signing key files.

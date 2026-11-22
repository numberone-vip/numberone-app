# Phase 2 — Wallet and Admin dashboard

Wallet (WalletScreen):
- Balance card, then "اختر طريقة الشحن" with two tabs: USDT (Binance) and Sham Cash.
- USDT flow is three numbered steps: amount presets (10/25/50/100) and a custom amount; the TXID field has a paste button, a live 64-character counter and inline validation; submit stays disabled until the TXID is valid.
- Deposit requests are whole cards; tapping an open one selects it.
- Sham Cash flow: currency chips, amount, then upload the receipt from each request.
- Transactions show direction arrows, Arabic labels (شراء، استرداد، شحن رصيد…), amounts and the running balance; the list collapses to 8 items with "عرض الكل".
- Amounts and codes are forced LTR inside Arabic text.

Admin (AdminScreen):
- Overview: "يحتاج إلى مراجعتك" with shortcuts into the Sham Cash and Orders tabs, plus a green all-clear state; KPIs for users, orders and volume.
- Tab badges show open counts; review lists filter to pending by default, with an "الكل" toggle.
- Approve dialogs validate input inline instead of closing on error; order expiry has 30/90/365-day shortcuts.
- Reject dialogs require a reason and say it is shown to the customer.
- Receipt viewer has zoom hints; "awaiting customer upload" warnings replace dead buttons.

Not changed: view models, repositories, RPCs, data models, supabase, and the other customer screens.
Not built: Gradle could not run offline. Please sync and build on your machine before release.

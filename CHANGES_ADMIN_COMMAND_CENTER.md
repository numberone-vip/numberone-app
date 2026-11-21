# Admin Command Center - status

App: 5-tab AdminScreen (presentation/screens/admin/*). Users tab uses the existing admin_list_customers and
admin_list_wallet_balances (no admin_list_users). Balance adjustment sends a per-dialog request UUID.

Migration to review: supabase/migrations/20261121120000_admin_wallet_adjust.sql
 - admin_adjust_wallet_balance(p_user_id, p_amount, p_note, p_request_id) - private.is_admin() only, row lock,
   no negative balance, idempotent on request id, ledger row with previous/new balance + reason.
 - admin_list_admin_ids() - for the Admin/User chip (optional; chip hidden if absent).
Held back (supabase/optional/PENDING_REVIEW_*.sql): config/services/subscription functions.

BLOCKED until live definitions are provided: subscription assignment and access reset must call the existing
admin_add_subscription_for_user / access_code_hash workflow. The app currently still calls the unverified names
admin_assign_subscription and admin_reset_subscription_access.

Not built or tested: no Gradle, no database access here.

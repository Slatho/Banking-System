# Teller Account Administration

Teller account administration is available only to an active user with the `MANAGER` role.

## First manager sign-in

The initial Manager username and temporary password are provided in a separate Word document. The Manager must change the temporary password before the staff-administration workspace opens. Change the initial password before sharing or using the application outside a local demonstration.

## Create a teller

1. Sign in as the Manager.
2. Open **Teller accounts**.
3. Enter a unique username. Usernames must be 4–24 characters, begin with a letter, and contain only letters, numbers, dots, underscores or hyphens.
4. Enter a temporary password of at least 10 characters.
5. Select **Create teller account**.
6. Give the temporary credentials to the teller through an appropriate private channel.

At the teller's first sign-in, the application requires the teller to change the temporary password before opening the teller workspace.

## View and deactivate/reactivate tellers

The Registered tellers table displays usernames, account status and creation date. Select a teller and choose **Deactivate / reactivate selected** to toggle access. A deactivated teller cannot sign in. Reactivating the account allows future sign-ins again.

Deactivation prevents new sign-ins. An already-open session must still be closed by the teller.

## Reset a teller password

1. Select the teller in the Registered tellers table.
2. Enter a new temporary password of at least 10 characters.
3. Select **Reset selected teller password**.
4. Share the temporary password privately.

The next successful sign-in requires the teller to choose a new personal password.

## Transaction approvals

1. Open **Pending approvals** in the Teller workspace.
2. Select a pending opening deposit, deposit, withdrawal or transfer.
3. Review the source account, destination account (for transfers), amount and submitter.
4. Choose **Approve selected** or **Reject selected**.

Approving a transfer debits the source account and credits the destination account as one database transaction. Rejected requests do not change balances. If funds are no longer available when a withdrawal or transfer is reviewed, the approval is blocked and the request remains pending for a decision.

## Access control

- Managers can create, view, deactivate, reactivate and reset teller accounts.
- Only active Teller accounts can approve or reject transactions.
- Tellers cannot access the Manager workspace or teller-administration service.
- Customers can submit and track requests for their own accounts.
- Passwords are stored as salted PBKDF2 hashes, not as plain text.

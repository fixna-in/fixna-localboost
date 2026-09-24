# Local password utility

Requires Java 21 and Maven on PATH. Run in an interactive PowerShell or CMD terminal.
The utility compiles the backend and resolves its existing dependencies, but never
starts Spring, connects to PostgreSQL, or modifies users.
The Windows launcher uses a generated Java argument file under `backend/target`
to preserve long dependency classpaths. No passwords are written to that file.
Launcher regression check (no password needed):

```powershell
& 'C:\Users\Dell\workspace\fixna-localboost\tools\test-password-tool.ps1'
```



Generate a salted BCrypt hash:

```powershell
& 'C:\Users\Dell\workspace\fixna-localboost\tools\password-tool.cmd' hash
```

Verify a password against a hash:

```powershell
& 'C:\Users\Dell\workspace\fixna-localboost\tools\password-tool.cmd' verify
```

Passwords are prompted without echo and must be 8 or more characters, not blank,
and no more than 72 UTF-8 bytes. Verification supports BCrypt cost 04–16 to bound
local CPU usage. Exit codes: 0 success/match, 1 mismatch, 2 invalid input or setup.
There is no decryption: BCrypt is one-way. Forgotten passwords must be reset.

To use the printed hash with the existing **psql-only** seed script:

```powershell
$env:FIXNA_TEST_PASSWORD_HASH = Read-Host 'Paste the BCrypt hash'
try {
    psql -X -h localhost -U postgres -d localboost -f 'C:\Users\Dell\workspace\fixna-localboost\tools\sql\demo-data.sql'
} finally {
    Remove-Item Env:FIXNA_TEST_PASSWORD_HASH
}
```

The seed script preserves existing accounts; generating/loading a new hash does
not reset an existing account's password. Do not commit real password hashes or
save plaintext passwords. Mutable input buffers are cleared, but Java strings
required by BCrypt cannot be explicitly erased from memory.

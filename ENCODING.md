# Encoding, line endings, and BOMs

This repository enforces consistent text-file formatting so that diffs stay
focused on real code changes and not whitespace churn.

## The rules

| Concern              | Rule                                                         |
|----------------------|--------------------------------------------------------------|
| Line endings         | **LF** for source code and config; **CRLF** only for `.bat` / `.cmd` |
| File encoding        | **UTF-8** (no BOM) for everything                            |
| Trailing whitespace  | **Stripped** automatically by most editors via `.editorconfig` |
| Final newline        | **Required** at end of every text file                      |
| Indentation          | **4 spaces** for Java, **2 spaces** for YAML/JSON           |

These rules are enforced in three places:

1. **`.gitattributes`** — tells Git which files are binary, what line
   endings they must use on commit/checkout, and what to ignore.
2. **`.editorconfig`** — tells every modern editor (VS Code, IntelliJ,
   Eclipse, Notepad++, …) how to format files on save.
3. **Local git config** — `core.autocrlf=input` and `core.eol=lf` so
   even tools that ignore `.gitattributes` (e.g. shell utilities)
   produce LF-only files.

## Why this matters

Without these rules, this repo used to have CRLF in every `.java` file,
no BOM handling, no `.editorconfig`, and `core.autocrlf=true` set
globally on the maintainer's machine. The result was:

- Every editor insert quietly rewrote parts of files from LF to CRLF,
  producing huge "no-op" diffs that obscured real changes.
- `.properties` files written through PowerShell's `WriteAllText`
  silently gained UTF-8 BOMs, which the Java compiler rejects.
- One file in the repo (`MediaObjectDisplayPanel.java`) had its BOM
  double-encoded into the literal characters `´╗┐`, breaking the
  Maven build until the BOM was stripped.

## Tools

Two helper scripts live in `scripts/`:

- **`scripts/inspect_eol.ps1`** — read-only check; reports any file
  with non-LF line endings or a UTF-8 BOM. Use this in CI or before
  committing to make sure nothing slipped through.

- **`scripts/normalize_eol.ps1`** — one-shot rewrite. Converts every
  tracked text file to LF (or CRLF for `.bat`/`.cmd`), strips BOMs from
  `.properties` files, and skips binaries. Idempotent.

Typical one-time setup on a fresh clone:

```powershell
# 1. Pin line-ending behavior for this repo (already done in HEAD,
#    but harmless to repeat):
git config --local core.autocrlf input
git config --local core.eol      lf
git config --local core.whitespace "-trailing-space -cr-at-eol"

# 2. Normalize everything currently in the working tree:
powershell -ExecutionPolicy Bypass -File scripts\normalize_eol.ps1

# 3. Verify:
powershell -ExecutionPolicy Bypass -File scripts\inspect_eol.ps1
```

After step 2, the next commit will contain only the *real* code
changes, with line-ending churn silently absorbed by Git's
`autocrlf=input` behavior.

## If a tool keeps adding BOMs

A few Windows-side tools (notably `Out-File` and `Set-Content` in
PowerShell) emit UTF-8 with a BOM by default. To force UTF-8 without
BOM from PowerShell:

```powershell
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
```

Or, from PowerShell 5.1+:

```powershell
$PSDefaultParameterValues['Out-File:Encoding'] = 'utf8NoBOM'
```

The `scripts\normalize_eol.ps1` script will also strip any stray BOM
the next time it runs.
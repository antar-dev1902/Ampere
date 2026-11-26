# Ampere

Ampere is a console-based EV Charging Station Management and Billing System written in pure Java 17 using only the JDK standard library.

## Prerequisites
- JDK 17 or newer on `PATH` (`javac` and `java`).
- No external libraries, build tools, databases, or IDE Run-Code extensions are required.
- Always compile the full `src/` tree from the project root; compiling `AmpereApp.java` alone inside `src/ampere/app/` will fail because cross-package dependencies will not be on the source/class path.

## Compiling and Running (Windows PowerShell)

Run from the project root (`Ampere/`):

```powershell
New-Item -ItemType Directory -Path out -Force | Out-Null
$javaFiles = @(Get-ChildItem -LiteralPath src -Recurse -Filter '*.java' -File | ForEach-Object FullName)
javac -encoding UTF-8 --release 17 -Xlint:all -d out $javaFiles
java -cp out ampere.app.AmpereApp
```

Final milestone demonstration (when `ConcurrentRequestDemo` is added in the final milestone):

```powershell
java -cp out ampere.thread.ConcurrentRequestDemo
```

## Milestones
- **First Evaluation (E1 — 15 classes):** In-memory vehicle registration, 2 x 3 station grid (`A1 AC, A2 AC, A3 DC` / `B1 AC, B2 AC, B3 DC`), compatible bay assignment and waiting queue, multithreaded charging workers (`350 ms` ticks), base-rate billing, and in-memory receipts/history.
- **Final Milestone (30 classes):** Adds checked exceptions, dedicated registration validator and console input helper, station layout and energy meter extraction, peak-hour pricing, immutable station snapshots, daemon monitor, report sorting/filtering, CSV/text/binary storage, cancellation verification, and syllabus threading demonstrations.

## Package Layout
- `ampere.app` — application entry point and console menu (`AmpereApp`, `ConsoleMenu`)
- `ampere.vehicle` — vehicle model and registration registry (`Vehicle`, `VehicleRegistry`)
- `ampere.request` — validated charging requests (`ChargingRequest`)
- `ampere.charger` — charger hierarchy (`Charger`, `ACCharger`, `DCFastCharger`)
- `ampere.station` — bay grid, station coordination, and waiting queue (`ChargingBay`, `ChargingStation`, `WaitingQueue`)
- `ampere.thread` — worker task (`ChargingTask`)
- `ampere.session` — live session state and immutable receipts (`ChargingSession`, `Receipt`)
- `ampere.billing` — rate lookup and receipt generation (`BillingService`)
- `ampere.storage` *(Final milestone)* — CSV history, receipt text export, and binary backup

## E1 Menu
```text
1 Register
2 Bays
3 Request
4 Active and waiting
5 History
6 Wait until idle
0 Exit
```

## Registration & Station Rules
- **Registration Normalization:** Whitespace and ASCII hyphens (`-`) are removed and letters are uppercased with `Locale.ROOT`. The normalized registration must be 4–12 ASCII letters/digits (for example, `PB01AB1234`). Duplicates are rejected.
- **Per-Run Registrations:** Vehicle details (battery capacity and supported charger types) are stored in memory for the current run only and must be registered again after restarting the application. Completed receipts persist in the final milestone.
- **Charging Ticks & Base Rates:** Normal ticks take `350 ms`. `AC` delivers up to `1 kWh` per tick at `12 Rs/kWh`; `DC` delivers up to `3 kWh` per tick at `18 Rs/kWh` (peak `18:00–22:00` `+3 Rs/kWh` is added in the final milestone).

## Storage Paths (Final Milestone, Relative to Project Root)
- `data/sessions.csv` — primary completed session history
- `data/sessions.bin` — optional binary backup
- `data/receipts/receipt-<id>.txt` — exported text receipts

Run the application from the same project root directory each time so relative `data/` paths resolve consistently.

## End-to-End Sample Input (E1)
Start `java -cp out ampere.app.AmpereApp` and enter one line per prompt:

```text
1
Antarjot Singh
PB10AB4321
AC
40
3
PB10AB4321
AC
10
5
6
5
0
```

Expected outcome: registers `PB10AB4321`, starts an AC charging session for `5 kWh`, waits until idle, prints a completed receipt for `5 kWh` at `12 Rs/kWh` (`60 Rs`), leaves the vehicle at `15/40 kWh` (`37%` battery), and exits cleanly.

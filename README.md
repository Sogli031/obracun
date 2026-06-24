# Brzi obračun plate — jedan mesec

Prosta Android aplikacija za **brz obračun plate za jedan mesec** (npr. da izračunaš
kolegi platu na licu mesta). Otvoriš, uneseš koeficijent i staž, izabereš mesec, biraš
smene (ručno ili auto-popunom), i vidiš punu razradu plate. **Ništa se ne čuva** — kad
zatvoriš aplikaciju, stanje nestaje.

## Odnos sa „Rad" aplikacijom

Logika obračuna je **kopija** iz glavne aplikacije
[Rad](https://github.com/Sogli/rad) (paket `com.rad.shifttracker`) — isti
`PayCalculator`, `MonthlyNetPayBreakdownCalculator`, `ShiftScheduler`, `Holidays` itd.,
pa je rezultat **identičan**. Kopirani calc testovi (188) dokazuju parnost.

> Napomena: pošto je calc kopiran, buduće izmene obračuna u „Rad" treba ručno preneti i ovde.

## Šta sadrži

- Unos: koeficijent, godine staža, brigada (tro/četvoro), izbor meseca.
- Kalendar sa izborom smene po danu (kao u „Rad"), dupla smena + prekovremeni, bolovanje/godišnji.
- Auto-popuna meseca po ciklusu (trobrigada / četvorobrigada / jedna smena / prva-druga).
- „Napredno" sekcija: porez %, doprinosi %, olakšica, hrana/dan, baza bolovanja, regres,
  akontacija, kredit, sindikalna članarina — sa podrazumevanim vrednostima iz „Rad".
- Razrada: bruto, doprinosi, porez, neto pre obustava, obustave, neto plata, konačna isplata.

## Tehnologije

- Kotlin + Jetpack Compose (Material 3), bez XML layout-a.
- Bez perzistencije, bez mreže, bez dozvola — sve u memoriji.
- `applicationId` `com.rad.shifttracker.calc` (instalira se uporedo sa „Rad").
- Build: AGP 8.7.3, Kotlin 2.0.21, JDK 17, minSdk 26, targetSdk 35.

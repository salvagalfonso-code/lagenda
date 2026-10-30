# L'Agenda iOS v350 — pas acumulatiu: popups d'esdeveniments i festivals

**Font de referència:** `ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/ui/FestivalPopup.kt`, `FestivalArtistSearchResults.kt` i `AgendaApp.kt`. Captura Android aportada pel propietari (29/10/2026).

## Implementat en codi iOS (sense pujar versió)

1. Classificació estructural d'activitats normals vs festivals/cicles/programes amb prioritat als camps del servidor. La duració o un `schedule` no converteix automàticament una obra/exposició en festival. Basat en Android amb mínim de tres jornades per a AUTO.
2. Popup centrat de fins al 92% de la pantalla i altura modulada (720 dp festivals; 640/555 dp altres), header amb cartell/degradat, etiqueta FESTIVAL o ESDEVENIMENT, rang de dates real i ubicació sense truncament de línia.
3. Pestanyes unificades **Artistes · Info · Ubicació · Comunitat**: l'apartat Artistes mostra actors/grups en esdeveniment normal i programació per jornades reals en festival, igual que el referent Android.
4. Panel de programació amb dies reals, mode **Tots/Els meus**, sessions amb horari/escenari, marca d'actuació guardada, seguir artista i imatge canònica quan hi ha fitxa d'entitat.
5. En cerca exacta d'un grup (també quan s'escriu i es prem Cercar), el festival apareix com una sola targeta, es focalitza l'artista concret i la llista es desplaça fins a la fila exacta, ressaltada. No inventa dia/hora quan no es publiquen. Els apartats que no són artistes no oferixen seguir falsament.
6. La navegació des del popup d'artista i des de Guardats preserva l'artista focalitzat, fins i tot quan la informació detallada encara no és al resum del feed.
7. Càrrega remota d'horaris del servidor conservada amb missatge d'error i botó de reintent.
8. En ubicació, el festival mostra seu i escenaris amb destí real; abans d'obrir Mapes, mostra confirmació.
9. Normal: targetes d'artista sense convertir el títol de l'esdeveniment en un artista fals; ús de fotografia canònica si disponible.
10. Reavaluació de dies oberts periòdica mentre el popup continua obert.

## Verificació

- **20 casos de política de domini** superats (`/mnt/data/test_ios_popup_policy_v350.kt`).
- 60 fitxers Kotlin de l'app Android comparats amb l'original v349: **0 diferències**.
- Verificació de claus/estructures d'UI: recompte de delimitadors del bloc popup coherent.
- **No verificat:** compilació completa iOS, aparença i gestos en iPhone, connexió real amb API/servidor i compra de tiquets. La comprovació general de `AgendaIosScreen.kt` sense dependències de Compose no ha completat en l'entorn disponible.

## Diferències encara pendents respecte d'Android

- Compartir cartell com a imatge (iOS actual compartix text).
- Comunitat del popup: iOS conserva una navegació al tauló de Comunitat, no una experiència incrustada idèntica a Android.
- Horaris individuals: guardat local de iOS; encara no verificat que es compartisca la mateixa persistència remota que Android.
- Dia cultural amb canvi a les 05:00 i no simplement mitjanit.
- Disseny final: provar mesures, scrolling, safe areas i punts de toc en iPhone.
- Fluxos de taquilla interna, recordatoris locals/externs i permisos, i imatges disponibles només després de càrrega d'entitats: verificació funcional pendent.

# Pas 40/48 · Publicació com a convidat i revisió · Implementació en codi

## Referència Android v336

`ServerApiRepository.submitEventForReview()` envia `POST events/community-submit.php` amb el títol, disciplina, artistes, data/hora, ubicació, tipus de venda i `guest_id`. A partir de v308 Android indica que **primer publica** i que una revisió posterior s'incorpora al Sheets com a autoreport. El client Android tracta `status=published` com a publicació, i un `claim_token` per a convidats anònims sense Bearer. iOS té convidats reals autenticats amb `guest/create.php`; usa el seu token i ID del servidor, amb la fusió de `guest/claim.php` del pas 3. No s'ha modificat el PHP original.

## Canvis d'este pas

- `IosPublicationReviewPolicy.kt`: diferencia confirmació `published`, `pending_review`, `draft` i estat desconegut; només declara revisió confirmada quan el servidor retorna un senyal explícit (`review_queued`, `queued_for_review`, `review_status`).
- `IosAgendaApi.publisherSubmit()`: valida el formulari i identitat real, comprova suspensió del compte registrat just abans de publicar, reutilitza el mateix endpoint Android, preserva el Bearer del convidat real, no reintenta mai un POST de publicació incert i torna un comprovant tipat.
- `IosPublisherPanel.kt`: resultat de publicació visible amb ID i estat real; un convidat pot triar «Crear compte o iniciar sessió» o «Ara no» després d'enviar. La pantalla no tanca el panell automàticament. Si falla una petició amb resultat incert, bloqueja el reintent fins que l'usuari consulte les publicacions existents i autoritze manualment un possible duplicat.
- `AgendaIosScreen.kt`: una única confirmació, sense segon avís superposat. El feed es refresca només si el servidor ha confirmat `published`. No hi ha dades personals noves persistides localment.

## Verificacions reals en l'entorn

- `comprovar_pas_40.py`: **PASS**; 448 fitxers previs del ZIP 1–39 continuen byte a byte iguals; tots els fitxers Android i servidor estan intactes.
- `provar_pas_40.kt`: **PASS** (36 assercions) del contracte real de la política de revisió.
- `swiftc -frontend -parse`: **PASS** (els fitxers Swift del projecte).
- Gradle/Xcode/iPhone: **no verificat**; `services.gradle.org` no és accessible des d'este entorn i no hi ha Xcode.
- Proves de regressió 1–39: la majoria dels scripts antics depenen de ZIPs / directoris externs no adjunts; en alguns casos assumixen que el codi de passos posteriors no canviarà. No es poden declarar PASS sense reconstruir eixes referències i adaptar el caràcter acumulatiu.
- API PHP/MariaDB, vinculació real de publicacions de convidats en registre i inserció a `REVISIO_PUBLICACIONS_APP`: **no verificades en viu**. `community-submit.php` pot publicar immediatament sense tornar una prova de revisió del Sheets; iOS no promet la revisió com a confirmada quan falta eixa prova.

## Estat

**Pas 40 implementat en codi amb les limitacions descrites.** Pas següent 41/48: edició completa de publicacions, artistes, cartells i programació. No entregar ZIP abans del pas 42.

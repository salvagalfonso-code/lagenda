# Pas 42/48 — Alta fiscal professional i pas previ a la venda d'entrades (codi)

**Base acumulativa:** ZIP 13/16 (1–39) més passos 40 i 41 en la còpia de treball; Android v336 i els fitxers de servidor romanen immutables.

## Què s'ha completat

- El menú «+» permet obrir el formulari d'alta després d'una publicació pròpia i registrada. Un convidat pot publicar però ha de registrar-se/iniciar sessió per passar a venda.
- Consulta `seller-onboarding/index.php` i identifica els titulars que el servidor ja té, diferenciant els que han acceptat les condicions vigents. Permet triar entre persones/autònoms, empreses i titulars existents.
- Per a un titular nou o amb condicions caducades, obliga a obrir les URL legals HTTPS retornades pel servidor abans de marcar les acceptacions. Sense `policies_ready` i URLs legals vàlides no pot enviar l'alta. Les acceptacions vigents d'un titular ja existent es reutilitzen només quan el servidor ho declara explícitament.
- Validació fiscal i elecció d'un esdeveniment pròpiament publicat (no basta una publicació pendent ni un esdeveniment alié). Envia el POST autenticat amb ID de l'esdeveniment i dades requerides; seleccionant titular existent no duplica la seua fitxa fiscal.
- Verifica `event_id`, `company_id`, `ticket_setup_ready` i absència d'`ticket_enabled` en el comprovant; després un nou GET ha de confirmar **eixe mateix titular** i la vigència dels consentiments. Evita elevar el rol localment o proclamar venda cobrada. L'estat `Professional` real depén de les respostes i permisos del servidor.
- Serialització d'altes amb `Mutex`; si un POST té resposta incerta, no es reintenta automàticament. La interfície demana consultar l'estat al servidor abans d'enviar-ne cap altre.

## Activació efectiva de venda (modalitats preexistents)

- A més de l'alta fiscal, iOS consulta `professional-seller/options.php?event_id=...` per a veure l'estat `ticket_enabled` i les modalitats d'entrada. Quan hi ha una modalitat activa i titular fiscal amb consentiments vigents, apareix un control explícit **Activar venda**.
- En prémer-lo, iOS envia a `professional-seller/options.php` l'acció `set_enabled` amb `event_id` i `enabled`, igual que Android. Una consulta GET posterior ha de mostrar `ticket_enabled=true` abans de declarar la venda activada. També permet desactivar-la amb verificació equivalent.
- No crea ni modifica modalitats de venda en este pas: correspon a la gestió professional (pas 43). L'API decideix la validació final de permisos, empresa assignada, vigència legal i oferta.
- Una resposta incerta no provoca un segon POST automàtic.

## Proves executables

- `COMPROVACIONS/provar_pas_42.kt`: 55 assercions de `IosPublisherRoles` + `IosSellerLegalPolicy` (Kotlin/JVM aïllat).
- Regressions Kotlin/JVM pas 36, pas 40 i pas 41; veure la transcripció d'execució per a l'estat concret.
- `COMPROVACIONS/comprovar_pas_42.py`: compara tots els fitxers de la base ZIP 13/16; verifica que Android, Swift i referències PHP originals són idèntics.
- Sintaxi Swift i integritat ZIP.

## Limitacions

- La comprovació JVM no prova la compilació del projecte KMP amb Compose ni la integració en iPhone. No s'ha validat cap POST amb MariaDB real, ni els documents legals desplegats ni la configuració fiscal/TPV en producció.
- **L'alta fiscal per si sola no obri la venda.** L'activació només ocorre quan l'usuari la sol·licita i el servidor la confirma amb GET. Sense modalitats actives o compte fiscal verificat, la venda no s'activa. L'edició de modalitats i la gestió d'ordres es tractaran al pas 43.
- El PDF/legal web obert pel sistema no confirma que la persona haja llegit el document; l'app només registra l'acció explícita d'obertura i l'acceptació.
- Les URL legals es comproven a nivell de protocol/host. No es pot certificar remotament la disponibilitat o contingut d'eixos documents.

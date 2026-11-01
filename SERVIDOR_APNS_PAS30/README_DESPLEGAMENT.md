# Pas 30 — Ampliació APNs per al servidor original

Aquesta carpeta conté **fitxers de servidor preparats per desplegament**. NO s'han executat en la MariaDB en viu, ni s'han instal·lat a quimerafest.com. El projecte Android v336 i la carpeta de servidor original `SERVIDOR_APNS` es mantenen idèntics.

## Passos d'instal·lació (amb còpia de seguretat)
1. Confirmar que la base correcta és `agendapv` i que les tres taules del SQL `01_artist_bells_apns.sql` estan instal·lades. Si no, desplegar primer `SERVIDOR_APNS`.
2. Executar `sql/02_apns_inbox_delivery.sql` a la base `agendapv`.
3. Reemplaçar el fitxer corresponent del servidor per `api/notifications/apns-device.php`. Instal·lar `cron/_ios_push_prefs.php`, `cron/artist-push.php` i el nou `cron/inbox-push.php` **fora del webroot**.
4. Definir `APNS_SEND_ENABLED=1`, `APNS_TEAM_ID`, `APNS_KEY_ID`, `APNS_P8_PATH`, `APNS_TOPIC`, `APNS_JWT_CACHE_PATH` en l'entorn segur del cron. L'aplicació mai rep ni guarda la clau privada d'Apple.
5. Programar els workers cron. Fer proves amb usuari de prova, un dispositiu APNs real signat, i assegurar que els rebuts reporten HTTP 200 d'Apple (no confondre acceptació amb entrega final).

`ios_apns_inbox_delivery` només marca com a **acceptat per Apple** quan la crida HTTP/2 retorna 200. Els recordatoris explícits de l'usuari no s'ajornen per descans. Les preferències s'obtenen sempre de `agenda_user_client_state`.

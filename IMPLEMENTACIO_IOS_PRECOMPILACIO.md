# L’Agenda — integració iOS sobre v343, sense nova numeració

**Base exacta:** `LAGENDA_V343_KMP_IOS_PRECOMPILACIO_LLISTES_PERFIL.zip`.

## Incorporat en esta passada — implementat en codi, pendent de compilació conjunta

1. **Batecs:** visor real amb navegació anterior/següent i gest horitzontal, sis batecs d’artistes seguits, accés a la fitxa de l’esdeveniment i estat «vist» enviat al servidor després d’obrir la història. No crea batecs simulats.
2. **Notificacions:** les sis pestanyes mostren avisos quan el `deep_link_type` real permet identificar la categoria (per exemple TICKET o COMMUNITY). Els avisos amb tipus indeterminat romanen en General. «Pròxim» no inventa recordatoris remots; el panell separat de recordatoris mostra els actius reals. Comptadors no llegits per categoria; «marcar com llegit» afecta només la categoria visible.
3. **Cercador:** criteris de població i categoria, sense forçar el recàlcul de l'instantània de «Per a tu». La garantia comercial continua aplicant-se a les campanyes actives. No s'ha implementat un radi quilomètric fals: les coordenades i la jerarquia territorial requereixen adaptació posterior.
4. **Filtres guardats:** la pantalla permet guardar, recuperar, aplicar i eliminar cerques **per població** mitjançant `preferences/search-filters.php`. Es respecta l'autenticació Bearer del servidor; el nom i els elements de la sessió es netegen quan canvia el compte.
5. **Popup d'artista:** disciplina sobre el cartell, botó Seguir davall, i targetes dels concerts. Inclou festivals que referencien l'artista només en `schedule`. No s'han inventat plataformes musicals ni productes que l'API actual no retorna.

## Verificacions executades

- 12 casos de proves Kotlin de categorització segura i navegació de batecs: PASS.
- `swiftc -frontend -parse` dels tres fonts Swift: PASS (només sintaxi).
- `plutil -lint` del projecte Xcode i d'Info.plist: PASS.
- Comprovació lèxica d'equilibri de delimitadors del fitxer Kotlin modificat: PASS (no és compilació Kotlin amb Compose).
- 91/91 fitxers d'`app/src` d'Android comparats amb el ZIP de base, idèntics.
- Contracte `preferences/search-filters.php` local: GET `presets`, POST `{action:save/delete,name,filters}`, Bearer.
- ZIP: prova d'integritat CRC i presència dels mòduls.

## No es pot declarar complet sense altra infraestructura o prova

- **Compilació d'iPhone:** cal un Mac amb Xcode/SDK iOS i resoldre errors reals Kotlin/Native/Swift; sense això, cap flux està verificat en dispositiu.
- **Compra de pagament (Redsys/Stripe):** el backend de tiquets de la còpia proporcionada rebutja imports >0 per `tickets/purchase.php`. Cal un checkout segur amb confirmació de servidor; no es poden emetre entrades de pagament sense cobrament verificat.
- **Push APNs:** cal registre del dispositiu, claus d'Apple, subscripcions i un servei d'enviament al servidor. Els avisos locals no ho substituïxen.
- **Eliminació del compte:** no hi ha endpoint d'autosupressió d'usuari en la còpia del servidor. No és segur reutilitzar l'endpoint d'esborrat del superadministrador.
- **Google / Sign in with Apple:** falten la configuració d'identificadors i credencials, els callbacks i l'autenticació de servidor compatible amb Apple.
- **Paritat exacta de «Per a tu» i del territori:** falta migrar senyals d'afinitat, comarques, radis, descobriment graduable i la jerarquia de l'algorisme d'Android.
- **L’AgendaPRO, moderació completa i restants popups:** el codi iOS encara no replica totes les pantalles/operacions Android.
- **Accessibilitat, captures, política de privacitat i TestFlight:** pendents de validació real.

**No s'ha modificat el servidor de producció ni executat una transacció real de compra.** No és correcte etiquetar el projecte com «conversió total» fins que estos blocs estiguen implementats i verificats.

## Com provar

En Mac, descomprimir el ZIP, obrir la carpeta `ANDROID_STUDIO` a Terminal i executar:

```sh
sh COMPROVACIONS/compilar_ios_mac.sh
```

També es pot obrir `iosApp/AgendaIOS.xcodeproj` amb Xcode. Per a Android, obrir la mateixa carpeta amb Android Studio.

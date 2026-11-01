<?php
/** iOS push preferences are always read from the same agenda_user_client_state as the app. */
declare(strict_types=1);
function ios_apns_preferences(PDO $pdo, int $userId): array {
    $q=$pdo->prepare("SELECT state_key,value_json FROM agenda_user_client_state WHERE user_id=?
        AND state_key IN ('ios_push_enabled','ios_push_quiet_enabled','ios_push_quiet_start',
          'ios_push_quiet_end','alert_community','alert_followed','alert_day_before',
          'alert_tickets','alert_suggestions','alert_changes')");
    $q->execute([$userId]);
    $out=[];
    foreach($q as $row) {
        $v=json_decode((string)$row['value_json'], true);
        $out[$row['state_key']]=is_array($v)?($v['v']??null):null;
    }
    return $out;
}
function ios_apns_allowed(array $prefs, string $kind): bool {
    if (($prefs['ios_push_enabled']??false)!==true) return false;
    $flags=['ARTIST'=>'alert_followed','EVENT'=>'alert_day_before',
        'COMMUNITY'=>'alert_community','TICKET'=>'alert_tickets','TICKETS'=>'alert_tickets',
        'SUGGESTION'=>'alert_suggestions','NEW_ARTIST_EVENT'=>'alert_followed'];
    $key=$flags[strtoupper($kind)]??null;
    return $key===null || ($prefs[$key]??true)===true;
}
/** Return end of the user's quiet interval in UTC; explicit custom reminders bypass it. */
function ios_apns_quiet_end(array $prefs, bool $custom=false, ?DateTimeImmutable $utcNow=null): ?DateTimeImmutable {
    if ($custom || ($prefs['ios_push_quiet_enabled']??false)!==true) return null;
    $start=(int)($prefs['ios_push_quiet_start']??1380);
    $end=(int)($prefs['ios_push_quiet_end']??480);
    if ($start<0 || $start>1439 || $end<0 || $end>1439 || $start===$end) return null;
    // Android has its own timezone setting; current iOS contract is Europe/Madrid.
    $tz=new DateTimeZone('Europe/Madrid');
    $now=($utcNow??new DateTimeImmutable('now',new DateTimeZone('UTC')))->setTimezone($tz);
    $minute=(int)$now->format('G')*60+(int)$now->format('i');
    $inside=$start<$end ? ($minute >= $start && $minute < $end) :
        ($minute >= $start || $minute < $end);
    if (!$inside) return null;
    $day=$now->format('Y-m-d');
    if ($start>$end && $minute >= $start) $day=$now->modify('+1 day')->format('Y-m-d');
    return (new DateTimeImmutable(sprintf('%s %02d:%02d:00',$day,intdiv($end,60),$end%60),$tz))
        ->setTimezone(new DateTimeZone('UTC'));
}

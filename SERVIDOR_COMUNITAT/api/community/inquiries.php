<?php
declare(strict_types=1);
require_once __DIR__ . '/../_community_auth.php';
$pdo = db();
$pdo->exec('SET NAMES utf8mb4');
$account=agenda_current_user($pdo);
$uid=(int)$account['id'];
$method=$_SERVER['REQUEST_METHOD'];
$body=$method==='POST' ? agenda_json_body() : [];
if ($method==='GET' && (string)($_GET['inbox'] ?? '')==='1') {
    try {
        $q=$pdo->prepare("SELECT i.id,i.group_id,g.event_id,g.title board_title,i.author_user_id,
                COALESCE(u.username,'Usuari') author,i.body,i.created_at
            FROM community_board_inquiries i JOIN community_groups g ON g.id=i.group_id
            JOIN users u ON u.id=i.author_user_id
            WHERE (g.creator_user_id=? OR i.peer_user_id=?)
            ORDER BY i.id DESC LIMIT 100");
        $q->execute([$uid,$uid]);
        $messages=[];
        foreach (array_reverse($q->fetchAll(PDO::FETCH_ASSOC)) as $m) {
            $messages[]=['id'=>(int)$m['id'],'group_id'=>(int)$m['group_id'],'event_id'=>(int)$m['event_id'],
                'board_title'=>$m['board_title'],'author'=>$m['author'],'author_user_id'=>(int)$m['author_user_id'],
                'body'=>$m['body'],'created_at'=>$m['created_at']];
        }
        agenda_json_response(200,['ok'=>true,'messages'=>$messages]);
    } catch (Throwable $e) {
        error_log('[Agenda board inbox] '.$e->getMessage());
        agenda_json_response(500,['ok'=>false,'message'=>'No s\'ha pogut consultar la bústia.']);
    }
}
$groupId=filter_var($method==='GET' ? ($_GET['group_id'] ?? null) : ($body['group_id'] ?? null),
    FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
if (!$groupId) agenda_json_response(422,['ok'=>false,'message'=>'Tauló invàlid.']);
try {
    $q=$pdo->prepare("SELECT g.creator_user_id,g.status,e.status event_status
        FROM community_groups g JOIN events e ON e.id=g.event_id WHERE g.id=?");
    $q->execute([(int)$groupId]);
    $group=$q->fetch(PDO::FETCH_ASSOC);
    if (!$group || $group['event_status']!=='published') agenda_json_response(404,['ok'=>false,'message'=>'Tauló no disponible.']);
    $owner=(int)$group['creator_user_id'];
    $isOwner=$owner===$uid;
    if ($method==='GET') {
        $sql="SELECT i.id,i.group_id,i.peer_user_id,i.author_user_id,i.body,i.created_at,
                COALESCE(p.username,'Usuari') peer_name,COALESCE(u.username,'Usuari') author
            FROM community_board_inquiries i JOIN users p ON p.id=i.peer_user_id
                JOIN users u ON u.id=i.author_user_id
            WHERE i.group_id=?" . ($isOwner ? '' : ' AND i.peer_user_id=?') . ' ORDER BY i.id DESC LIMIT 200';
        $q=$pdo->prepare($sql); $q->execute($isOwner ? [(int)$groupId] : [(int)$groupId,$uid]);
        $items=array_reverse($q->fetchAll(PDO::FETCH_ASSOC));
        $messages=[];
        foreach ($items as $item) {
            $messages[]=['id'=>(int)$item['id'],'group_id'=>(int)$item['group_id'],
                'peer_user_id'=>(int)$item['peer_user_id'],'peer_name'=>$item['peer_name'],
                'author'=>$item['author'],'body'=>$item['body'],'created_at'=>$item['created_at'],
                'own'=>(int)$item['author_user_id']===$uid];
        }
        agenda_json_response(200,['ok'=>true,'messages'=>$messages]);
    }
    if ($method!=='POST') agenda_json_response(405,['ok'=>false,'message'=>'Mètode no permés.']);
    $text=agenda_text($body['body'] ?? '',2000);
    if ($text==='') agenda_json_response(422,['ok'=>false,'message'=>'Escriu un missatge.']);
    if (!$isOwner && in_array($group['status'],['CLOSED','CANCELLED','ARCHIVED'],true)) {
        agenda_json_response(409,['ok'=>false,'message'=>'El tauló està tancat.']);
    }
    if ($isOwner) {
        $peer=filter_var($body['peer_user_id'] ?? null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
        if (!$peer || (int)$peer===$uid) agenda_json_response(422,['ok'=>false,'message'=>'Tria una persona per respondre.']);
        // El creador només pot respondre a una persona que haja iniciat una consulta real.
        $q=$pdo->prepare('SELECT 1 FROM community_board_inquiries WHERE group_id=? AND peer_user_id=? LIMIT 1');
        $q->execute([(int)$groupId,(int)$peer]);
        if (!$q->fetchColumn()) agenda_json_response(403,['ok'=>false,'message'=>'No hi ha cap consulta d\'esta persona.']);
    } else {
        $peer=$uid;
    }
    // També bloquegem contactes privats entre persones bloquejades.
    $b=$pdo->prepare('SELECT 1 FROM community_user_blocks WHERE
        (blocker_user_id=? AND blocked_user_id=?) OR (blocker_user_id=? AND blocked_user_id=?) LIMIT 1');
    $b->execute([$owner,(int)$peer,(int)$peer,$owner]);
    if ($b->fetchColumn()) agenda_json_response(403,['ok'=>false,'message'=>'No es pot contactar amb esta persona.']);
    $ban=$pdo->prepare('SELECT 1 FROM community_group_bans WHERE group_id=? AND user_id=?');
    $ban->execute([(int)$groupId,$uid]);
    if ($ban->fetchColumn()) agenda_json_response(403,['ok'=>false,'message'=>'No tens accés a este tauló.']);
    // Moderació bàsica d'enviaments reiterats.
    $q=$pdo->prepare('SELECT COUNT(*) FROM community_board_inquiries WHERE author_user_id=? AND created_at >= DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 MINUTE)');
    $q->execute([$uid]);
    if ((int)$q->fetchColumn()>=6) agenda_json_response(429,['ok'=>false,'message'=>'Massa missatges; intenta-ho més tard.']);
    $q=$pdo->prepare('INSERT INTO community_board_inquiries(group_id,peer_user_id,author_user_id,body,created_at) VALUES (?,?,?,?,UTC_TIMESTAMP())');
    $q->execute([(int)$groupId,(int)$peer,$uid,$text]);
    agenda_json_response(200,['ok'=>true,'message_id'=>(int)$pdo->lastInsertId()]);
} catch (Throwable $error) {
    error_log('[Agenda board inquiries] '.$error->getMessage());
    agenda_json_response(500,['ok'=>false,'message'=>'No s\'han pogut carregar o enviar les consultes.']);
}

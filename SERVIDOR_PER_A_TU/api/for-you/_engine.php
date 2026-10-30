<?php
declare(strict_types=1);
/** Motor central de Per a tu. La selecció és comuna als clients; mai usa l'estat de la UI. */
function fy_norm(string $s): string {
    $s = trim(strtolower($s));
    $s = strtr($s, ['à'=>'a','á'=>'a','ä'=>'a','è'=>'e','é'=>'e','ë'=>'e','í'=>'i','ï'=>'i','ò'=>'o','ó'=>'o','ö'=>'o','ú'=>'u','ü'=>'u','ç'=>'c','ñ'=>'n','·'=>'']);
    return trim(preg_replace('/[^a-z0-9]+/u', ' ', $s) ?? '');
}
function fy_codes(mixed $raw, array $aliases): array {
    $parts = is_array($raw) ? $raw : preg_split('/[|;,]/u', (string)$raw);
    $out=[];
    foreach ($parts ?: [] as $p) {
        if (is_array($p)) continue;
        $key=fy_norm((string)$p); if ($key==='') continue;
        $out[$aliases[$key] ?? str_replace(' ', '_',$key)]=true;
    }
    return array_keys($out);
}
function fy_values(mixed $raw, int $max=100): array {
    if (!is_array($raw)) return [];
    $out=[];foreach (array_slice($raw,0,$max) as $x) {
        if (!is_string($x) && !is_numeric($x)) continue;
        $v=trim((string)$x);if($v!=='' && strlen($v)<=760) $out[]=$v;
    }
    return array_values(array_unique($out));
}
function fy_preferences(array $posted): array {
    $radii=[];
    if (is_array($posted['radii']??null)) foreach(array_slice($posted['radii'],0,100,true) as $name=>$value) {
        if(!is_string($name)||strlen($name)>760) continue;
        $radius=(int)$value;
        if(in_array($radius,[5,15,30,50,100],true)) $radii[trim($name)]=$radius;
    }
    $level=strtoupper((string)($posted['discovery']??'BALANCED'));
    if(!in_array($level,['MY_TASTES','BALANCED','EXPLORE'],true))$level='BALANCED';
    // The server cache signature must be identical for equivalent iOS/Android preferences.
    $categories=fy_values($posted['categories']??[]);sort($categories,SORT_STRING);
    $subs=fy_values($posted['subcategories']??[]);sort($subs,SORT_STRING);
    $towns=fy_values($posted['towns']??[]);sort($towns,SORT_STRING);
    $comarques=fy_values($posted['comarques']??[]);sort($comarques,SORT_STRING);
    ksort($radii,SORT_STRING);
    return [
        'categories'=>$categories,
        'subcategories'=>$subs,
        'towns'=>$towns,
        'comarques'=>$comarques,
        'radii'=>$radii,
        'suggestions'=>($posted['suggestions']??false)===true,
        'discovery'=>$level,
        'valencian_first'=>($posted['valencian_first']??true)!==false,
    ];
}
function fy_territory(array $rows): array {
    $index=[];
    foreach ($rows as $row) {
        $names=[(string)($row['name']??''),(string)($row['officialName']??'')];
        foreach(explode('/',(string)($row['otherNames']??'')) as $alias)$names[]=trim($alias);
        foreach($names as $name){$key=fy_norm($name);if($key!==''&&!isset($index[$key]))$index[$key]=$row;}
    }
    return $index;
}
function fy_distance_km(array $a,array $b): float {
    $lat1=deg2rad((float)$a['latitude']);$lat2=deg2rad((float)$b['latitude']);
    $dlat=$lat2-$lat1;$dlon=deg2rad((float)$b['longitude']-(float)$a['longitude']);
    $h=sin($dlat/2)**2+cos($lat1)*cos($lat2)*sin($dlon/2)**2;
    return 6371.0*2*atan2(sqrt($h),sqrt(max(0.0,1.0-$h)));
}
function fy_near(array $event, array $prefs, array $geo, bool $fixed): bool {
    static $byComarca=[];
    if ($byComarca===[]) foreach($geo as $g) {
        $key=fy_norm((string)($g['comarca']??''));
        if($key!=='') $byComarca[$key][]=$g;
    }
    $town=fy_norm((string)($event['town']??''));
    $comarca=fy_norm((string)($event['comarca']??''));
    $place=$geo[$town]??null;
    foreach($prefs['towns'] as $origin) {
        $key=fy_norm($origin);
        if($key===$town) return true;
        $from=$geo[$key]??null;
        if($place && $from && fy_distance_km($from,$place)<=($fixed?100:($prefs['radii'][$origin]??30)))return true;
    }
    foreach($prefs['comarques'] as $name) {
        $key=fy_norm($name);
        if($key===$comarca) return true;
        if(!$place)continue;
        // Comarca: origen de totes les localitats de la comarca, igual que Android.
        foreach($byComarca[$key]??[] as $g) {
            if(fy_distance_km($g,$place)<=($fixed?100:30))return true;
        }
    }
    return false;
}
function fy_quota(int $core,string $level): int {
    return match($level) {
        'MY_TASTES'=>$core===0?1:max(1,min(8,intdiv($core,6))),
        'EXPLORE'=>$core===0?8:max(4,min(40,intdiv($core*2,3))),
        default=>$core===0?3:max(2,min(20,intdiv($core,3))),
    };
}
/** All metadata is built from the canonical public API and the same taxonomy/geo/style assets as Android. */
function fy_select(array $events,array $follows,array $prefs,array $assets,array $affinities=[]): array {
    $aliases=$assets['styles']['aliases']??[];
    $geo=fy_territory($assets['territory']['municipalities']??[]);
    $taxonomy=$assets['taxonomy'];
    $cats=[];$subdefs=[];
    foreach($taxonomy['categories']??[] as $c) $cats[fy_norm($c['code'])]=fy_norm($c['label']);
    foreach($taxonomy['subcategories']??[] as $s) $subdefs[$s['code']]=$s;
    $followed=['ARTIST'=>[],'VENUE'=>[],'ORGANIZER'=>[],'CATEGORY'=>[]];
    foreach($follows as $row){$type=strtoupper((string)($row['entity_type']??$row['type']??''));if(!isset($followed[$type]))continue;
        foreach(['entity_key','entity_label','key','label'] as $field){$v=fy_norm((string)($row[$field]??''));if($v!=='')$followed[$type][$v]=true;}
    }
    $preferred=[];
    foreach(array_merge($prefs['categories'],$prefs['subcategories'],array_keys($followed['CATEGORY'])) as $raw) {
        $code=fy_norm($raw);$preferred[str_replace(' ','_',$code)]=true;
        if(isset($cats[$code]))foreach($subdefs as $sub){if(fy_norm($sub['parent'])===$code)$preferred[str_replace(' ','_',fy_norm($sub['code']))]=true;}
    }
    $hasGeo=$prefs['towns']!==[]||$prefs['comarques']!==[];
    // Calculate fallback styles from the actual events for artists lacking a catalog entity record.
    $followStyles=[];
    foreach($events as $e) {
        $artists=$e['artists']??[];
        foreach($artists as $a) {
            $name=is_array($a)?($a['name']??''):$a;
            if(isset($followed['ARTIST'][fy_norm((string)$name)])) foreach(fy_codes($e['styles']??[], $aliases) as $s)$followStyles[$s]=true;
        }
    }
    $recentStyle=[];
    foreach($affinities as $row){
        if(strtoupper((string)($row['entity_type']??''))!=='STYLE')continue;
        $key=fy_codes([(string)($row['entity_key']??'')],$aliases)[0]??'';
        if($key!=='')$recentStyle[$key]=($recentStyle[$key]??0)+max(1,(int)($row['interaction_count']??1));
    }
    $core=[];$suggestions=[];$order=[];$seen=[];
    foreach($events as $e){
        $id=(int)($e['id']??0);if($id<=0||isset($seen[$id]))continue;$seen[$id]=true;
        if(($e['status']??'published')!=='published'||($e['free_internal_reservation']??false)===true)continue;
        $artistKeys=array_map(static fn($a)=>fy_norm((string)(is_array($a)?($a['name']??''):$a)), $e['artists']??[]);
        foreach($e['schedule']??[] as $s){if(isset($s['artist_name']))$artistKeys[]=fy_norm((string)$s['artist_name']);}
        $organizers=array_map(static fn($a)=>fy_norm((string)(is_array($a)?($a['name']??''):$a)), $e['organizers']??[]);
        $venue=fy_norm((string)($e['venue']??''));
        $followedEntity=count(array_intersect($artistKeys,array_keys($followed['ARTIST'])))>0 ||
            count(array_intersect($organizers,array_keys($followed['ORGANIZER'])))>0 || isset($followed['VENUE'][$venue]);
        $category=fy_norm((string)($e['category']??''));
        $codes=[];
        foreach(fy_codes($e['styles']??[],$aliases) as $s)$codes[$s]=true;
        foreach(fy_codes($e['catalog_categories']??[],$aliases) as $s)$codes[$s]=true;
        // Infantil is transversal, never treated as a competing main discipline.
        if (isset($codes['infantil']) || isset($codes['familiar']) || isset($codes['public_infantil'])) {
            $codes['children']=true;
        }
        if($category!==''){
            $codes[str_replace(' ','_',$category)]=true;
            foreach($cats as $code=>$label)if($category===$code||$category===$label)$codes[str_replace(' ','_',$code)]=true;
        }
        foreach($subdefs as $sub) {
            $parent=fy_norm((string)$sub['parent']);
            if($category!==$parent && ($cats[$parent]??'')!==$category)continue;
            $terms=array_merge([(string)$sub['code'],(string)$sub['label']],$sub['aliases']??[]);
            foreach($terms as $term){if(isset($codes[str_replace(' ','_',fy_norm((string)$term))])){$codes[str_replace(' ','_',fy_norm($sub['code']))]=true;break;}}
        }
        $relevant= count(array_intersect(array_keys($codes),array_keys($preferred)))>0;
        $matchesStyle=count(array_intersect(array_keys($codes),array_keys($followStyles)))>0;
        $preferredGeo=$hasGeo ? fy_near($e,$prefs,$geo,false) : false;
        $fixedGeo=$hasGeo ? fy_near($e,$prefs,$geo,true) : false;
        $personalized=($preferred!==[]?$relevant:$hasGeo) && (!$hasGeo || $fixedGeo);
        $fromFollowed=$matchesStyle && $preferredGeo;
        $order[$id]=$e;
        if($followedEntity||$personalized||$fromFollowed){$core[$id]=true;continue;}
        if(!$prefs['suggestions']||!$preferredGeo)continue;
        $score=0;foreach($codes as $code=>$_)$score+=($recentStyle[$code]??0);
        if($score>0)$suggestions[$id]=$score;
    }
    if($prefs['suggestions']&&$suggestions!==[]){
        // Android policy: score descending, then event id ascending.
        uksort($suggestions,static function($a,$b)use(&$suggestions){return ($suggestions[$b]<=>$suggestions[$a])?:($a<=>$b);});
        foreach(array_slice(array_keys($suggestions),0,fy_quota(count($core),$prefs['discovery'])) as $id)$core[$id]=true;
    }
    $result=array_filter($order,static fn($e)=>isset($core[(int)$e['id']]));
    uasort($result,static function($a,$b)use($prefs){
        $ad=(string)($a['date']??'9999');$bd=(string)($b['date']??'9999');if($ad!==$bd)return $ad<=>$bd;
        if($prefs['valencian_first']){
            $va=(int)(str_contains(fy_norm((string)($a['language']??'')),'valencia')||str_contains(fy_norm((string)($a['language']??'')),'catala'));
            $vb=(int)(str_contains(fy_norm((string)($b['language']??'')),'valencia')||str_contains(fy_norm((string)($b['language']??'')),'catala'));
            if($va!==$vb)return $vb<=>$va;
        }
        return ((string)($a['time']??''))<=>((string)($b['time']??'')) ?: ((int)$a['id']<=> (int)$b['id']);
    });
    return array_values(array_map('intval',array_keys($result)));
}

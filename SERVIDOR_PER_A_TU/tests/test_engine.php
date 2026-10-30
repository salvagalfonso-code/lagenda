<?php
declare(strict_types=1);
require __DIR__.'/../api/for-you/_engine.php';
$base=__DIR__.'/../api/for-you/data/';
$assets=[];
foreach(['styles'=>'style_affinity_catalog_v3.json','territory'=>'agenda_territori_v277.json','taxonomy'=>'taxonomia_android.json'] as $key=>$file)$assets[$key]=json_decode((string)file_get_contents($base.$file),true,512,JSON_THROW_ON_ERROR);
function check_case(bool $yes,string $label):void{if(!$yes){fwrite(STDERR,"FAIL: $label\n");exit(1);}echo "OK: $label\n";}
function event(int $id,string $style,string $town,string $cat='Música',array $artists=[]):array{return [
  'id'=>$id,'date'=>'2026-11-05','time'=>'20:00','status'=>'published',
  'artists'=>array_map(fn($a)=>['name'=>$a],$artists),'schedule'=>[],'organizers'=>[],
  'category'=>$cat,'styles'=>$style,'town'=>$town,'comarca'=>'Camp de Morvedre','venue'=>'Sala',
  'language'=>'Valencià'];}
$p=fy_preferences(['categories'=>['MUSIC'],'towns'=>['Sagunt'],'radii'=>['Sagunt'=>15],'suggestions'=>false]);
$events=[event(1,'rock','Sagunt'),event(2,'rock','Barcelona'),event(3,'jazz','Sagunt'),event(4,'rock','Madrid')];
$ids=fy_select($events,[],$p,$assets);
check_case($ids===[1,3],'Categoria general només dins de 100 km');
$p2=fy_preferences(['subcategories'=>['MUSIC__JAZZ_BLUES_SOUL'],'towns'=>['Sagunt'],'suggestions'=>false]);
$ids=fy_select($events,[],$p2,$assets);
check_case($ids===[3],'Subcategoria restringix correctament');
$p3=fy_preferences(['categories'=>[],'towns'=>[]]);
$ids=fy_select($events,[['entity_type'=>'ARTIST','entity_key'=>'la prova','entity_label'=>'La Prova']],$p3,$assets);
check_case($ids===[],'Sense esdeveniments seguits no inventa coincidències');
$events[3]['artists']=[['name'=>'La Prova']];
$ids=fy_select($events,[['entity_type'=>'ARTIST','entity_key'=>'la prova','entity_label'=>'La Prova']],$p3,$assets);
check_case($ids===[4],'Artista seguit és global, encara que no estiga prop');
$events[3]['free_internal_reservation']=true;
$ids=fy_select($events,[['entity_type'=>'ARTIST','entity_key'=>'la prova','entity_label'=>'La Prova']],$p3,$assets);
check_case($ids===[],'Reserva interna gratuïta exclosa');
check_case(fy_quota(18,'MY_TASTES')===3 && fy_quota(18,'BALANCED')===6 && fy_quota(18,'EXPLORE')===12,'Quotes de descoberta idèntiques a Android');
check_case(fy_preferences(['radii'=>['Sagunt'=>11,'Vila-real'=>15]])['radii']===['Vila-real'=>15],'Radis restringits als valors oficials');
$child=event(5,'infantil|titelles','Sagunt','Teatre');
$ids=fy_select([$child],[],fy_preferences(['categories'=>['CHILDREN']]),$assets);
check_case($ids===[5],'Infantil transversal a qualsevol disciplina');

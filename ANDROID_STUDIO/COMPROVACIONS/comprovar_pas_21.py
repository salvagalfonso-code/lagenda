"""Pas 21: static + original source comparison, not a full iPhone UI build."""
from pathlib import Path
import hashlib
import zipfile
root=Path(__file__).resolve().parents[1]
ui=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt').read_text()
nav=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosBatecNavigationPolicy.kt').read_text()
test=(root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosBatecNavigationPolicyTest.kt').read_text()
checks=[
  'batecViewerGroups = followedGroups.toList()',
  'batecViewerStories = batecs.toList()',
  'batecViewerOwnerId = owner',
  'batecViewerOwnerId != batecViewerUserId',
  'groups = batecViewerGroups',
  'stories = batecViewerStories',
  'LaunchedEffect(batecStripOrder)',
  'keepBatecStripAtStart = false',
  'BatecBubble(',
  'ColorFilter.colorMatrix(ColorMatrix()',
  'Color(0xFF1D9BF0)',
  'group.storyKeys.size > 1',
  'IosBatecNavigationPolicy.tapDirection',
  'IosBatecNavigationPolicy.swipeDirection',
  'IosBatecNavigationPolicy.navigate',
  '.aspectRatio(9f / 16f)',
  'onToggleFollow(group.artist)',
  'repeat(150)',
  'delay(100)',
  'returnToStartEpoch = batecsReturnEpoch',
  'if (selectedIndex > 0)',
]
for x in checks: assert x in ui,x
assert 'data object Restart' in nav and 'data object Close' in nav
assert 'halfScreenTapsAsOnAndroid' in test and 'horizontalGesturesThreshold' in test
# Verify the current Android package and the original v349 Android subtree are unchanged.
for source in ['/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip',
               '/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip']:
    verified=0
    with zipfile.ZipFile(source) as z:
        for name in z.namelist():
            if name.startswith('ANDROID_STUDIO/app/') and not name.endswith('/'):
                target=(root/name.removeprefix('ANDROID_STUDIO/')) if 'V349' in source else (Path('/mnt/data/ANDROID_V336_REFERENCIA')/name)
                assert target.is_file() and hashlib.sha256(target.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(), name
                verified+=1
    assert verified>=90,(source,verified)
print('PAS_21_PASS: stable per-user viewer, grey bubbles, Android gestures/ratio/return, Android immutable')

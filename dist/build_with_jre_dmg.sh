#! /bin/bash
# -*- coding: utf-8 -*-

if [ ! -d "java8macWithJRE" ]; then
    echo "java8macWithJREがありません" >&2
    exit 1
fi

# プロパティファイルよりバージョン情報の読み込み
VERSION=$(cat ../resources/version.properties | sed -n -E 's/implements_version=([0123456789.]+).*$/\1/p')
echo "version=($VERSION)"

DMG_NAME="CharacterManaJ_${VERSION}_with_jre"
echo "DMG_NAME=${DMG_NAME}.dmg"

WORK_DMG_NAME="${DMG_NAME}_with_jre_work"
echo "WORK_DMG_NAME=${WORK_DMG_NAME}.dmg"

VOLUME_NAME="CharacterManaJ Ver${VERSION}"

if [ -f "$TMPDIR/${WORK_DMG_NAME}.dmg" ]; then
   rm -frv "$TMPDIR/${WORK_DMG_NAME}.dmg"
fi

hdiutil create -size 300m -fs HFS+ -volname "$VOLUME_NAME" -layout NONE -type UDIF "$TMPDIR/${WORK_DMG_NAME}.dmg"

hdiutil attach "$TMPDIR/${WORK_DMG_NAME}.dmg"

DIST_DIR="/Volumes/${VOLUME_NAME}"

cp -v README_mac.txt "$DIST_DIR/README.txt"

echo "copy: java8macWithJRE/CharacterManaJ.app $DIST_DIR/"
ditto java8macWithJRE/CharacterManaJ.app "$DIST_DIR/CharacterManaJ.app"

hdiutil detach "$DIST_DIR"

if [ -f "$TMPDIR/${DMG_NAME}.dmg" ]; then
    rm -f "$TMPDIR/${DMG_NAME}.dmg"
fi

hdiutil convert "$TMPDIR/${WORK_DMG_NAME}.dmg" -format UDZO -imagekey zlib-level=9 -o "$TMPDIR/${DMG_NAME}.dmg"

rm -fv "$TMPDIR/${WORK_DMG_NAME}.dmg"
mv -fv "$TMPDIR/${DMG_NAME}.dmg" .

echo "done"

#! /bin/bash
# -*- coding: utf-8 -*-

if [ ! -d "java6mac" -o ! -d "java8mac" ]; then
    echo "java6mac/java8mac folder not found." >&2
    exit 1
fi

# バージョン情報をプロパティファイルより抜き出す
VERSION=$(cat ../resources/version.properties | sed -n -E 's/implements_version=([0123456789.]+).*$/\1/p')
echo "version=($VERSION)"

# 生成するDMG名
DMG_NAME="CharacterManaJ_${VERSION}"
echo "DMG_NAME=${DMG_NAME}.dmg"

# 作成に一時使用するDMG名
WORK_DMG_NAME="${DMG_NAME}_work"
echo "WORK_DMG_NAME=${WORK_DMG_NAME}.dmg"

# ボリューム名
VOLUME_NAME="CharacterManaJ Ver${VERSION}"

# 一時DMGの削除
if [ -f "$TMPDIR/${WORK_DMG_NAME}.dmg" ]; then
   rm -frv "$TMPDIR/${WORK_DMG_NAME}.dmg"
fi

# 一時DMGの作成
hdiutil create -size 50m -fs HFS+ -volname "$VOLUME_NAME" -layout NONE -type UDIF "$TMPDIR/${WORK_DMG_NAME}.dmg"

# 一時DMGのマウント
hdiutil attach "$TMPDIR/${WORK_DMG_NAME}.dmg"

# マウント先
DIST_DIR="/Volumes/${VOLUME_NAME}"

# 配布物のコピー
cp -v README_mac.txt "$DIST_DIR/README.txt"
cp -v CharacterManaJ.jar "$DIST_DIR/"

# 配布物(java6)のコピー
echo "copy: java6mac $DIST_DIR/java6mac"
cp -rp java6mac "$DIST_DIR/java6mac"

# 配布物(java8)のコピー
echo "copy: java8mac/CharacterManaJ.app $DIST_DIR/"
cp -rp java8mac/CharacterManaJ.app "$DIST_DIR/"

# 配布先に移動
pushd "$DIST_DIR"

echo "*create hardlink"

# jarファイルをハードリンクにする
ln -fv CharacterManaJ.jar java6mac/CharacterManaJ.app/Contents/Resources/Java/CharacterManaJ.jar 

ln -fv CharacterManaJ.jar CharacterManaJ.app/Contents/Java/CharacterManaJ.jar 

popd

# 一時DMGのデタッチ
hdiutil detach "$DIST_DIR"

# 配布用DMGの削除
if [ -f "$TMPDIR/${DMG_NAME}.dmg" ]; then
    rm -f "$TMPDIR/${DMG_NAME}.dmg"
fi

# 一時DMGから圧縮単一ファイル型の配布用DMGに変換
# (convertはHFS+ディスク上で作業する必要がある)
hdiutil convert "$TMPDIR/${WORK_DMG_NAME}.dmg" -format UDZO -imagekey zlib-level=9 -o "$TMPDIR/${DMG_NAME}.dmg"

# 作業済み一時DMGの削除
rm -fv "$TMPDIR/${WORK_DMG_NAME}.dmg"

# 配布用DMGのテンポラリからの移動
mv -fv "$TMPDIR/${DMG_NAME}.dmg" .

echo "done"

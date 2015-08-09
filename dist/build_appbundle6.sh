#! /bin/bash
# -*- coding: utf-8 -*-

JARNAME=CharacterManaJ.jar
APPDIR=java6mac/CharacterManaJ.app

if [ ! -d "$APPDIR" ]; then
    echo "directory not found: $APPDIR" >&2
    exit 1
fi

# 実行可能jarのコピー
cp -pv $JARNAME $APPDIR/Contents/Resources/java/${JARNAME}

# Java起動スタブをコピー
cp -apv "/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub" $APPDIR/Contents/MacOS/

# バンドル属性をセット
/usr/bin/setFile -a B $APPDIR

# Java起動スタブに実行パーミッションを設定
chmod 755 $APPDIR/Contents/MacOS/JavaApplicationStub

# リソースディレクトリのパーミッション設定
chmod -R 774 $APPDIR/Contents/Resources/

echo "done"

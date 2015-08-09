#! /bin/bash
# -*- coding: utf-8 -*-

JARNAME=CharacterManaJ.jar
APPDIR=java8mac/CharacterManaJ.app

PlistBuddy=/usr/libexec/PlistBuddy

if [ ! -d "$APPDIR" ]; then
    echo "directory not found: $APPDIR" >&2
    exit 1
fi

# 実行可能jarのコピー
cp -pv $JARNAME $APPDIR/Contents/java/${JARNAME}

# バンドル属性をセット
/usr/bin/setFile -a B $APPDIR

# Java起動スタブに実行パーミッションを設定
chmod 755 $APPDIR/Contents/MacOS/JavaAppLauncher

# リソースディレクトリのパーミッション設定
chmod -R 774 $APPDIR/Contents/Resources/


# JVMRuntimeの設定を取得する.
jdkname=$($PlistBuddy -c "Print JVMRuntime" $APPDIR/Contents/Info.plist 2>/dev/null)
echo jdkname=$jdkname

if [ ! -z "$jdkname" ]; then
    # JVMRuntimeなしに設定する
    $PlistBuddy -c "Delete :JVMRuntime" $APPDIR/Contents/Info.plist

    # JVMRuntimeを消す
    rm -frv $APPDIR/Contents/PlugIns/${jdkname}
fi

echo "done"

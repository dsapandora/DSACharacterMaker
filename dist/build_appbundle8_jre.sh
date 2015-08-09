#!/bin/bash
# -*- Coding: utf-8 -*-

# システム内のJAVA_HOMEの取得
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8.0)
echo "JAVA_HOME=${JAVA_HOME}"

# info.plistの編集コマンド
PlistBuddy=/usr/libexec/PlistBuddy

#################
# 作業ディレクトリ
export TARGETDIR="withJRE"

# コピー元ディレクトリ
export EXPORTDIRSRC="java8mac"

# JRE同梱版生成先ディレクトリ
export EXPORTDIR="java8macWithJRE"

#################

if [ ! -d $EXPORTDIRSRC ]; then
   echo "not directory $EXPORTDIRSRC" >&2
   exit 1
fi

# まるごとコピーする
echo "copy $EXPORTDIRSRC --> $EXPORTDIR"
rm -fr "$EXPORTDIR"
mkdir -p "$EXPORTDIR"
ditto -v "$EXPORTDIRSRC" "$EXPORTDIR"

# 作業用出力先
mkdir -pv $TARGETDIR

# appbundlerによるjre付きバンドルの作成
ant -f build_appbundle8_jre.xml

# 生成したJRE付きのバンドルファイル内からjdk名を取得する
jdkname=$($PlistBuddy -c "print JVMRuntime" $TARGETDIR/CharacterManaJ.app/Contents/Info.plist)
echo new jdk_name=$jdkname
if [ -z "$jdkname" ]; then
    echo "can't read the new JVMRuntime." >&2
    exit 1
fi

# 現行のjdk名を取得する
oldjdkname=$($PlistBuddy -c "print JVMRuntime" $EXPORTDIR/CharacterManaJ.app/Contents/Info.plist)
echo current: jdk_name=$oldjdkname

if [ "$oldjdkname" = "$jdkname" ]; then
    # 同じjdkバージョンならなにもしない
    echo "*already same jdk"
else
    if [ ! -z "$oldjdkname" ]; then
    # 既存のjdkを消す
	rm -fr $EXPORTDIR/CharacterManaJ.app/Contents/PlugIns/$oldjdkname
    fi

    # 新しいjdk名に書き換える
    if [ -z "$oldjdkname" ]; then
	$PlistBuddy -c "add :JVMRuntime string $jdkname" $EXPORTDIR/CharacterManaJ.app/Contents/Info.plist
    else
	$PlistBuddy -c "set :JVMRuntime $jdkname" $EXPORTDIR/CharacterManaJ.app/Contents/Info.plist
    fi;

    # 新しいjdkをコピーする
    echo "copy $TARGETDIR/CharacterManaJ.app/Contents/PlugIns/$jdkname --> $EXPORTDIR/CharacterManaJ.app/Contents/PlugIns/"
    ditto -v $TARGETDIR/CharacterManaJ.app/Contents/PlugIns/$jdkname $EXPORTDIR/CharacterManaJ.app/Contents/PlugIns/$jdkname
fi

# 生成完了後は不要なので消す 
rm -fr $TARGETDIR

echo "done"

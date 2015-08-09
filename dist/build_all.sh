#!/bin/bash
# -*- coding: utf-8 -*-

echo "*****************************"
echo "Mac用の配布物の一括作成を行います"
echo "*****************************"

./build_appbundle6.sh
if [ $? -ne 0 ]; then
    echo "error" >&2
    exit 1
fi

echo "*****************************"
./build_appbundle8.sh
if [ $? -ne 0 ]; then
    echo "error" >&2
    exit 1
fi

echo "*****************************"
./build_appbundle8_jre.sh
if [ $? -ne 0 ]; then
    echo "error" >&2
    exit 1
fi

echo "*****************************"
./build_dmg.sh
if [ $? -ne 0 ]; then
    echo "error" >&2
    exit 1
fi

echo "*****************************"
./build_with_jre_dmg.sh
if [ $? -ne 0 ]; then
    echo "error" >&2
    exit 1
fi

echo "*****************************"
echo "all done"




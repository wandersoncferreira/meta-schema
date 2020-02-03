#!/bin/bash
set -e
case $1 in
    clj)
        lein test-clj
        ;;
    *)
        echo "Please select [clj]"
        exit 1
        ;;
esac

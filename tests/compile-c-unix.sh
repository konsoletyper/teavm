SOURCE_DIR=$(pwd)
BUILD_DIR=$SOURCE_DIR/build
mkdir -p $BUILD_DIR
pushd $BUILD_DIR >/dev/null && \
cmake -S $SOURCE_DIR -B . >/dev/null && \
make --quiet >/dev/null && \
popd >/dev/null && \
rm -rf $BUILD_DIR
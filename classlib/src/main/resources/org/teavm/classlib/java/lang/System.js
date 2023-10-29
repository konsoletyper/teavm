function doArrayCopy(src, srcPos, dest, destPos, length) {
    if (length !== 0) {
        if (typeof src.data.buffer !== 'undefined') {
            dest.data.set(src.data.subarray(srcPos, srcPos + length), destPos);
        } else if (src !== dest || destPos < srcPos) {
            for (let i = 0; i < length; i = (i + 1) | 0) {
                dest.data[destPos++] = src.data[srcPos++];
            }
        } else {
            srcPos = (srcPos + length) | 0;
            destPos = (destPos + length) | 0;
            for (let i = 0; i < length; i = (i + 1) | 0) {
                dest.data[--destPos] = src.data[--srcPos];
            }
        }
    }
}
function currentTimeMillis() {
    return Long_fromNumber(new (teavm_globals.Date)().getTime());
}
static inline int32_t instanceof(void* obj, int32_t (*cls)(JavaClass*)) {
    return obj != NULL && cls(CLASS_OF(obj));
}

static inline void* checkcast(void* obj, int32_t (*cls)(JavaClass*)) {
    return obj == NULL || cls(CLASS_OF(obj)) ? obj : throwClassCastException();
}
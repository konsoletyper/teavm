__attribute__((import_module("env"), import_name("malloc")))
extern void* teavm_malloc(unsigned long size);

__attribute__((import_module("env"), import_name("free")))
extern void teavm_free(void* ptr);

__attribute__((import_module("env"), import_name("realloc")))
extern void* teavm_realloc(void* ptr, unsigned long size);

void *malloc(unsigned long size) {
  return teavm_malloc(size);
}

void free(void* ptr) {
  teavm_free(ptr);
}

void* realloc(void* ptr, unsigned long size) {
  return teavm_realloc(ptr, size);
}
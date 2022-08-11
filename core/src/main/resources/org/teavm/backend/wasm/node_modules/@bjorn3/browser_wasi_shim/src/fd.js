// @flow

import * as wasi from "./wasi_defs.js";

export class Fd {
    fd_advise(offset/*: number*/, len/*: BigInt*/, advice/*: number*/)/*: number*/ {
        return -1;
    }
    fd_allocate(offset/*: number*/, len/*: BigInt*/)/*: number*/ {
        return -1;
    }
    fd_close()/*: number*/ {
        return -1;
    }
    fd_datasync()/*: number*/ {
        return -1;
    }
    fd_fdstat_get()/*: { ret: number, fdstat: wasi.Fdstat | null }*/ {
        return { ret: -1, fdstat: null };
    }
    fd_fdstat_set_flags(flags/*: number*/)/*: number*/ {
        return -1;
    }
    fd_fdstat_set_rights(fs_rights_base/*: BigInt*/, fs_rights_inheriting/*: BigInt*/)/*: number*/ {
        return -1;
    }
    fd_filestat_get() {
        return { ret: -1, filestat: null };
    }
    fd_filestat_set_size(size)/*: number*/ {
        return -1;
    }
    fd_filestat_set_times(atim, mtim, fst_flags)/*: number*/ {
        return -1;
    }
    fd_pread(view8/*: Uint8Array*/, iovs, offset) {
        return { ret: -1, nread: 0 };
    }
    fd_prestat_get() {
        return { ret: -1, prestat: null };
    }
    fd_prestat_dir_name(path_ptr/*: number*/, path_len/*: number*/) {
        return { ret: -1, prestat_dir_name: null };
    }
    fd_pwrite(view8/*: Uint8Array*/, iovs, offset) {
        return { ret: -1, nwritten: 0 };
    }
    fd_read(view8/*: Uint8Array*/, iovs/*: Array<wasi.Iovec>*/)/*: { ret: number, nread: number }*/ {
        return { ret: -1, nread: 0 };
    }
    fd_readdir_single(cookie/*: BigInt*/) {
        return { ret: -1, dirent: null };
    }
    fd_seek(offset, whence) {
        return { ret: -1, offset: 0 };
    }
    fd_sync()/*: number*/ {
        return -1;
    }
    fd_tell() {
        return { ret: -1, offset: 0 };
    }
    fd_write(view8, iovs) {
        return { ret: -1, nwritten: 0 };
    }
    path_create_directory(path)/*: number*/ {
        return -1;
    }
    path_filestat_get(flags, path) {
        return { ret: -1, filestat: null };
    }
    path_filestat_set_times(flags, path, atim, mtim, fst_flags) {
        return -1;
    }
    path_link(old_fd, old_flags, old_path, new_path)/*: number*/ {
        return -1;
    }
    path_open(dirflags, path, oflags, fs_rights_base, fs_rights_inheriting, fdflags) {
        return { ret: -1, fd_obj: null };
    }
    path_readlink(path) {
        return { ret: -1, data: null };
    }
    path_remove_directory(path)/*: number*/ {
        return -1;
    }
    path_rename(old_path, new_fd, new_path)/*: number*/ {
        return -1;
    }
    path_symlink(old_path, new_path)/*: number*/ {
        return -1;
    }
    path_unlink_file(path)/*: number*/ {
        return -1;
    }
}

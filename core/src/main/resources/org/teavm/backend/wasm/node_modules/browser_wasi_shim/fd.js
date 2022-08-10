export class Fd {
    fd_advise(offset, len, advice) {
        return -1;
    }
    fd_allocate(offset, len) {
        return -1;
    }
    fd_close() {
        return -1;
    }
    fd_datasync() {
        return -1;
    }
    fd_fdstat_get() {
        return { ret: -1, fdstat: null };
    }
    fd_fdstat_set_flags(flags) {
        return -1;
    }
    fd_fdstat_set_rights(fs_rights_base, fs_rights_inheriting) {
        return -1;
    }
    fd_filestat_get() {
        return { ret: -1, filestat: null };
    }
    fd_filestat_set_size(size) {
        return -1;
    }
    fd_filestat_set_times(atim, mtim, fst_flags) {
        return -1;
    }
    fd_pread(view8, iovs, offset) {
        return { ret: -1, nread: 0 };
    }
    fd_prestat_get() {
        return { ret: -1, prestat: null };
    }
    fd_prestat_dir_name(path_ptr, path_len) {
        return { ret: -1, prestat_dir_name: null };
    }
    fd_pwrite(view8, iovs, offset) {
        return { ret: -1, nwritten: 0 };
    }
    fd_read(view8, iovs) {
        return { ret: -1, nread: 0 };
    }
    fd_readdir_single(cookie) {
        return { ret: -1, dirent: null };
    }
    fd_seek(offset, whence) {
        return { ret: -1, offset: 0 };
    }
    fd_sync() {
        return -1;
    }
    fd_tell() {
        return { ret: -1, offset: 0 };
    }
    fd_write(view8, iovs) {
        return { ret: -1, nwritten: 0 };
    }
    path_create_directory(path) {
        return -1;
    }
    path_filestat_get(flags, path) {
        return { ret: -1, filestat: null };
    }
    path_filestat_set_times(flags, path, atim, mtim, fst_flags) {
        return -1;
    }
    path_link(old_fd, old_flags, old_path, new_path) {
        return -1;
    }
    path_open(dirflags, path, oflags, fs_rights_base, fs_rights_inheriting, fdflags) {
        return { ret: -1, fd_obj: null };
    }
    path_readlink(path) {
        return { ret: -1, data: null };
    }
    path_remove_directory(path) {
        return -1;
    }
    path_rename(old_path, new_fd, new_path) {
        return -1;
    }
    path_symlink(old_path, new_path) {
        return -1;
    }
    path_unlink_file(path) {
        return -1;
    }
}

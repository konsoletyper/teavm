package wasi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.TimeZone;
import org.teavm.interop.Export;

public class Test {
    public static void main(String[] args) throws IOException {
        switch (args[0]) {
        case "echo-args": {
            for (int i = 1; i < args.length; ++i) {
                System.out.print(args[i]);
                if (i < args.length - 1) {
                    System.out.print(" ");
                }
            }
            break;
        }
        case "random": {
            System.out.print(Math.random());
            break;
        }
        case "env": {
            System.out.print(System.getenv(args[1]));
            System.out.print(System.getenv(args[2]));
            break;
        }
        case "floats": {
            float n1 = Float.parseFloat(args[1]);
            float n2 = Float.parseFloat(args[2]);
            System.out.print("" + Float.isNaN(n1 / n2));
            System.out.print(":" + Float.isFinite(n1 / n2));
            System.out.print(":" + Float.isInfinite(n1 / n2));
            break;
        }
        case "doubles": {
            double n1 = Double.parseDouble(args[1]);
            double n2 = Double.parseDouble(args[2]);
            System.out.print("" + Double.isNaN(n1 / n2));
            System.out.print(":" + Double.isFinite(n1 / n2));
            System.out.print(":" + Double.isInfinite(n1 / n2));
            break;
        }
        case "catch": {
            try {
                doThrow(args[1]);
            } catch (Exception e) {
                System.out.print(e.getMessage());
            }
            break;
        }
        case "epoch": {
            System.out.print(System.currentTimeMillis());
            break;
        }
        case "stdin": {
            byte[] buffer = new byte[256];
            int count = 0;
            while ((count = System.in.read(buffer, 0, buffer.length)) != -1) {
                System.out.write(buffer, 0, count);
            }
            break;
        }
        case "mkdirs": {
            if (!new File(args[1]).mkdirs()) {
                throw new AssertionError();
            }
            break;
        }
        case "create": {
            if (!new File(args[1]).createNewFile()) {
                throw new AssertionError();
            }
            break;
        }
        case "create_already_exists": {
            if (new File(args[1]).createNewFile()) {
                throw new AssertionError();
            }
            break;
        }
        case "write": {
            try (PrintStream out = new PrintStream(new FileOutputStream(args[1]))) {
                out.print(args[2]);
            }
            break;
        }
        case "read": {
            try (InputStream in = new FileInputStream(args[1])) {
                System.out.println(readString(in));
            }
            break;
        }
        case "seek": {
            long position = Long.parseLong(args[2]);
            RandomAccessFile file = new RandomAccessFile(args[1], "r");
            long length = file.length();
            file.seek(position);
            if (position != file.getFilePointer()) {
                throw new AssertionError();
            }
            byte[] buffer = new byte[(int) (length - position)];
            file.readFully(buffer);
            System.out.write(buffer);
            break;
        }
        case "resize": {
            long length = Long.parseLong(args[2]);
            new RandomAccessFile(args[1], "rw").setLength(length);
            break;
        }
        case "length": {
            System.out.println(new File(args[1]).length());
            break;
        }
        case "rename": {
            if (!new File(args[1]).renameTo(new File(args[2]))) {
                throw new AssertionError();
            }
            break;
        }
        case "delete": {
            if (!new File(args[1]).delete()) {
                throw new AssertionError();
            }
            break;
        }
        case "list": {
            File[] files = new File(args[1]).listFiles();
            String[] names = new String[files.length];
            for (int i = 0; i < files.length; ++i) {
                names[i] = files[i].getName();
            }
            Arrays.sort(names);

            for (int i = 0; i < names.length; ++i) {
                System.out.print(names[i]);
                if (i < names.length - 1) {
                    System.out.print(" ");
                }
            }
            break;
        }
        case "mtime": {
            long time = Long.parseLong(args[2]);
            if (!new File(args[1]).setLastModified(time)) {
                throw new AssertionError();
            }
            if (time != new File(args[1]).lastModified()) {
                throw new AssertionError();
            }
            break;
        }
        case "bad-mkdirs": {
            if (new File(args[1]).mkdirs()) {
                throw new AssertionError();
            }
            System.out.print("SUCCESS");
            break;
        }
        case "bad-create": {
            try {
                new File(args[1]).createNewFile();
                throw new AssertionError();
            } catch (IOException e) {
                System.out.print("SUCCESS");
            }
            break;
        }
        case "bad-write": {
            try {
                new FileOutputStream(args[1]);
                throw new AssertionError();
            } catch (FileNotFoundException e) {
                System.out.print("SUCCESS");
            }
            break;
        }
        case "bad-read": {
            try {
                new FileInputStream(args[1]);
                throw new AssertionError();
            } catch (FileNotFoundException e) {
                System.out.print("SUCCESS");
            }
            break;
        }
        case "bad-random-access": {
            try {
                new RandomAccessFile(args[1], "r");
                throw new AssertionError();
            } catch (FileNotFoundException e) {
                System.out.print("SUCCESS");
            }
            break;
        }
        case "bad-length": {
            if (new File(args[1]).length() != 0) {
                throw new AssertionError();
            }
            System.out.print("SUCCESS");
            break;
        }
        case "bad-rename": {
            if (new File(args[1]).renameTo(new File(args[2]))) {
                throw new AssertionError();
            }
            System.out.print("SUCCESS");
            break;
        }
        case "bad-delete": {
            if (new File(args[1]).delete()) {
                throw new AssertionError();
            }
            System.out.print("SUCCESS");
            break;
        }
        case "bad-list": {
            if (new File(args[1]).listFiles() != null) {
                throw new AssertionError();
            }
            System.out.print("SUCCESS");
            break;
        }
        case "bad-mtime": {
            if (new File(args[1]).lastModified() != 0) {
                throw new AssertionError();
            }
            System.out.print("SUCCESS");
            break;
        }
        default:
            System.err.println("unknown command: " + args[0]);
        }
    }

    private static void doThrow(String message) throws Exception {
        throw new Exception(message);
    }

    private static String readString(InputStream in) throws IOException {
        byte[] buffer = new byte[256];
        int offset = 0;
        int count = 0;
        while ((count = in.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += count;

            if (offset >= buffer.length) {
                throw new IOException("buffer overflow");
            }
        }

        return new String(buffer, 0, offset, StandardCharsets.UTF_8).trim();
    }
}

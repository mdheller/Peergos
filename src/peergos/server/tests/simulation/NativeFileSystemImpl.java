package peergos.server.tests.simulation;

import peergos.shared.user.fs.FileProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NativeFileSystemImpl implements FileSystem {

    private final Path root;
    private final String user;
    private List<String> followers = new ArrayList<>();
    private List<String> following = new ArrayList<>();
    private List<String> requestsToFollow = new ArrayList<>();

    private final AccessControl accessControl = new AccessControl.MemoryImpl();
    private final FileSystemRepository repo;

    public NativeFileSystemImpl(Path root, String user, FileSystemRepository repo) {
        this.root = root;
        this.user = user;
        this.repo = repo;
        init();
    }

    private void init() {
        Path userRoot = Paths.get("/" + user);
//        Path sharedRoot = userRoot.resolve("shared");
//        Path peergosShare = sharedRoot.resolve("peergos");

        for (Path path : Arrays.asList(
                userRoot
//                , sharedRoot,
//                peergosShare
        )) {
            accessControl.add(path, user(), Permission.WRITE);
            accessControl.add(path, user(), Permission.READ);
            mkdir(path);
        }

    }

    @Override
    public String user() {
        return user;
    }

    private void ensureCan(Path path, Permission permission) {
        ensureCan(path, permission, user());
    }

    private void ensureCan(Path path, Permission permission, String user) {
        Path nativePath = virtualToNative(path);
        if (! Files.exists(nativePath) && permission == Permission.READ)
            throw new IllegalStateException("Cannot read "+ path +" : native file "+ nativePath + " does not exist.");

        if (! accessControl.can(path, user, permission))
            throw new IllegalStateException("User " + user() +" not permitted to "+ permission + " " + path);
    }



    @Override
    public void acceptFollower(String fromUser, boolean reciprocate) {
        if (! requestsToFollow.contains(fromUser))
            throw new IllegalStateException("No follow request from "+ fromUser);
        requestsToFollow.remove(fromUser);
        followers.add(fromUser);
        if (reciprocate) {
            requestToFollow(fromUser);
        }
    }

    @Override
    public void requestToFollow(String user) {
        ((NativeFileSystemImpl) repo.get(user)).requestsToFollow.add(user());
    }

    @Override
    public byte[] read(Path path) {
        Path nativePath = virtualToNative(path);
        ensureCan(path, Permission.READ);

        try {
            return Files.readAllBytes(nativePath);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    @Override
    public void write(Path path, byte[] data) {

        Path nativePath = virtualToNative(path);
        ensureCan(path.getParent(), Permission.READ);
        ensureCan(path, Permission.WRITE);

        try {
            Files.write(nativePath, data);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    @Override
    public void delete(Path path) {
        ensureCan(path, Permission.WRITE);

        walk(path, p -> {
                try {
                    Files.delete(virtualToNative(p));
                } catch (IOException ioe) {
                    throw new IllegalStateException(ioe);
                }
        });

    }

    @Override
    public List<String> following() {
        return new ArrayList<>(following);
    }

    @Override
    public List<String> followers() {
        return new ArrayList<>(followers);
    }


    private boolean isOwner(Path path) {
        return user().equals(accessControl.getOwner(path));
    }

    @Override
    public void grant(Path path, String user, FileSystem.Permission permission) {
        ensureCan(path, permission, user);
        if (! followers.contains(user))
            throw new IllegalStateException("User "+ user + " is not  following "+ user());
        accessControl.add(path, user, permission);
    }

    @Override
    public void revoke(Path path, String user, FileSystem.Permission permission) {
        ensureCan(path, permission, user);
        if (! followers.contains(user))
            throw new IllegalStateException("User "+ user + " is not  following "+ user());
        accessControl.remove(path, user, permission);
    }

    @Override
    public Stat stat(Path path) {
        return new Stat() {
            @Override
            public String user() {
                return user;
            }

            @Override
            public FileProperties fileProperties() {
                File file = virtualToNative(path).toFile();

                long length = file.length();

                file.lastModified();
                if (Integer.MAX_VALUE < length)
                    throw new IllegalStateException("Large files not supported");
                int sizeLo = (int) length;
                int sizeHi = 0;

                LocalDateTime lastModified = LocalDateTime.ofInstant(Instant.ofEpochSecond(file.lastModified() / 1000),
                        ZoneOffset.systemDefault());

                // These things are a bit awkward, not supporting them for now
                Optional<byte[]> thumbnail = Optional.empty();
                boolean isHidden = false;
                String mimeType="NOT_SUPPORTED";

                return new FileProperties(file.getName(), file.isDirectory(), mimeType, sizeHi, sizeLo, lastModified, isHidden, thumbnail);

            }

            @Override
            public boolean isReadable() {
                return accessControl.can(path, user(), Permission.READ);
            }

            @Override
            public boolean isWritable() {
                return accessControl.can(path, user(), Permission.WRITE);
            }
        };
    }

    private Path virtualToNative(Path path) {
        Path relativePath = Paths.get("/").relativize(path);
        return Paths.get(root.toString(), relativePath.toString());
    }

    @Override
    public void mkdir(Path path) {
        if (! path.equals(Paths.get("/"+ user()))) {
            Path parentDir = path.getParent();
            ensureCan(parentDir, Permission.WRITE);
        }


        Path nativePath = virtualToNative(path);
        boolean mkdir = nativePath.toFile().mkdir();
        if (! mkdir)
            throw new IllegalStateException("Could not make dir "+ nativePath);
    }

    @Override
    public List<Path> ls(Path path) {
        Path nativePath = virtualToNative(path);
        try {
            return Files.list(nativePath)
                    .map(e -> path.resolve(e.getFileName().toString()))
                    .collect(Collectors.toList());

        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    public static void main(String[] args) {
        System.out.println("HELO");

        Path p1 = Paths.get("/something/else");
        Path p2 = Paths.get("/another/thing");

        Path p3 = Paths.get("/").relativize(p2);
        Path p4 = Paths.get(p1.toString(), p3.toString());

        System.out.println(p4);

        Path p5 = Paths.get("/some/thing/else");
        System.out.println(p5.getName(1));

    }
}

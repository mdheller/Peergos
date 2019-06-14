package peergos.server.tests.simulation;

import peergos.server.util.PeergosNetworkUtils;
import peergos.shared.Crypto;
import peergos.shared.NetworkAccess;
import peergos.shared.user.UserContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static peergos.server.tests.simulation.Simulator.buildNetwork;

public interface FileSystemRepository {
    FileSystem get(String user);
    void add(FileSystem fs);

    /**
     * Can only share things with people who are following you...
     */
    class Impl implements FileSystemRepository {
        private Map<String, FileSystem> userToFileSystem = new HashMap<>();

        @Override
        public FileSystem get(String user) {
            FileSystem fileSystem = userToFileSystem.get(user);
            if (fileSystem == null)
                throw new IllegalStateException("User "+ user + " unknown!");
            return fileSystem;
        }

        @Override
        public void add(FileSystem fs) {
            if (userToFileSystem.containsKey(fs.user()))
                throw new IllegalStateException("User "+ fs.user() + " already in repo");
            userToFileSystem.put(fs.user(), fs);
        }
    }

    public static FileSystemRepository build(List<FileSystem> fileSystems) {
        FileSystemRepository repo = new FileSystemRepository.Impl();
        fileSystems.forEach(repo::add);

        for (int i = 0; i < fileSystems.size(); i++) {
            for (int j = i+1; j < fileSystems.size(); j++) {
                if (i <=j)
                    continue;
                FileSystem left = fileSystems.get(i);
                FileSystem right = fileSystems.get(j);
                left.requestToFollow(right.user());
                right.acceptFollower(left.user(), true);
                left.acceptFollower(right.user(), false);
            }
        }

        return repo;
    }


    public static void main(String[] args) throws Exception {
        Crypto crypto = Crypto.initJava();
        NetworkAccess networkAccess = buildNetwork();
        int nUsers  = 3;
        List<FileSystem> peergosFileSystems = IntStream.range(1, nUsers).mapToObj(Integer::toString)
                .map(s -> {
                    UserContext userContext = PeergosNetworkUtils.ensureSignedUp(s, s, networkAccess, crypto);
                    return new PeergosFileSystemImpl(userContext);
                }).collect(Collectors.toList());

        List<FileSystem> nativeFileSystems = IntStream.range(1, nUsers).mapToObj(Integer::toString)
                .map(e -> {
                    try {
                        Path root = Files.createTempDirectory("test_filesystem");
                        FileSystemRepository repo = new FileSystemRepository.Impl();
                        return new NativeFileSystemImpl(root, e, repo);
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                }).collect(Collectors.toList());

        FileSystemRepository peergosFSrepo = build(peergosFileSystems);
        FileSystemRepository nativeFSrepo = build(nativeFileSystems);

        System.exit(0);

    }
}


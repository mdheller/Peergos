package peergos.server.tests.simulation;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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



        public static FileSystemRepository FileSystemRepositoryFactory(int userCount, Supplier<FileSystem> fsSupplier) {
            FileSystemRepository repo = new FileSystemRepository.Impl();
            for (int i = 0; i < userCount; i++)
                repo.add(fsSupplier.get());

            Impl r = (Impl) repo;
            List<String> users = new ArrayList<>(r.userToFileSystem.keySet());

            int nUsers = users.size();
            for (int i = 0; i < nUsers; i++) {
                for (int j = i+1; j < nUsers; j++) {
                    if (i <=j)
                        continue;
                    FileSystem left = r.userToFileSystem.get(users.get(i));
                    FileSystem right = r.userToFileSystem.get(users.get(j));

                }

            }
            for (FileSystem fs : r.userToFileSystem.values()) {
                for (String user : users) {
                    if (fs.user().equals(user))
                        continue;
                }
            }
            return null;
        }
    }

}


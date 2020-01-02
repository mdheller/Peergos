package peergos.server.storage;

import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.*;
import peergos.shared.cbor.*;
import peergos.shared.crypto.hash.*;
import peergos.shared.io.ipfs.cid.*;
import peergos.shared.io.ipfs.multibase.binary.*;
import peergos.shared.storage.*;
import peergos.shared.io.ipfs.multihash.*;
import io.prometheus.client.Histogram;
import peergos.shared.util.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.*;

public class S3BlockStorage implements ContentAddressedStorage {
    private static final Logger LOG = Logger.getGlobal();

    private static final Histogram readTimerLog = Histogram.build()
            .labelNames("filesize")
            .name("block_read_seconds").help("Time to read a block from immutable storage")
            .exponentialBuckets(0.01, 2, 16).register();
    private static final Histogram writeTimerLog = Histogram.build()
            .labelNames("filesize")
            .name("s3_block_write_seconds").help("Time to read a block from immutable storage")
            .exponentialBuckets(0.01, 2, 16).register();

    private final Multihash id;
    private final AmazonS3 s3Client;
    private final String bucket, folder;

    public S3BlockStorage(String region, String accessKey, String secretKey, String bucket, String folder, Multihash id) {
        this.id = id;
        this.bucket = bucket;
        this.folder = folder.isEmpty() || folder.endsWith("/") ? folder : folder + "/";
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
        s3Client = builder.build();
    }

    private static String hashToKey(Multihash hash) {
        // To be compatible with IPFS we use the ame scheme here, the cid bytes encoded as uppercase base32
        return new Base32().encodeAsString(hash.toBytes());
    }

    private Multihash keyToHash(String key) {
        // To be compatible with IPFS we use the ame scheme here, the cid bytes encoded as uppercase base32
        byte[] decoded = new Base32().decode(key.substring(folder.length()));
        return Cid.cast(decoded);
    }

    @Override
    public CompletableFuture<Optional<CborObject>> get(Multihash hash) {
        if (hash instanceof Cid && ((Cid) hash).codec == Cid.Codec.Raw)
            throw new IllegalStateException("Need to call getRaw if cid is not cbor!");
        return getRaw(hash).thenApply(opt -> opt.map(CborObject::fromByteArray));
    }

    @Override
    public CompletableFuture<Optional<byte[]>> getRaw(Multihash hash) {
        return Futures.of(map(hash, h -> {
            GetObjectRequest get = new GetObjectRequest(bucket, folder + hashToKey(hash));
            Histogram.Timer readTimer = readTimerLog.labels(h.toString()).startTimer();
            try (S3Object res = s3Client.getObject(get); DataInputStream din = new DataInputStream(new BufferedInputStream(res.getObjectContent()))) {
                return Optional.of(Serialize.readFully(din));
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                readTimer.observeDuration();
            }
        }, e -> Optional.empty()));
    }

    @Override
    public CompletableFuture<List<Multihash>> pinUpdate(PublicKeyHash owner, Multihash existing, Multihash updated) {
        return Futures.of(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<Multihash>> recursivePin(PublicKeyHash owner, Multihash hash) {
        return Futures.of(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<Multihash>> recursiveUnpin(PublicKeyHash owner, Multihash hash) {
        return Futures.of(Collections.emptyList());
    }

    @Override
    public CompletableFuture<Boolean> gc() {
        return Futures.errored(new IllegalStateException("S3 doesn't implement GC!"));
    }

    @Override
    public CompletableFuture<Optional<Integer>> getSize(Multihash hash) {
        return Futures.of(map(hash, h -> {
            GetObjectRequest get = new GetObjectRequest(bucket, folder + hashToKey(hash));
            Histogram.Timer readTimer = readTimerLog.labels(h.toString()).startTimer();
            try (S3Object res = s3Client.getObject(get)) {
                return Optional.of((int)res.getObjectMetadata().getContentLength());
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                readTimer.observeDuration();
            }
        }, e -> Optional.empty()));
    }

    public boolean contains(Multihash key) {
        return map(key, h -> s3Client.doesObjectExist(bucket, folder + hashToKey(h)), e -> false);
    }

    public <T> T map(Multihash hash, Function<Multihash, T> success, Function<Throwable, T> absent) {
        try {
            return success.apply(hash);
        } catch (AmazonServiceException e) {
            /* Caught an AmazonServiceException,
               which means our request made it
               to Amazon S3, but was rejected with an error response
               for some reason.
            */
            if ("NoSuchKey".equals(e.getErrorCode())) {
                Histogram.Timer readTimer = readTimerLog.labels("0").startTimer();
                readTimer.observeDuration();
                return absent.apply(e);
            }
            LOG.warning("AmazonServiceException: " + e.getMessage());
            LOG.warning("AWS Error Code:   " + e.getErrorCode());
            throw new RuntimeException(e.getMessage(), e);
        } catch (AmazonClientException e) {
            /* Caught an AmazonClientException,
               which means the client encountered
               an internal error while trying to communicate
               with S3, such as not being able to access the network.
            */
            LOG.severe("AmazonClientException: " + e.getMessage());
            LOG.severe("Thrown at:" + e.getCause().toString());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<Multihash> id() {
        return Futures.of(id);
    }

    @Override
    public CompletableFuture<TransactionId> startTransaction(PublicKeyHash owner) {
        // TODO delegate this to a sql table
        return CompletableFuture.completedFuture(new TransactionId(Long.toString(System.currentTimeMillis())));
    }

    @Override
    public CompletableFuture<Boolean> closeTransaction(PublicKeyHash owner, TransactionId tid) {
        // TODO delegate this to a sql table
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<List<Multihash>> put(PublicKeyHash owner,
                                                  PublicKeyHash writer,
                                                  List<byte[]> signatures,
                                                  List<byte[]> blocks,
                                                  TransactionId tid) {
        return put(writer, signatures, blocks, false);
    }

    @Override
    public CompletableFuture<List<Multihash>> putRaw(PublicKeyHash owner,
                                                     PublicKeyHash writer,
                                                     List<byte[]> signatures,
                                                     List<byte[]> blocks,
                                                     TransactionId tid) {
        return put(writer, signatures, blocks, true);
    }

    private CompletableFuture<List<Multihash>> put(PublicKeyHash writer,
                                                   List<byte[]> signatures,
                                                   List<byte[]> blocks,
                                                   boolean isRaw) {
        return CompletableFuture.completedFuture(blocks.stream()
                .map(b -> put(b, isRaw))
                .collect(Collectors.toList()));
    }

    /** Must be atomic relative to reads of the same key
     *
     * @param data
     */
    public Multihash put(byte[] data, boolean isRaw) {
        Histogram.Timer writeTimer = writeTimerLog.labels("" +data.length).startTimer();
        try {
            Multihash hash = new Multihash(Multihash.Type.sha2_256, Hash.sha256(data));
            Cid cid = new Cid(1, isRaw ? Cid.Codec.Raw : Cid.Codec.DagCbor, hash.type, hash.getHash());
            if (contains(cid))
                return cid;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.length);
            PutObjectRequest put = new PutObjectRequest(bucket, folder + hashToKey(cid), new ByteArrayInputStream(data), metadata);
            PutObjectResult putResult = s3Client.putObject(put);
            return cid;
        } catch (AmazonServiceException e) {
            /* Caught an AmazonServiceException,
               which means our request made it
               to Amazon S3, but was rejected with an error response
               for some reason.
            */
            LOG.severe("AmazonServiceException: " + e.getMessage());
            LOG.severe("AWS Error Code:   " + e.getErrorCode());
            throw new RuntimeException(e.getMessage(), e);
        } catch (AmazonClientException e) {
            /* Caught an AmazonClientException,
               which means the client encountered
               an internal error while trying to communicate
               with S3, such as not being able to access the network.
            */
            LOG.severe("AmazonClientException: " + e.getMessage());
            LOG.severe("Thrown at:" + e.getCause().toString());
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            writeTimer.observeDuration();
        }
    }

    private List<Multihash> getFiles(long maxReturned) {
        List<Multihash> results = new ArrayList<>();
        applyToAll(obj -> results.add(keyToHash(obj.getKey())), maxReturned);
        return results;
    }

    private List<String> getFilenames(long maxReturned) {
        List<String> results = new ArrayList<>();
        applyToAll(obj -> results.add(obj.getKey()), maxReturned);
        return results;
    }

    private void applyToAll(Consumer<S3ObjectSummary> processor, long maxObjects) {
        try {
            LOG.log(Level.FINE, "Listing blobs");
            final ListObjectsV2Request req = new ListObjectsV2Request()
                    .withBucketName(bucket)
                    .withPrefix(folder)
                    .withMaxKeys(10_000);
            ListObjectsV2Result result;
            long processedObjects = 0;
            do {
                result = s3Client.listObjectsV2(req);

                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                    if (objectSummary.getKey().endsWith("/")) {
                        LOG.fine(" - " + objectSummary.getKey() + "  " + "(directory)");
                        continue;
                    }
                    LOG.fine(" - " + objectSummary.getKey() + "  " +
                            "(size = " + objectSummary.getSize() +
                            "; modified: " + objectSummary.getLastModified() + ")");
                    processor.accept(objectSummary);
                    processedObjects++;
                    if (processedObjects >= maxObjects)
                        return;
                }
                LOG.log(Level.FINE, "Next Continuation Token : " + result.getNextContinuationToken());
                req.setContinuationToken(result.getNextContinuationToken());
            } while (result.isTruncated());

        } catch (AmazonServiceException ase) {
            /* Caught an AmazonServiceException,
               which means our request made it
               to Amazon S3, but was rejected with an error response
               for some reason.
            */
            LOG.warning("AmazonServiceException: " + ase.getMessage());
            LOG.warning("AWS Error Code:   " + ase.getErrorCode());

        } catch (AmazonClientException ace) {
            /* Caught an AmazonClientException,
               which means the client encountered
               an internal error while trying to communicate
               with S3, such as not being able to access the network.
            */
            LOG.severe("AmazonClientException: " + ace.getMessage());
            LOG.severe("Thrown at:" + ace.getCause().toString());
        }
    }

    public void delete(Multihash hash) {
        DeleteObjectRequest del = new DeleteObjectRequest(bucket, folder + hashToKey(hash));
        s3Client.deleteObject(del);
    }

    public static void main(String[] args) {
        // Use this method to test access to a bucket
        String bucket = args[0];
        String accessKey = args[1];
        String secretKey = args[2];
        String region = args.length > 3 ? args[3] : "eu-west-1";
        String folder = "";

        System.out.println("Testing S3 bucket: " + bucket + " in region " + region + " with base dir: " + folder);
        Multihash id = new Multihash(Multihash.Type.sha2_256, RAMStorage.hash("S3Storage".getBytes()));
        S3BlockStorage s3 = new S3BlockStorage(region, accessKey, secretKey, bucket, folder, id);

        System.out.println("***** Testing ls and read on bucket " + bucket);
        System.out.println("Testing ls...");
        List<Multihash> files = s3.getFiles(1000);
        System.out.println("Success! found " + files.size());

        System.out.println("Testing read...");
        byte[] data = s3.getRaw(files.get(0)).join().get();
        System.out.println("Success: read blob of size " + data.length);

        System.out.println("Testing write...");
        byte[] uploadData = new byte[10 * 1024];
        new Random().nextBytes(uploadData);
        Multihash put = s3.put(uploadData, true);
        System.out.println("Success!");

        System.out.println("Testing delete...");
        s3.delete(put);
        System.out.println("Success!");
    }

    @Override
    public String toString() {
        return "S3BlockStore[" + bucket + ":" + folder + "]";
    }
}
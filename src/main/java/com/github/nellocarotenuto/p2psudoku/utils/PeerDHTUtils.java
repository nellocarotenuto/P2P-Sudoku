package com.github.nellocarotenuto.p2psudoku.utils;

import java.util.Map;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.FutureRemove;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;
import org.javatuples.Pair;

/**
 * An helper class that exposes common DHT operations but handling concurrency.
 */
public class PeerDHTUtils {

    /**
     * Creates a new entry in the DHT if one with the same key doesn't already exist.
     *
     * @param dht the DHT to add the object in
     * @param key the key at which to put the new object
     * @param data the data to put into the DHT
     *
     * @throws ElementAlreadyExistsException if an element with the same key already exists in the DHT
     * @throws FailedOperationException if something goes wrong when performing the operation on the DHT
     */
    public static void create(PeerDHT dht, Number160 key, Data data) throws Exception {
        FutureGet get = dht.get(key)
                           .getLatest()
                           .start();

        while (!get.isCompleted());

        if (!get.isSuccess()) {
            throw new FailedOperationException("Unable to create the element in the DHT.");
        }

        if (!get.isEmpty()) {
            throw new ElementAlreadyExistsException("Element " + key + " already exists in the DHT.");
        }

        FuturePut put = dht.put(key)
                           .data(data)
                           .putIfAbsent()
                           .start();

        while (!put.isCompleted());

        if (!put.isSuccess()) {
            throw new FailedOperationException("Unable to create the element in the DHT.");
        }
    }

    /**
     * Gets the latest version of an element from the DHT and guarantees that it is the version every peer agrees on.
     *
     * @param dht the DHT to retrieve the object from
     * @param key the key of the element to retrieve
     *
     * @return the full element retrieved
     *
     * @throws ElementNotFoundException if no element is associated to the key in the DHT
     * @throws FailedOperationException if something goes wrong when performing the operation on the DHT
     */
    public static Pair<Number640, Data> get(PeerDHT dht, Number160 key) throws Exception {
        FutureGet get = dht.get(key)
                           .getLatest()
                           .start();

        while (!get.isCompleted());

        if (!get.isSuccess()) {
            throw new FailedOperationException("Unable to get the element " + key + " from the DHT.");
        }

        if (get.isEmpty()) {
            throw new ElementNotFoundException("Element " + key + " doesn't exist in the DHT.");
        }

        try {
            return checkLatestVersion(get.rawData());
        } catch (UnalignedElementsException e) {
            throw new FailedOperationException("Unable to get the element " + key + " from the DHT.");
        }
    }

    /**
     * Updates an element in the DHT handling if every peer agrees on the current version of the element.
     *
     * @param dht  the DHT to update the object in
     * @param pair the pair to put into the DHT (old key and new data)
     *
     * @throws FailedOperationException if something goes wrong when performing the operation on the DHT
     */
    public static void update(PeerDHT dht, Pair<Number640, Data> pair) throws Exception {
        Number640 key = pair.getValue0();
        Data data = pair.getValue1();

        data.addBasedOn(key.versionKey());

        Number160 location = key.locationKey();
        Number160 version = new Number160(key.versionKey().timestamp() + 1, data.hash());

        FuturePut put1 = dht.put(location)
                            .data(data.prepareFlag(), version)
                            .start();

        while (!put1.isCompleted());

        try {
            Pair<Number640, Byte> latest = checkLatestVersion(put1.rawResult());

            if (latest.getValue1() == 1) {
                FuturePut put2 = dht.put(latest.getValue0().locationKey())
                                    .versionKey(latest.getValue0().versionKey())
                                    .putConfirm()
                                    .data(new Data())
                                    .start();

                while (!put2.isCompleted());
            } else {
                throw new UnalignedElementsException();
            }
        } catch (UnalignedElementsException e) {
            FutureRemove remove = dht.remove(location)
                                     .versionKey(version)
                                     .start();

            while (!remove.isCompleted());

            throw new FailedOperationException("Unable to update the element " + key.locationKey() + " in the DHT.");
        }
    }

    /**
     * Removes an element from the DHT.
     *
     * @param dht the DHT to remove the object from
     * @param key the location key of the element to remove
     *
     * @throws FailedOperationException if something goes wrong when performing the operation on the DHT
     */
    public static void remove(PeerDHT dht, Number160 key) {
        FutureRemove remove = dht.remove(key)
                                 .all()
                                 .start();

        while (!remove.isCompleted());

        if (!remove.isSuccess()) {
            throw new FailedOperationException("Unable to remove the element " + key + " from the DHT.");
        }
    }

    /**
     * Checks whether the peers agree or not on the data associated to a key in the DHT.
     *
     * @param rawData the map containing each view of the data for each peer in the system
     * @param <K>     the type of data (could be either Data or Byte)
     *
     * @throws UnalignedElementsException if peers do not agree on the latest version of the element
     */
    private static <K> Pair<Number640, K> checkLatestVersion(Map<PeerAddress, Map<Number640, K>> rawData) throws UnalignedElementsException {
        Number640 latestKey = rawData.entrySet().iterator().next().getValue().keySet().iterator().next();
        K latestData = rawData.entrySet().iterator().next().getValue().values().iterator().next();

        for (Map.Entry<PeerAddress, Map<Number640, K>> entry : rawData.entrySet()) {
            if (!entry.getValue().keySet().iterator().next().equals(latestKey) ||
                !entry.getValue().values().iterator().next().equals(latestData)) {
                throw new UnalignedElementsException("Inconsistency detected for " + latestKey.locationKey() + ".");
            }
        }

        return new Pair<>(latestKey, latestData);
    }

}

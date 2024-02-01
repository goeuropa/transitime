/* (C)2023 */
package org.transitclock.service;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.transitclock.SingletonContainer;
import org.transitclock.core.dataCache.ErrorCacheFactory;
import org.transitclock.core.dataCache.HistoricalAverage;
import org.transitclock.core.dataCache.HoldingTimeCache;
import org.transitclock.core.dataCache.HoldingTimeCacheKey;
import org.transitclock.core.dataCache.IpcArrivalDepartureComparator;
import org.transitclock.core.dataCache.KalmanErrorCacheKey;
import org.transitclock.core.dataCache.StopArrivalDepartureCacheFactory;
import org.transitclock.core.dataCache.StopArrivalDepartureCacheKey;
import org.transitclock.core.dataCache.StopPathCacheKey;
import org.transitclock.core.dataCache.TripDataHistoryCacheFactory;
import org.transitclock.core.dataCache.TripKey;
import org.transitclock.core.dataCache.frequency.FrequencyBasedHistoricalAverageCache;
import org.transitclock.core.dataCache.scheduled.ScheduleBasedHistoricalAverageCache;
import org.transitclock.service.dto.IpcArrivalDeparture;
import org.transitclock.service.dto.IpcHistoricalAverage;
import org.transitclock.service.dto.IpcHistoricalAverageCacheKey;
import org.transitclock.service.dto.IpcHoldingTimeCacheKey;
import org.transitclock.service.dto.IpcKalmanErrorCacheKey;
import org.transitclock.service.contract.CacheQueryInterface;

/**
 * @author Sean Og Crudden Server to allow cache content to be queried.
 */
@Slf4j
public class CacheQueryServiceImpl implements CacheQueryInterface {
    // Should only be accessed as singleton class
    private static CacheQueryServiceImpl singleton;

    public static CacheQueryInterface instance() {
        return singleton;
    }

    private final ScheduleBasedHistoricalAverageCache scheduleBasedHistoricalAverageCache;
    private final HoldingTimeCache holdingTimeCache;
    private final FrequencyBasedHistoricalAverageCache frequencyBasedHistoricalAverageCache;

    public CacheQueryServiceImpl() {
        scheduleBasedHistoricalAverageCache = SingletonContainer.getInstance(ScheduleBasedHistoricalAverageCache.class);
        holdingTimeCache = SingletonContainer.getInstance(HoldingTimeCache.class);
        frequencyBasedHistoricalAverageCache = SingletonContainer.getInstance(FrequencyBasedHistoricalAverageCache.class);
    }

    /**
     * Starts up the CacheQueryServer so that RMI calls can be used to query cache. This will
     * automatically cause the object to continue to run and serve requests.
     *
     * @return the singleton CacheQueryServer object. Usually does not need to used since the server
     *     will be fully running.
     */
    public static CacheQueryServiceImpl start() {
        if (singleton == null) {
            singleton = new CacheQueryServiceImpl();
        }

        return singleton;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.transitclock.ipc.interfaces.CacheQueryInterface#
     * getStopArrivalDepartures(java.lang.String)
     */
    @Override
    public List<IpcArrivalDeparture> getStopArrivalDepartures(String stopId) throws RemoteException {

        try {
            StopArrivalDepartureCacheKey nextStopKey = new StopArrivalDepartureCacheKey(
                    stopId, Calendar.getInstance().getTime());

            return StopArrivalDepartureCacheFactory.getInstance().getStopHistory(nextStopKey);

        } catch (Exception e) {

            throw new RemoteException(e.toString(), e);
        }
    }

    @Override
    public Integer entriesInCache(String cacheName) throws RemoteException {

        // TODO Auto-generated method stub
        return -1;
    }

    @Override
    public IpcHistoricalAverage getHistoricalAverage(String tripId, Integer stopPathIndex) throws RemoteException {
        StopPathCacheKey key = new StopPathCacheKey(tripId, stopPathIndex);

        HistoricalAverage average = scheduleBasedHistoricalAverageCache.getAverage(key);
        return new IpcHistoricalAverage(average);
    }

    @Override
    public List<IpcArrivalDeparture> getTripArrivalDepartures(String tripId, LocalDate localDate, Integer starttime)
            throws RemoteException {

        try {
            List<IpcArrivalDeparture> result = new ArrayList<>();

            if (tripId != null && localDate != null && starttime != null) {

                Date date =
                        Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                TripKey tripKey = new TripKey(tripId, date, starttime);

                result = TripDataHistoryCacheFactory.getInstance().getTripHistory(tripKey);
            } else if (tripId != null && localDate != null && starttime == null) {
                Date date =
                        Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                for (TripKey key : TripDataHistoryCacheFactory.getInstance().getKeys()) {
                    if (key.getTripId().equals(tripId) && date.compareTo(key.getTripStartDate()) == 0) {
                        result.addAll(TripDataHistoryCacheFactory.getInstance().getTripHistory(key));
                    }
                }
            } else if (tripId != null && localDate == null && starttime == null) {
                for (TripKey key : TripDataHistoryCacheFactory.getInstance().getKeys()) {
                    if (key.getTripId().equals(tripId)) {
                        result.addAll(TripDataHistoryCacheFactory.getInstance().getTripHistory(key));
                    }
                }
            } else if (tripId == null && localDate != null && starttime == null) {
                Date date =
                        Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                for (TripKey key : TripDataHistoryCacheFactory.getInstance().getKeys()) {
                    if (date.compareTo(key.getTripStartDate()) == 0) {
                        result.addAll(TripDataHistoryCacheFactory.getInstance().getTripHistory(key));
                    }
                }
            }

            result.sort(new IpcArrivalDepartureComparator());

            return result;

        } catch (Exception e) {

            throw new RemoteException(e.toString(), e);
        }
    }

    @Override
    public List<IpcHistoricalAverageCacheKey> getScheduledBasedHistoricalAverageCacheKeys() throws RemoteException {

        List<StopPathCacheKey> keys =
                scheduleBasedHistoricalAverageCache.getKeys();
        List<IpcHistoricalAverageCacheKey> ipcResultList = new ArrayList<IpcHistoricalAverageCacheKey>();

        for (StopPathCacheKey key : keys) {
            ipcResultList.add(new IpcHistoricalAverageCacheKey(key));
        }
        return ipcResultList;
    }

    @Override
    public Double getKalmanErrorValue(String tripId, Integer stopPathIndex) throws RemoteException {
        KalmanErrorCacheKey key = new KalmanErrorCacheKey(tripId, stopPathIndex);
        return ErrorCacheFactory.getInstance().getErrorValue(key).getError();
    }

    @Override
    public List<IpcKalmanErrorCacheKey> getKalmanErrorCacheKeys() throws RemoteException {
        List<KalmanErrorCacheKey> keys = ErrorCacheFactory.getInstance().getKeys();
        List<IpcKalmanErrorCacheKey> ipcResultList = new ArrayList<>();

        for (KalmanErrorCacheKey key : keys) {
            ipcResultList.add(new IpcKalmanErrorCacheKey(key));
        }
        return ipcResultList;
    }

    @Override
    public List<IpcHoldingTimeCacheKey> getHoldingTimeCacheKeys() throws RemoteException {
        List<HoldingTimeCacheKey> keys = holdingTimeCache.getKeys();
        List<IpcHoldingTimeCacheKey> ipcResultList = new ArrayList<IpcHoldingTimeCacheKey>();

        for (HoldingTimeCacheKey key : keys) {
            ipcResultList.add(new IpcHoldingTimeCacheKey(key));
        }
        return ipcResultList;
    }

    @Override
    public List<IpcHistoricalAverageCacheKey> getFrequencyBasedHistoricalAverageCacheKeys() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }
}

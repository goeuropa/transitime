/* (C)2023 */
package org.transitclock.domain.structs;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.DynamicUpdate;
import org.transitclock.Core;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * For storing static configuration for vehicle in block.
 *
 * @author Hubert GoEuropa
 */
@Entity
@DynamicUpdate
@Slf4j
@Table(name = "export_table")
public class ExportTable implements Serializable {

    // ID of vehicle
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "data_date")
    @Temporal(TemporalType.DATE)
    private Date dataDate;

    @Column(name = "export_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date exportDate;

    @Column(name = "export_type")
    private int exportType;

    @Column(name = "export_status")
    private int exportStatus;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file")
    private byte[] file;

    public static ExportTable create(Date dataDate, int exportType, String fileName) {
        ExportTable exportElement = new ExportTable(dataDate, exportType, fileName);

        // Log VehicleToBlockConfig in log file
        logger.info(exportElement.toString());

        // Queue to write object to database
        Core.getInstance().getDbLogger().add(exportElement);

        // Return new VehicleToBlockConfig
        return exportElement;
    }

    public ExportTable(Date dataDate, int exportType, String fileName) {
        this.dataDate = dataDate;
        this.exportType = exportType;
        this.fileName = fileName;
        this.exportDate = new Date();
        this.exportStatus = 1;
    }

    public ExportTable(long id, Date dataDate, Date exportDate, int exportType, int exportStatus, String fileName) {
        this.id = id;
        this.dataDate = dataDate;
        this.exportDate = exportDate;
        this.exportType = exportType;
        this.exportStatus = exportStatus;
        this.fileName = fileName;
    }

    /** Needed because Hibernate requires no-arg constructor */
    @SuppressWarnings("unused")
    protected ExportTable() {
        dataDate = null;
        exportDate = null;
        exportType = 0;
        fileName = null;
        file = null;
    }

    /**
     * Reads List of VehicleConfig objects from database
     *
     * @param session
     * @return List of VehicleConfig objects
     * @throws HibernateException
     */
    public static List<ExportTable> getExportTable(Session session) throws HibernateException {
        // String hql = "FROM ExportTable";
        var query = session.createQuery("FROM ExportTable ORDER BY exportDate DESC", ExportTable.class);
        return query.list();
    }

    public static void deleteExportTableRecord(long id, Session session) throws HibernateException {
        Transaction transaction = session.beginTransaction();
        try {
            var q = session
                    .createMutationQuery("delete from ExportTable where id = :id")
                    .setParameter("id", id);
            q.executeUpdate();

            transaction.commit();
        } catch (Throwable t) {
            transaction.rollback();
            throw t;
        }
    }

    public static List<ExportTable> getExportFile(Session session, long id) throws HibernateException {
        return session.createQuery("FROM ExportTable WHERE id = :id", ExportTable.class)
                .setParameter("id", id)
                .list();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getDataDate() {
        return dataDate;
    }

    public void setDataDate(Date dataDate) {
        this.dataDate = dataDate;
    }

    public Date getExportDate() {
        return exportDate;
    }

    public void setExportDate(Date exportDate) {
        this.exportDate = exportDate;
    }

    public int getExportType() {
        return exportType;
    }

    public void setExportType(int exportType) {
        this.exportType = exportType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public int getExportStatus() {
        return exportStatus;
    }

    public void setExportStatus(int exportStatus) {
        this.exportStatus = exportStatus;
    }
}

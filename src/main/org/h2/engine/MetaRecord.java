/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.engine;

import java.sql.SQLException;
import java.util.Comparator;
import org.h2.api.DatabaseEventListener;
import org.h2.command.CommandInterface;
import org.h2.command.Prepared;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.SearchRow;
import org.h2.value.ValueInt;
import org.h2.value.ValueString;

/**
 * A record in the system table of the database.
 * It contains the SQL statement to create the database object.
 */
public class MetaRecord implements Comparable<MetaRecord> {

    /**
     * Comparator for prepared constraints, sorts unique and primary key
     * constraints first.
     */
    static final Comparator<Prepared> CONSTRAINTS_COMPARATOR = (o1, o2) -> {
        if (o1 == null) {
            return o2 == null ? 0 : 1;
        } else if (o2 == null) {
            return -1;
        }
        int t1 = o1.getType(), t2 = o2.getType();
        boolean u1 = t1 == CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_PRIMARY_KEY
                || t1 == CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_UNIQUE;
        boolean u2 = t2 == CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_PRIMARY_KEY
                || t2 == CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_UNIQUE;
        if (u1 == u2) {
            return o1.getPersistedObjectId() - o2.getPersistedObjectId();
        }
        return u1 ? -1 : 1;
    };

    private final int id;
    private final int objectType;
    private final String sql;

    /**
     * Copy metadata from the specified object into specified search row.
     *
     * @param obj
     *            database object
     * @param r
     *            search row
     */
    public static void populateRowFromDBObject(DbObject obj, SearchRow r) {
        r.setValue(0, ValueInt.get(obj.getId()));
        r.setValue(1, ValueInt.get(0));
        r.setValue(2, ValueInt.get(obj.getType()));
        r.setValue(3, ValueString.get(obj.getCreateSQL()));
    }

    public MetaRecord(SearchRow r) {
        id = r.getValue(0).getInt();
        objectType = r.getValue(2).getInt();
        sql = r.getValue(3).getString();
    }

    /**
     * Execute the meta data statement.
     *
     * @param db the database
     * @param systemSession the system session
     * @param listener the database event listener
     */
    void prepareAndExecute(Database db, Session systemSession, DatabaseEventListener listener) {
        try {
            Prepared command = systemSession.prepare(sql);
            command.setPersistedObjectId(id);
            command.update();
        } catch (DbException e) {
            throwException(db, listener, e);
        }
    }

    /**
     * Prepares the meta data statement.
     *
     * @param db the database
     * @param systemSession the system session
     * @param listener the database event listener
     * @return the prepared command
     */
    Prepared prepare(Database db, Session systemSession, DatabaseEventListener listener) {
        try {
            Prepared command = systemSession.prepare(sql);
            command.setPersistedObjectId(id);
            return command;
        } catch (DbException e) {
            throwException(db, listener, e);
            return null;
        }
    }

    /**
     * Execute the meta data statement.
     *
     * @param db the database
     * @param command the prepared command
     * @param listener the database event listener
     */
    void execute(Database db, Prepared command, DatabaseEventListener listener) {
        try {
            command.update();
        } catch (DbException e) {
            throwException(db, listener, e);
        }
    }

    private void throwException(Database db, DatabaseEventListener listener, DbException e) {
        e = e.addSQL(sql);
        SQLException s = e.getSQLException();
        db.getTrace(Trace.DATABASE).error(s, sql);
        if (listener != null) {
            listener.exceptionThrown(s, sql);
            // continue startup in this case
        } else {
            throw e;
        }
    }

    public int getId() {
        return id;
    }

    public int getObjectType() {
        return objectType;
    }

    public String getSQL() {
        return sql;
    }

    /**
     * Sort the list of meta records by 'create order'.
     *
     * @param other the other record
     * @return -1, 0, or 1
     */
    @Override
    public int compareTo(MetaRecord other) {
        int c1 = getCreateOrder();
        int c2 = other.getCreateOrder();
        if (c1 != c2) {
            return c1 - c2;
        }
        return getId() - other.getId();
    }

    /**
     * Get the sort order id for this object type. Objects are created in this
     * order when opening a database.
     *
     * @return the sort index
     */
    private int getCreateOrder() {
        switch (objectType) {
        case DbObject.SETTING:
            return 0;
        case DbObject.USER:
            return 1;
        case DbObject.SCHEMA:
            return 2;
        case DbObject.FUNCTION_ALIAS:
            return 3;
        case DbObject.DOMAIN:
            return 4;
        case DbObject.SEQUENCE:
            return 5;
        case DbObject.CONSTANT:
            return 6;
        case DbObject.TABLE_OR_VIEW:
            return 7;
        case DbObject.INDEX:
            return 8;
        case DbObject.CONSTRAINT:
            return 9;
        case DbObject.TRIGGER:
            return 10;
        case DbObject.SYNONYM:
            return 11;
        case DbObject.ROLE:
            return 12;
        case DbObject.RIGHT:
            return 13;
        case DbObject.AGGREGATE:
            return 14;
        case DbObject.COMMENT:
            return 15;
        default:
            throw DbException.throwInternalError("type="+objectType);
        }
    }

    @Override
    public String toString() {
        return "MetaRecord [id=" + id + ", objectType=" + objectType +
                ", sql=" + sql + "]";
    }

}

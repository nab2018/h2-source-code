/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mvstore;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.h2.engine.CastDataProvider;
import org.h2.engine.Database;
import org.h2.engine.Mode;
import org.h2.mvstore.db.StatefulDataType;
import org.h2.mvstore.db.ValueDataType;
import org.h2.mvstore.type.BasicDataType;
import org.h2.mvstore.type.DataType;
import org.h2.result.RowFactory;
import org.h2.result.SearchRow;
import org.h2.store.DataHandler;
import org.h2.value.CompareMode;
import org.h2.value.Value;

/**
 * Class RowDataType.
 * <UL>
 * <LI> 8/12/17 10:48 AM initial creation
 * </UL>
 *
 * @author <a href='mailto:andrei.tokar@gmail.com'>Andrei Tokar</a>
 */
public final class RowDataType extends BasicDataType<SearchRow> implements StatefulDataType {

    private final ValueDataType valueDataType;
    private final int[]         sortTypes;
    private final int[]         indexes;
    private final int           columnCount;

    public RowDataType(CastDataProvider provider, CompareMode compareMode, Mode mode, DataHandler handler,
            int[] sortTypes, int[] indexes, int columnCount) {
        this.valueDataType = new ValueDataType(provider, compareMode, mode, handler, sortTypes);
        this.sortTypes = sortTypes;
        this.indexes = indexes;
        this.columnCount = columnCount;
        assert indexes == null || sortTypes.length == indexes.length;
    }

    public int[] getIndexes() {
        return indexes;
    }

    public RowFactory getRowFactory() {
        return valueDataType.getRowFactory();
    }

    public void setRowFactory(RowFactory rowFactory) {
        valueDataType.setRowFactory(rowFactory);
    }

    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public SearchRow[] createStorage(int capacity) {
        return new SearchRow[capacity];
    }

    @Override
    public int compare(SearchRow a, SearchRow b) {
        if (a == b) {
            return 0;
        }
        if (indexes == null) {
            int len = a.getColumnCount();
            assert len == b.getColumnCount() : len + " != " + b.getColumnCount();
            for (int i = 0; i < len; i++) {
                int comp = valueDataType.compareValues(a.getValue(i), b.getValue(i), sortTypes[i]);
                if (comp != 0) {
                    return comp;
                }
            }
            return 0;
        } else {
            for (int i = 0; i < indexes.length; i++) {
                int index = indexes[i];
                Value v1 = a.getValue(index);
                Value v2 = b.getValue(index);
                if (v1 == null || v2 == null) {
                    // can't compare further
                    break;
                }
                int comp = valueDataType.compareValues(a.getValue(index), b.getValue(index), sortTypes[i]);
                if (comp != 0) {
                    return comp;
                }
            }
            long aKey = a.getKey();
            long bKey = b.getKey();
            return aKey == SearchRow.MATCH_ALL_ROW_KEY || bKey == SearchRow.MATCH_ALL_ROW_KEY ?
                    0 : Long.compare(aKey, bKey);
        }
    }

//    @Override
    public int binarySearch(SearchRow key, Object storage, int size, int initialGuess) {
        return binarySearch(key, (SearchRow[])storage, size, initialGuess);
    }

    public int binarySearch(SearchRow key, SearchRow[] keys, int size, int initialGuess) {
        int low = 0;
        int high = size - 1;
        // the cached index minus one, so that
        // for the first time (when cachedCompare is 0),
        // the default value is used
        int x = initialGuess - 1;
        if (x < 0 || x > high) {
            x = high >>> 1;
        }
        while (low <= high) {
            int compare = compare(key, keys[x]);
            if (compare > 0) {
                low = x + 1;
            } else if (compare < 0) {
                high = x - 1;
            } else {
                return x;
            }
            x = (low + high) >>> 1;
        }
        return -(low + 1);
    }

    @Override
    public int getMemory(SearchRow row) {
        return row.getMemory();
    }

    @Override
    public SearchRow read(ByteBuffer buff) {
        //TODO: switch to compact format when format backward-compatibility is not required
        return readCompatible(buff);
/*
        SearchRow row = valueDataType.getRowFactory().createRow();
        row.setKey(DataUtils.readVarLong(buff));
        if (indexes == null) {
            int columnCount = DataUtils.readVarInt(buff);
            for (int i = 0; i < columnCount; i++) {
                row.setValue(i, valueDataType.read(buff));
            }
        } else {
            for (int i : indexes) {
                row.setValue(i, valueDataType.read(buff));
            }
        }
        return row;
*/
    }

    public SearchRow readCompatible(ByteBuffer buff) {
        return (SearchRow)valueDataType.read(buff);
    }


    @Override
    public void write(WriteBuffer buff, SearchRow row) {
        //TODO: switch to compact format when format backward-compatibility is not required
        writeCompatible(buff, row);
//        buff.putVarLong(row.getKey());
//        if (indexes == null) {
//            int columnCount = row.getColumnCount();
//            buff.putVarInt(columnCount);
//            for (int i = 0; i < columnCount; i++) {
//                valueDataType.write(buff, row.getValue(i));
//            }
//        } else {
//            for (int i : indexes) {
//                valueDataType.write(buff, row.getValue(i));
//            }
//        }
    }

    public void writeCompatible(WriteBuffer buff, SearchRow row) {
        valueDataType.writeRow(buff, row, indexes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != RowDataType.class) {
            return false;
        }
        RowDataType other = (RowDataType) obj;
        return columnCount == other.columnCount
            && Arrays.equals(indexes, other.indexes)
            && Arrays.equals(sortTypes, other.sortTypes)
            && valueDataType.equals(other.valueDataType);
    }

    @Override
    public int hashCode() {
        int res = super.hashCode();
        res = res * 31 + columnCount;
        res = res * 31 + Arrays.hashCode(indexes);
        res = res * 31 + Arrays.hashCode(sortTypes);
        res = res * 31 + valueDataType.hashCode();
        return res;
    }

    @Override
    public void save(WriteBuffer buff, DataType<DataType<?>> metaDataType, Database database) {
        buff.putVarInt(columnCount);
        writeIntArray(buff, sortTypes);
        writeIntArray(buff, indexes);
    }

    private static void writeIntArray(WriteBuffer buff, int[] array) {
        if(array == null) {
            buff.putVarInt(0);
        } else {
            buff.putVarInt(array.length + 1);
            for (int i : array) {
                buff.putVarInt(i);
            }
        }
    }

    @Override
    public void load(ByteBuffer buff, DataType<DataType<?>> metaDataType, Database database) {
        throw DataUtils.newUnsupportedOperationException("load()");
    }

    @Override
    public Factory getFactory() {
        return FACTORY;
    }



    private static final Factory FACTORY = new Factory();

    public static final class Factory implements StatefulDataType.Factory {

        @Override
        public RowDataType create(ByteBuffer buff, DataType<DataType<?>> metaDataType, Database database) {
            int columnCount = DataUtils.readVarInt(buff);
            int[] sortTypes = readIntArray(buff);
            int[] indexes = readIntArray(buff);
            CompareMode compareMode = database == null ? CompareMode.getInstance(null, 0) : database.getCompareMode();
            Mode mode = database == null ? Mode.getRegular() : database.getMode();
            RowFactory rowFactory = RowFactory.getDefaultRowFactory()
                    .createRowFactory(database, compareMode, mode, database, sortTypes, indexes, columnCount);
            return rowFactory.getRowDataType();
        }

        private static int[] readIntArray(ByteBuffer buff) {
            int len = DataUtils.readVarInt(buff) - 1;
            if(len < 0) {
                return null;
            }
            int[] res = new int[len];
            for (int i = 0; i < res.length; i++) {
                res[i] = DataUtils.readVarInt(buff);
            }
            return res;
        }
    }
}

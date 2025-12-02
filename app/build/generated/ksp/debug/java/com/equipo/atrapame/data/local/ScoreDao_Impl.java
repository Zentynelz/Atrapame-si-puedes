package com.equipo.atrapame.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ScoreDao_Impl implements ScoreDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalScoreEntity> __insertionAdapterOfLocalScoreEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkAsSynced;

  public ScoreDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalScoreEntity = new EntityInsertionAdapter<LocalScoreEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `scores` (`localId`,`idFirebase`,`playerName`,`moves`,`timeElapsed`,`timestamp`,`synced`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalScoreEntity entity) {
        statement.bindLong(1, entity.getLocalId());
        if (entity.getIdFirebase() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getIdFirebase());
        }
        statement.bindString(3, entity.getPlayerName());
        statement.bindLong(4, entity.getMoves());
        statement.bindLong(5, entity.getTimeElapsed());
        statement.bindLong(6, entity.getTimestamp());
        final int _tmp = entity.getSynced() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    };
    this.__preparedStmtOfMarkAsSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE scores SET synced = 1, idFirebase = ? WHERE localId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final LocalScoreEntity score, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalScoreEntity.insert(score);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markAsSynced(final int localId, final String firebaseId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAsSynced.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, firebaseId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, localId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkAsSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getTopScores(final int limit,
      final Continuation<? super List<LocalScoreEntity>> $completion) {
    final String _sql = "SELECT * FROM scores ORDER BY moves ASC, timeElapsed ASC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalScoreEntity>>() {
      @Override
      @NonNull
      public List<LocalScoreEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLocalId = CursorUtil.getColumnIndexOrThrow(_cursor, "localId");
          final int _cursorIndexOfIdFirebase = CursorUtil.getColumnIndexOrThrow(_cursor, "idFirebase");
          final int _cursorIndexOfPlayerName = CursorUtil.getColumnIndexOrThrow(_cursor, "playerName");
          final int _cursorIndexOfMoves = CursorUtil.getColumnIndexOrThrow(_cursor, "moves");
          final int _cursorIndexOfTimeElapsed = CursorUtil.getColumnIndexOrThrow(_cursor, "timeElapsed");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final List<LocalScoreEntity> _result = new ArrayList<LocalScoreEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalScoreEntity _item;
            final int _tmpLocalId;
            _tmpLocalId = _cursor.getInt(_cursorIndexOfLocalId);
            final String _tmpIdFirebase;
            if (_cursor.isNull(_cursorIndexOfIdFirebase)) {
              _tmpIdFirebase = null;
            } else {
              _tmpIdFirebase = _cursor.getString(_cursorIndexOfIdFirebase);
            }
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final int _tmpMoves;
            _tmpMoves = _cursor.getInt(_cursorIndexOfMoves);
            final long _tmpTimeElapsed;
            _tmpTimeElapsed = _cursor.getLong(_cursorIndexOfTimeElapsed);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp != 0;
            _item = new LocalScoreEntity(_tmpLocalId,_tmpIdFirebase,_tmpPlayerName,_tmpMoves,_tmpTimeElapsed,_tmpTimestamp,_tmpSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPlayerScores(final String playerName,
      final Continuation<? super List<LocalScoreEntity>> $completion) {
    final String _sql = "SELECT * FROM scores WHERE playerName = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, playerName);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalScoreEntity>>() {
      @Override
      @NonNull
      public List<LocalScoreEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLocalId = CursorUtil.getColumnIndexOrThrow(_cursor, "localId");
          final int _cursorIndexOfIdFirebase = CursorUtil.getColumnIndexOrThrow(_cursor, "idFirebase");
          final int _cursorIndexOfPlayerName = CursorUtil.getColumnIndexOrThrow(_cursor, "playerName");
          final int _cursorIndexOfMoves = CursorUtil.getColumnIndexOrThrow(_cursor, "moves");
          final int _cursorIndexOfTimeElapsed = CursorUtil.getColumnIndexOrThrow(_cursor, "timeElapsed");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final List<LocalScoreEntity> _result = new ArrayList<LocalScoreEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalScoreEntity _item;
            final int _tmpLocalId;
            _tmpLocalId = _cursor.getInt(_cursorIndexOfLocalId);
            final String _tmpIdFirebase;
            if (_cursor.isNull(_cursorIndexOfIdFirebase)) {
              _tmpIdFirebase = null;
            } else {
              _tmpIdFirebase = _cursor.getString(_cursorIndexOfIdFirebase);
            }
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final int _tmpMoves;
            _tmpMoves = _cursor.getInt(_cursorIndexOfMoves);
            final long _tmpTimeElapsed;
            _tmpTimeElapsed = _cursor.getLong(_cursorIndexOfTimeElapsed);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp != 0;
            _item = new LocalScoreEntity(_tmpLocalId,_tmpIdFirebase,_tmpPlayerName,_tmpMoves,_tmpTimeElapsed,_tmpTimestamp,_tmpSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingSyncScores(
      final Continuation<? super List<LocalScoreEntity>> $completion) {
    final String _sql = "SELECT * FROM scores WHERE synced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalScoreEntity>>() {
      @Override
      @NonNull
      public List<LocalScoreEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLocalId = CursorUtil.getColumnIndexOrThrow(_cursor, "localId");
          final int _cursorIndexOfIdFirebase = CursorUtil.getColumnIndexOrThrow(_cursor, "idFirebase");
          final int _cursorIndexOfPlayerName = CursorUtil.getColumnIndexOrThrow(_cursor, "playerName");
          final int _cursorIndexOfMoves = CursorUtil.getColumnIndexOrThrow(_cursor, "moves");
          final int _cursorIndexOfTimeElapsed = CursorUtil.getColumnIndexOrThrow(_cursor, "timeElapsed");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final List<LocalScoreEntity> _result = new ArrayList<LocalScoreEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalScoreEntity _item;
            final int _tmpLocalId;
            _tmpLocalId = _cursor.getInt(_cursorIndexOfLocalId);
            final String _tmpIdFirebase;
            if (_cursor.isNull(_cursorIndexOfIdFirebase)) {
              _tmpIdFirebase = null;
            } else {
              _tmpIdFirebase = _cursor.getString(_cursorIndexOfIdFirebase);
            }
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final int _tmpMoves;
            _tmpMoves = _cursor.getInt(_cursorIndexOfMoves);
            final long _tmpTimeElapsed;
            _tmpTimeElapsed = _cursor.getLong(_cursorIndexOfTimeElapsed);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp != 0;
            _item = new LocalScoreEntity(_tmpLocalId,_tmpIdFirebase,_tmpPlayerName,_tmpMoves,_tmpTimeElapsed,_tmpTimestamp,_tmpSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

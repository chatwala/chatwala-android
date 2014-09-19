package com.chatwala.android.db;

import android.database.sqlite.SQLiteDatabase;
import com.chatwala.android.app.ChatwalaApplication;
import com.chatwala.android.messages.ChatwalaMessage;
import com.chatwala.android.messages.ChatwalaSentMessage;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String DATABASE_NAME = "chatwala3.db";
    private static final int DATABASE_VERSION = 1;

    private static DatabaseHelper instance;
    private final ChatwalaApplication app;

    public static final Class[] allTables = new Class[] {
        ChatwalaMessage.class,
        ChatwalaSentMessage.class
    };

    private DatabaseHelper(ChatwalaApplication app) {
        super(app, DATABASE_NAME, null, DATABASE_VERSION);
        this.app = app;
    }

    public static synchronized DatabaseHelper attachToApp(ChatwalaApplication app) {
        if (instance == null) {
            instance = new DatabaseHelper(app);
        }

        return instance;
    }

    public static DatabaseHelper get() {
        return instance;
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            createTables(connectionSource);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        /*try {
            Dao<ChatwalaMessage, String> messageDao = getChatwalaMessageDao();
            messageDao.executeRaw("ALTER TABLE 'message' ADD COLUMN userThumbnailUrl VARCHAR");
        }
        catch(Exception e) {
            Logger.e("Got an error while updating the database", e);
        }

        try {
            Dao<ChatwalaMessage, String> messageDao = getChatwalaMessageDao();
            messageDao.executeRaw("ALTER TABLE 'message' ADD COLUMN isDeleted INTEGER DEFAULT 0");
        }
        catch(Exception e) {
            Logger.e("Got an error while updating the database", e);
        }*/
    }

    private void createTables(ConnectionSource connectionSource) throws SQLException {
        for (Class c : allTables) {
            TableUtils.createTable(connectionSource, c);
        }
    }

    private void dropTables(ConnectionSource connectionSource) throws SQLException {
        for (Class c : allTables) {
            TableUtils.dropTable(connectionSource, c, true);
        }
    }

    public Dao<ChatwalaMessage, String> getChatwalaMessageDao() throws SQLException {
        return getDao(ChatwalaMessage.class);
    }

    public Dao<ChatwalaSentMessage, String> getChatwalaSentMessageDao() throws SQLException {
        return getDao(ChatwalaSentMessage.class);
    }

    @Override
    public void close() {
        super.close();
        //If we cache DAOs, release the references here.
    }
}

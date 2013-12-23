package com.chatwala.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import co.touchlab.android.superbus.provider.sqlite.AbstractSqlitePersistenceProvider;
import com.chatwala.android.ChatwalaApplication;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: matthewdavis
 * Date: 12/17/13
 * Time: 12:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper
{
    private static final String DATABASE_NAME = "chatwala.db";
    private static final int DATABASE_VERSION = 4;

    private static DatabaseHelper instance;
    private final ChatwalaApplication app;

    public static final Class[] allTables = new Class[]
            {
                    ChatwalaMessage.class
            };

    private DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        app = (ChatwalaApplication)context.getApplicationContext();
    }

    public static synchronized DatabaseHelper getInstance(Context context)
    {
        if (instance == null)
        {
            instance = new DatabaseHelper(context);
        }

        return instance;
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource)
    {
        try
        {
            createTables(connectionSource);
            ((AbstractSqlitePersistenceProvider) (app.getProvider())).createTables(db);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion)
    {
        try
        {
            dropTables(connectionSource);
            ((AbstractSqlitePersistenceProvider) (app.getProvider())).dropTables(db);
            onCreate(db, connectionSource);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void createTables(ConnectionSource connectionSource) throws SQLException
    {
        for (Class c : allTables)
        {
            TableUtils.createTable(connectionSource, c);
        }
    }

    private void dropTables(ConnectionSource connectionSource) throws SQLException
    {
        for (Class c : allTables)
        {
            TableUtils.dropTable(connectionSource, c, true);
        }
    }

    public Dao<ChatwalaMessage, String> getChatwalaMessageDao() throws SQLException
    {
        return getDao(ChatwalaMessage.class);
    }

    @Override
    public void close()
    {
        super.close();
        //If we cache DAOs, release the references here.
    }
}

package Model.Database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import Model.Dao.FavoriteDao;
import Model.Entity.FavoriteEntity;

/**
 * Room 數據庫單例
 * 管理應用程序的本地數據庫
 */
@Database(entities = {FavoriteEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "graduation_project_db";
    private static volatile AppDatabase INSTANCE;

    /**
     * 獲取 FavoriteDao
     */
    public abstract FavoriteDao favoriteDao();

    /**
     * 獲取數據庫實例（單例模式，雙重檢查鎖定）
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}

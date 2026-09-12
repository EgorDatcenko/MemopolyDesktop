package com.memopoly.steam;


import com.memopoly.utils.AppLog;

/**
 * Менеджер достижений: разблокирует ачивки в Steam и показывает внутриигровой тост.
 */
public final class SteamAchievementsManager {
    private static AchievementToastCallback toastCallback;

    private SteamAchievementsManager() {}

    public interface AchievementToastCallback {
        void onAchievementUnlocked(String achievementId, String titleRu, String titleEn, String descriptionRu, String descriptionEn);
    }

    public static void setToastCallback(AchievementToastCallback callback) {
        toastCallback = callback;
    }

    /**
     * Разблокирует достижение в Steam и показывает тост.
     * Разблокировка уже разблокированной ачивки безвредна.
     */
    public static void unlock(String achievementId) {
        if (!SteamManager.isAvailable()) return;
        com.codedisaster.steamworks.SteamUserStats stats = SteamManager.getStats();
        if (stats == null) return;

        boolean achieved = stats.isAchieved(achievementId, false);
        if (achieved) {
            AppLog.info("Achievements", "Achievement " + achievementId + " уже разблокирован");
            return;
        }

        stats.setAchievement(achievementId);
        stats.storeStats();
        AppLog.info("Achievements", "Разблокирована ачивка: " + achievementId);

        String[] localized = AchievementCatalog.get(achievementId);
        if (localized != null && toastCallback != null) {
            toastCallback.onAchievementUnlocked(
                achievementId,
                localized[0], localized[1], // titleRu, titleEn
                localized[2], localized[3]  // descriptionRu, descriptionEn
            );
        }
    }

    /**
     * Инкрементирует прогресс-ачивку и автоматически разблокирует при достижении максимума.
     */
    public static void increment(String achievementId, int currentProgress, int maxProgress) {
        if (!SteamManager.isAvailable()) return;
        com.codedisaster.steamworks.SteamUserStats stats = SteamManager.getStats();
        if (stats == null) return;

        stats.indicateAchievementProgress(achievementId, currentProgress, maxProgress);
        stats.storeStats();

        if (currentProgress >= maxProgress) {
            unlock(achievementId);
        }
    }
}

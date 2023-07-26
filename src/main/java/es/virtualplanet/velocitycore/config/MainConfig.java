package es.virtualplanet.velocitycore.config;

import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.dazzleconf.sorter.AnnotationBasedSorter;

import java.util.List;

public interface MainConfig {

    @AnnotationBasedSorter.Order(1)
    @ConfKey("mysql")
    @SubSection
    MainConfig.MySQLInfo getMySQL();

    interface MySQLInfo {

        @AnnotationBasedSorter.Order(1)
        @ConfKey("host")
        @ConfDefault.DefaultString("localhost")
        String getHost();

        @AnnotationBasedSorter.Order(2)
        @ConfKey("port")
        @ConfDefault.DefaultInteger(3306)
        int getPort();

        @AnnotationBasedSorter.Order(3)
        @ConfKey("user")
        @ConfDefault.DefaultString("root")
        String getUser();

        @AnnotationBasedSorter.Order(4)
        @ConfKey("database")
        @ConfDefault.DefaultString("velocitycore")
        String getDatabase();

        @AnnotationBasedSorter.Order(5)
        @ConfKey("password")
        @ConfDefault.DefaultString("password")
        String getPassword();
    }

    @AnnotationBasedSorter.Order(2)
    @ConfKey("redis")
    @SubSection
    MainConfig.RedisInfo getRedis();

    interface RedisInfo {

        @AnnotationBasedSorter.Order(1)
        @ConfKey("host")
        @ConfDefault.DefaultString("localhost")
        String getHost();

        @AnnotationBasedSorter.Order(2)
        @ConfKey("port")
        @ConfDefault.DefaultInteger(6379)
        int getPort();

        @AnnotationBasedSorter.Order(3)
        @ConfKey("password")
        @ConfDefault.DefaultString("password")
        String getPassword();

        @AnnotationBasedSorter.Order(4)
        @ConfKey("timeout")
        @ConfDefault.DefaultInteger(2000)
        int getTimeout();
    }

    @AnnotationBasedSorter.Order(3)
    @ConfKey("discord")
    @SubSection
    MainConfig.DiscordInfo getDiscord();

    interface DiscordInfo {

        @AnnotationBasedSorter.Order(1)
        @ConfKey("token")
        @ConfDefault.DefaultString("token")
        String getToken();

        @AnnotationBasedSorter.Order(2)
        @ConfKey("verify")
        @SubSection
        MainConfig.DiscordInfo.Verify getVerify();

        interface Verify {

            @AnnotationBasedSorter.Order(1)
            @ConfKey("enabled")
            @ConfDefault.DefaultBoolean(false)
            boolean isEnabled();

            @AnnotationBasedSorter.Order(2)
            @ConfKey("verified-role")
            @ConfDefault.DefaultString("roleId")
            String getVerifiedRole();

            @AnnotationBasedSorter.Order(3)
            @ConfKey("donor-role")
            @ConfDefault.DefaultString("roleId")
            String getDonorRole();

            @AnnotationBasedSorter.Order(4)
            @ConfKey("mapping")
            @ConfDefault.DefaultStrings("rankName:roleId")
            List<String> getMapping();
        }
    }

    @AnnotationBasedSorter.Order(4)
    @ConfKey("server")
    @SubSection
    MainConfig.ServerInfo getServer();

    interface ServerInfo {

        @AnnotationBasedSorter.Order(1)
        @ConfKey("auth")
        @ConfDefault.DefaultString("authlobby")
        String getAuth();

        @AnnotationBasedSorter.Order(2)
        @ConfKey("hubs")
        @ConfDefault.DefaultStrings({"lobby-1"})
        List<String> getHubs();
    }

    @AnnotationBasedSorter.Order(5)
    @ConfKey("auto-reconnect")
    @SubSection
    MainConfig.AutoReconnectInfo getAutoReconnect();

    interface AutoReconnectInfo {
        @AnnotationBasedSorter.Order(1)
        @ConfKey("enabled")
        @ConfDefault.DefaultBoolean(true)
        boolean isEnabled();

        @AnnotationBasedSorter.Order(2)
        @ConfKey("available-in")
        @ConfDefault.DefaultStrings({"survival"})
        List<String> getAvailableServers();
    }
}

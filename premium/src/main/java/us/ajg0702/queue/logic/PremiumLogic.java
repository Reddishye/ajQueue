package us.ajg0702.queue.logic;

import com.google.common.collect.ImmutableList;
import us.ajg0702.queue.api.Logic;
import us.ajg0702.queue.api.players.AdaptedPlayer;
import us.ajg0702.queue.api.players.QueuePlayer;
import us.ajg0702.queue.api.queues.QueueServer;
import us.ajg0702.queue.api.util.QueueLogger;
import us.ajg0702.queue.common.QueueMain;
import us.ajg0702.queue.common.players.QueuePlayerImpl;
import us.ajg0702.queue.logic.permissions.PermissionGetter;

public class PremiumLogic implements Logic {
    @Override
    public boolean isPremium() {
        return true;
    }

    @Override
    public QueuePlayer priorityLogic(QueueServer server, AdaptedPlayer player) {
        int maxOfflineTime = PermissionGetter.getMaxOfflineTime(player);

        QueueMain main = QueueMain.getInstance();

        if(player.hasPermission("ajqueue.bypass") || player.hasPermission("ajqueue.serverbypass."+server.getName())) {
            QueuePlayer queuePlayer = new QueuePlayerImpl(player, server, Integer.MAX_VALUE, maxOfflineTime);
            server.addPlayer(queuePlayer, 0);
            main.getQueueManager().sendPlayers(server);
            return queuePlayer;
        }

        int priority = PermissionGetter.getPriority(player);
        int serverPriority = PermissionGetter.getServerPriotity(server.getName(), player);

        int highestPriority = Math.max(priority, serverPriority);

        QueuePlayer queuePlayer = new QueuePlayerImpl(player, server, highestPriority, maxOfflineTime);

        QueueLogger logger = main.getLogger();
        boolean debug = main.getConfig().getBoolean("priority-queue-debug");

        if(debug) {
            logger.info("[priority] "+player.getName()+" highestPriority: "+highestPriority);
            logger.info("[priority] "+player.getName()+"   priority: "+priority);
            logger.info("[priority] "+player.getName()+"   serverPriority: "+serverPriority);
        }

        if(highestPriority <= 0) {
            if(debug) {
                logger.info("[priority] "+player.getName()+"  No priority" );
            }
            server.addPlayer(queuePlayer);
            return queuePlayer;
        }

        ImmutableList<QueuePlayer> list = server.getQueue();

        for(int i = 0; i < list.size(); i++) {
            QueuePlayer pl = list.get(i);
            if(pl.getPriority() < highestPriority) {
                if(debug) {
                    logger.info("[priority] "+player.getName()+"  Adding to: "+i);
                }
                server.addPlayer(queuePlayer, i);
                return queuePlayer;
            }
        }

        if(debug) {
            logger.info("[priority] "+player.getName()+"  Cant go infront of anyone" );
        }
        server.addPlayer(queuePlayer);
        return queuePlayer;
    }

    @Override
    public boolean playerDisconnectedTooLong(QueuePlayer player) {
        return player.getTimeSinceOnline() > player.getMaxOfflineTime();
    }
}

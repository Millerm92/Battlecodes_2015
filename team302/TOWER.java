package team302;

import battlecode.common.*;

public class TOWER {
    public static RobotController rc;
    public static double prevHealth = 1000;
    public static boolean first = true;
    public static int ordinal;
    public static int MAX_RALLY_SIZE = 2;
    public static int startRound = 0;
    public static MapLocation myLocation;
    public static int towerOrdinal;
    
    public static void execute(RobotController rc_in) throws GameActionException {
        rc = rc_in;
        myLocation = RobotPlayer.myLocation;
    	RobotPlayer.targets = MyConstants.assaultPriorities;
        
        if(first) {
        	int swiggityswuglyswootswagfrigFUCK_DOM = 1;
        	int totalOre = 0;
        	MapLocation[] ores = MapLocation.getAllMapLocationsWithinRadiusSq(myLocation, 24);
        	for(int i = 0, len = ores.length; i < len; i+= 2) {
	        	totalOre = totalOre + (int) rc.senseOre(ores[i]);
	        	swiggityswuglyswootswagfrigFUCK_DOM ++;
	        }
        	totalOre = totalOre / swiggityswuglyswootswagfrigFUCK_DOM;
        	int prevOre = rc.readBroadcast(MyConstants.AVG_ORE);
        	rc.broadcast(MyConstants.AVG_ORE, totalOre + prevOre);

            towerOrdinal = getTowerOrdinal();
        	first = false;
        }
        
        if (isClosestToHq()) {
            rc.broadcast(MyConstants.ATTACK_GROUP_OFFSET, myLocation.x);
            rc.broadcast(MyConstants.ATTACK_GROUP_OFFSET + 1, myLocation.y);

            if (rc.readBroadcast(MyConstants.ATTACK_GROUP_OFFSET + 6) == 1) {
                rc.broadcast(MyConstants.ATTACK_GROUP_OFFSET + 6, 0);
            }

            RobotInfo[] myBots = rc.senseNearbyRobots(RobotPlayer.type.sensorRadiusSquared, RobotPlayer.myTeam);
            if (myBots.length > MAX_RALLY_SIZE) {
                rc.broadcast(MyConstants.ATTACK_GROUP_OFFSET + 6, 1);
                startRound = Clock.getRoundNum();
            }
        }



        double currHealth = rc.getHealth();
        if (currHealth < prevHealth && currHealth >= 150) {
            rc.broadcast(MyConstants.TOWER_UNDER_DISTRESS + towerOrdinal, 1);
        } else {
            rc.broadcast(MyConstants.TOWER_UNDER_DISTRESS + towerOrdinal, 0);
        }
        
        if (rc.isWeaponReady()) {
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotPlayer.attackRange, RobotPlayer.enemyTeam);
			if (enemiesInRange.length > 0) {
				MapLocation target = Util.smartAttack(rc, RobotPlayer.targets, enemiesInRange);
				if (target != null) {
					if (rc.canAttackLocation(target)) {
						rc.attackLocation(target);
					}
				}
			}
		}
        
        prevHealth = currHealth;

        broadcastHp(rc, currHealth);
    }

    // broadcast hp to a channel so the HQ can determine if we are winning or losing
    // if the roundnum doesnt get updated, we know the tower is dead, and the val should be 0
    public static void broadcastHp(RobotController rc, double health) throws GameActionException {
        rc.broadcast(MyConstants.MY_TOWER_HEALTH + (towerOrdinal * 2), Clock.getRoundNum());
        rc.broadcast(MyConstants.MY_TOWER_HEALTH + (towerOrdinal * 2) + 1, (int)health);
    }

    //am i the closest tower
    public static boolean isClosestToHq() {
        double dist;
        double myDist = myLocation.distanceSquaredTo(RobotPlayer.myHq);
        for (MapLocation t: RobotPlayer.myTowers) {
            dist = t.distanceSquaredTo(RobotPlayer.myHq);
            if (dist < myDist) {
                return false;
            }
        }

        return true;
    }

    public static int getTowerOrdinal() {
        MapLocation myLocation = rc.getLocation();
        for (int i = 0; i < RobotPlayer.myTowers.length; i++) {
            if (myLocation.equals(RobotPlayer.myTowers[i])) {
                return i ;
            }
        }
        return -1;
    }
}
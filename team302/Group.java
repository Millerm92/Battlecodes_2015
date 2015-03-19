package team302;
import battlecode.common.*;

public class Group {
    public static RobotController rc;
    public static boolean leader;
    public static boolean first = true;
    public static int groupOffset;
    public static MapLocation rallyPoint;

    public static void group(RobotController rc_in) throws GameActionException {
        rc = rc_in;
        MapLocation myLocation = RobotPlayer.myLocation;

        if (first) {
            groupOffset = findGroupOffset();
            leader = shouldILead();

            int roundNum = Clock.getRoundNum();
            int target = MyConstants.MINER_INFO;
            while(roundNum - rc.readBroadcast(target) < 2) {
                target = target + 4;
            }
            RobotPlayer.myChannel = target;
            rallyPoint = getRallyPoint();
            first = false;
        }
        if (!leader) {
            leader = startLeading();
        }
        if (leader) {
            leader = true;
            MapLocation pointOfInterest;

            if (Leader.state == Leader.RallyState.UNASSIGNED || Leader.state == Leader.RallyState.MOVING_TO_RALLY_LOC || Leader.state == Leader.RallyState.RALLYING) {
                Util.debug(rc, "leader un " + rallyPoint.toString());
                pointOfInterest = rallyPoint;
                rc.broadcast(groupOffset + GroupConstants.POI_TYPE, -1);
            } else{
                int x = rc.readBroadcast(MyConstants.MAIN_GOAL);
                int y = rc.readBroadcast(MyConstants.MAIN_GOAL + 1);
                pointOfInterest = new MapLocation(x,y);
                rc.broadcast(groupOffset + GroupConstants.POI_TYPE, rc.readBroadcast(MyConstants.MAIN_GOAL + 4));
            }

            for (int i = 0; i < RobotPlayer.myTowers.length; i++) {
                if (rc.readBroadcast(MyConstants.TOWER_UNDER_DISTRESS + i) == 1 && myLocation.distanceSquaredTo(RobotPlayer.myTowers[i]) < 50) {
                    pointOfInterest = RobotPlayer.myTowers[i];
                    rc.broadcast(groupOffset + GroupConstants.POI_TYPE, -1);
                    Leader.state = Leader.RallyState.MOVING_TO_POI;
                    break;
                }
            }
            
            rc.broadcast(groupOffset + GroupConstants.LEADER_LOCATION, myLocation.x);
            rc.broadcast(groupOffset + GroupConstants.LEADER_LOCATION + 1, myLocation.y);
            rc.broadcast(groupOffset + GroupConstants.LEADER_STAYALIVE, Clock.getRoundNum());
            rc.broadcast(groupOffset + GroupConstants.POINT_OF_INTEREST, pointOfInterest.x);
            rc.broadcast(groupOffset + GroupConstants.POINT_OF_INTEREST + 1, pointOfInterest.y);
            rc.broadcast(groupOffset + GroupConstants.GROUP_STAYALIVE, Clock.getRoundNum());

            rc.broadcast(RobotPlayer.myChannel, Clock.getRoundNum());
            if(rc.getSupplyLevel() < 500) {
            	if (rc.readBroadcast(RobotPlayer.myChannel + 1) != 2){
                    rc.broadcast(RobotPlayer.myChannel + 1, 1);
            	}
            } else if(rc.getSupplyLevel() >= 1000) {
                rc.broadcast(RobotPlayer.myChannel + 1, 0);
            }



            Leader.lead(rc, groupOffset, pointOfInterest, RobotPlayer.myChannel);
        } else {
            FollowTheLeader.follow(rc, groupOffset);
            rc.broadcast(groupOffset + GroupConstants.GROUP_STAYALIVE, rc.readBroadcast(groupOffset + GroupConstants.GROUP_STAYALIVE) + Clock.getRoundNum());
        }

    }

    public static boolean startLeading() throws GameActionException {
        int leaderStayAlive = rc.readBroadcast(groupOffset + GroupConstants.LEADER_STAYALIVE);
        if (leaderStayAlive < Clock.getRoundNum() - 1) {
            Leader.state = Leader.RallyState.MOVING_TO_POI;
            return true;
        }

        return false;
    }

    public static boolean shouldILead() throws GameActionException {
        int leaderStayAlive = rc.readBroadcast(groupOffset + GroupConstants.LEADER_STAYALIVE);
        if (leaderStayAlive < Clock.getRoundNum() - 1) {
            Util.debug(rc, "new lead");
            return true;
        }

        return false;
    }

    public static int findGroupOffset() throws GameActionException {
        double avgStayAlive;
        double totalStayAlive;
        int currOffset = MyConstants.GROUP_OFFSET;
        int numInGroup;
        while (currOffset < MyConstants.GROUP_OFFSET + 1200) {
turn currOffset;
            numInGroup = rc.readBroadcast(currOffset + GroupConstants.GROUP_COUNT);
            if (numInGroup < GroupConstants.GROUP_SIZE) {
                rc.broadcast(currOffset + GroupConstants.GROUP_COUNT, numInGroup + 1);
                return currOffset;
            }

            currOffset += 14;
        }

        return -1;
    }

    public static MapLocation getRallyPoint() throws GameActionException{
        MapLocation closest = RobotPlayer.enemyHq;
        double closestDistance = 999999;
        double currDistance;
        for (MapLocation m: RobotPlayer.myTowers) {
            currDistance = m.distanceSquaredTo(RobotPlayer.myHq);
            if (currDistance < closestDistance && currDistance > 60) {
                closestDistance = currDistance;
                closest = m;
            }
        }
        for (MapLocation m: RobotPlayer.enemyTowers) {
            currDistance = m.distanceSquaredTo(RobotPlayer.myHq);
            if (currDistance < closestDistance && currDistance > 60) {
                closestDistance = currDistance;
                closest = m;
            }
        }

        return closest;
    }
}